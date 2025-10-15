package edu.uestc.iscssl.itsbackend.utils;


import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.Gson;
import edu.uestc.iscssl.itsbackend.utils.MapInfo;
import edu.uestc.iscssl.itsbackend.utils.ParamInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


@Component
public class XmlUtils {
    // 从配置文件读取资源路径
    @Value("${app.resources.path:./}")
    private String resourcesPath;
    
    // 静态实例，用于支持静态方法中的配置访问
    private static XmlUtils instance;
    
    @Autowired
    public void setInstance(XmlUtils xmlUtils) {
        XmlUtils.instance = xmlUtils;
    }
    
    public static String xml2Json(String filePath) throws IOException {
        MapInfo mapInfo=getMapInfo(filePath);
        Gson gson=new Gson();
        return gson.toJson(mapInfo);
    }

    public static String MapInfo2Json(MapInfo mapInfo) throws IOException {
        Gson gson=new Gson();
        return gson.toJson(mapInfo);
    }
    public static String Param2Json(ParamInfo param) throws IOException {
        Gson gson=new Gson();
        return gson.toJson(param);
    }
    public static MapInfo getMapInfo(String filePath) throws IOException {
        // 使用配置化的路径，而不是硬编码路径
        String basePath = (instance != null && instance.resourcesPath != null) ? 
                          instance.resourcesPath : System.getProperty("user.dir");
        String path = basePath + "\\" + filePath;
        FileReader fileReader=new FileReader(path);
        BufferedReader bufferedReader =new BufferedReader(fileReader);
        StringBuilder sb=new StringBuilder();
        String str;
        while((str=bufferedReader.readLine())!=null) {
            sb.append(str);
        }
        XmlMapper xmlMapper=new XmlMapper();
        MapInfo mapInfo = xmlMapper.readValue(sb.toString(), MapInfo.class);
        mapInfo.setRoadNum(mapInfo.getMarginalPoints().size());
        mapInfo.setControllerNumber(mapInfo.getCrosses().size());
        return mapInfo;
    }

}
