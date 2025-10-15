package edu.uestc.iscssl.itsbackend.Process.Statistic.File;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cv_shandong on 2019/6/21.
 */
public class SMap_XML {
    private String XML_text;

    public SMap_XML(String path){
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(path);
            BufferedReader bufferedReader =new BufferedReader(fileReader);
            String strs = "";
            String str = null;
            while((str=bufferedReader.readLine())!=null) {
                if(!strs.equals(""))
                    strs+="\n";
                strs+=str;
            }
            XML_text = strs;
        } catch (FileNotFoundException e) {
            XML_text = "<ERROR>invalid filename</ERROR>";
        } catch (IOException e) {
            XML_text = "<ERROR>invalid file</ERROR>";
        }

    }

    public SMap_XML(int flag, String ipt){
        if(flag==0){
            XML_text = ipt;
            return;
        }
    }

    public void printText(){
        System.out.println(XML_text);
    }

    public List<String> getKeysFromStrings(String flags, String key){
        String arr = XML_text;
        String[] arrs = arr.split("<"+flags+">");
        List<String> rtn= new ArrayList<String>();
        for(int i=1;i<arrs.length;i++){
            arrs[i]=arrs[i].split("</"+flags+">")[0];
            arrs[i]=arrs[i].replace("\n","");
            arrs[i]=arrs[i].replace("\t","");
            arrs[i]=arrs[i].split("<"+key+">",2)[1].split("</"+key+">",2)[0];
            rtn.add(arrs[i]);
        }

        return rtn;

    }

    public List<SMap_XML> getXMLsFromStrings(String flags){
        List<SMap_XML> rtn = new ArrayList<SMap_XML>();
        String arr = XML_text;
        String[] arrs = arr.split("<"+flags+">");
        for(int i=1;i<arrs.length;i++){
            arrs[i]=arrs[i].split("</"+flags+">")[0];
            rtn.add(new SMap_XML(0,arrs[i]));
        }
        return rtn;


    }

}