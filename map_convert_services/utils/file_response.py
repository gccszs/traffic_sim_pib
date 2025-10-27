import os
from fastapi import Response
from fastapi import HTTPException, UploadFile
from urllib.parse import quote
from map_convert_services.map_utils import osmtrans, mapmaker, mapmaker_new


async def map_convert_to_binary(upload_file: UploadFile, upload_file_path: str) -> Response:
    """文件转换并返回二进制流响应"""
    nameparts = upload_file.filename.split('.')
    if len(nameparts) < 2:
        raise HTTPException(status_code=400, detail="Invalid file name")

    file_name, file_extension = nameparts[0], nameparts[-1].lower()
    original_file_path = get_safe_path(upload_file_path, upload_file.filename)
    new_file_location = get_safe_path(upload_file_path, file_name)

    # 根据文件类型处理
    if file_extension == 'osm':
        new_txt_file_location = suffix_extension_appender(new_file_location, '.txt')
        result = osmtrans.osm_to_txt(original_file_path, new_txt_file_location)
        if not result:
            raise HTTPException(status_code=503, detail="Convert osm to txt error")
        xml_file_path, conversion_method = await convert_txt_to_xml(new_txt_file_location, new_file_location)
    elif file_extension == 'txt':
        xml_file_path, conversion_method = await convert_txt_to_xml(original_file_path, new_file_location)
    else:
        raise HTTPException(status_code=400, detail=f"Unsupported file format: {file_extension}")

    # 读取转换后的文件内容
    with open(xml_file_path, 'rb') as f:
        file_content = f.read()

    # 获取文件信息
    file_size = os.path.getsize(xml_file_path)
    filename = os.path.basename(xml_file_path)

    # 安全处理文件名
    safe_filename = safe_filename_header(filename)
    safe_original_filename = safe_filename_header(upload_file.filename)

    # 返回二进制流响应，确保使用正确的编码
    return Response(
        content=file_content,
        media_type="application/xml",
        headers={
            "Content-Disposition": f"attachment; filename*=UTF-8''{safe_filename}",
            "Content-Length": str(file_size),
            "X-Filename": safe_filename,
            "X-File-Size": str(file_size),
            "X-Original-File": safe_original_filename,
            "X-Conversion-Method": conversion_method,
            "X-Success": "true"
        }
    )


async def convert_txt_to_xml(original_txt_file_path: str, new_file_location: str):
    """TXT转XML并返回文件路径和转换方法"""
    xml_file_path = suffix_extension_appender(new_file_location, '.xml')

    # 检查输入文件是否存在
    if not os.path.exists(original_txt_file_path):
        raise HTTPException(status_code=404, detail="Source file not found")

    # 尝试转换
    result = mapmaker.txt_to_xml(original_txt_file_path, xml_file_path)
    conversion_method = 'old'

    if not result:
        conversion_method = 'new'
        result = mapmaker_new.txt_to_xml_new(original_txt_file_path, xml_file_path)

    if not result:
        raise HTTPException(status_code=503, detail="Convert txt to xml error")

    # 检查输出文件是否存在
    if not os.path.exists(xml_file_path):
        raise HTTPException(status_code=500, detail="Output file was not created")

    return xml_file_path, conversion_method


def suffix_extension_appender(file_path_without_extension: str, extension: str) -> str:
    """添加文件扩展名"""
    return ''.join([file_path_without_extension, extension])

def get_safe_path(base_dir: str, filename: str) -> str:
    """获取安全的文件路径，处理特殊字符"""
    # 清理文件名，移除或替换可能引起问题的字符
    safe_filename = os.path.basename(filename)
    # 替换可能引起编码问题的字符
    safe_filename = safe_filename.encode('utf-8', 'ignore').decode('utf-8')
    return os.path.join(base_dir, safe_filename)


def safe_filename_header(filename: str) -> str:
    """安全地处理文件名，用于HTTP头部"""
    try:
        # 尝试UTF-8编码
        filename.encode('utf-8')
        # 使用quote进行URL编码
        return quote(filename)
    except UnicodeEncodeError:
        # 如果UTF-8编码失败，使用回退方案
        safe_name = filename.encode('utf-8', 'ignore').decode('utf-8')
        return quote(safe_name) if safe_name else "converted_file.xml"