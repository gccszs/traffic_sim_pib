def json_to_xml(json_obj: dict, root_tag="Data") -> str:
    # 使用 xmltodict 的库函数构建 XML
    xml_str = xmltodict.unparse({root_tag: json_obj}, pretty=True)
    return xml_str