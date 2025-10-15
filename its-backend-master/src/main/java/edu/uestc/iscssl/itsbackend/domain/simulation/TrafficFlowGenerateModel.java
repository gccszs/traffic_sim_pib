package edu.uestc.iscssl.itsbackend.domain.simulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TrafficFlowGenerateModel implements Serializable {
    int id;
    String modelName;
    String[] extraParamName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String[] getExtraParamName() {
        return extraParamName;
    }

    public void setExtraParamName(String[] extraParamName) {
        this.extraParamName = extraParamName;
    }



    public TrafficFlowGenerateModel(int id, String modelName,String[] extraParamName) {
        this.id = id;
        this.modelName = modelName;
        this.extraParamName=extraParamName;
    }
    public static Object[] getModel()  {
        List<TrafficFlowGenerateModel> result=new ArrayList<>();
        String[] extraParamName0 = new String[]{};
        String[] extraParamName3 = new String[]{
                "形状参数"
        };
        String[] extraParamName4 = new String[]{
                "方差"
        };
        result.add(new TrafficFlowGenerateModel(0,"均匀分布",extraParamName0));
        result.add(new TrafficFlowGenerateModel(1,"指数分布",extraParamName0));
        result.add(new TrafficFlowGenerateModel(2,"伽马分布",extraParamName3));
        result.add(new TrafficFlowGenerateModel(3,"正态分布",extraParamName4));

/*        try {*/

            //String path=new ClassPathResource("TrafficFlowGenerateModel").getFile().getAbsolutePath();//改使用58-61行的内容
/*            Files.lines(Paths.get(path)).forEach(line->{
                String[] slice=line.split(",");
                if (slice.length==2)
                    result.add(new TrafficFlowGenerateModel(Integer.parseInt(slice[0]),slice[1],0));
                else{
                    int extraParamNum=slice.length-2;
                    TrafficFlowGenerateModel model=new TrafficFlowGenerateModel(Integer.parseInt(slice[0]),slice[1],extraParamNum);
                    for (int i=0;i<extraParamNum;i++){
                        model.extraParamName[i]=slice[2+i];
                    }
                    result.add(model);
                }

            });*/
/*
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        return result.toArray( );
    }
}
