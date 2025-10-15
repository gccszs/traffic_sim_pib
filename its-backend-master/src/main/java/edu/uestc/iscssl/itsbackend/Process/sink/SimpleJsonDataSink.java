package edu.uestc.iscssl.itsbackend.Process.sink;


import edu.uestc.iscssl.common.common.Constant;
import edu.uestc.iscssl.itsbackend.utils.DataInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class SimpleJsonDataSink implements DataSink{
    private Path filePath;
    public SimpleJsonDataSink(String fileName) {
        filePath= Paths.get("C:\\its\\simulationFile\\"+fileName);
    }
    public void sink(String data){
        try {
            Files.write(filePath,new ArrayList<String>(1){{add(data);}},StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sink2(DataInfo dataInfo) {
    }
}
