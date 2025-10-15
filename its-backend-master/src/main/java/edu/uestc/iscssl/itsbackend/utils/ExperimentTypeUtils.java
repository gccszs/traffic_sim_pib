package edu.uestc.iscssl.itsbackend.utils;

public class ExperimentTypeUtils {
    public static String toType(Integer type){
        if(type<5)
            switch (type){
                case 0: return "交通路网仿真实验";
                case 1: return "车辆行为模型仿真实验";
                case 2: return "交通流配置仿真实验";
                case 3: return "交通信号控制仿真实验";
                default: return "自定义仿真实验";
            }
        else
            switch (type/10){
                case 5: return "换道模型对比实验";
                case 6: return "跟驰模型对比实验";
                case 7: return "交通流生成模型对比实验";
                case 8: return "交通流配置方案对比实验";
                default: return "信号灯配置方案对比实验";
            }
    }
}
