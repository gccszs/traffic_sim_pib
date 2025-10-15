package edu.uestc.iscssl.itsbackend.Process.Statistic.File;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: ma
 * @Description: 道路的数据格式 (目前还用不上)
 * @Date: 01:49:24 2019-5-18
 */
public class Baseline {

    private List<double[]> points = new ArrayList<double[]>();
    private int num;
    private double width;
    private int maxLeft;
    private int maxRight;

    public Baseline(String input,int num){
        this.num = num;
        this.width = 10.0;
        this.maxLeft = 4;
        this.maxRight = 4;
        String[] inputs = input.split(",");
        for(int i=0;i<inputs.length;i++){
            String[] inputsP = inputs[i].split(" ",2);
            if(inputsP.length<2)break;
            double[] adds = {Double.parseDouble(inputsP[0]) , Double.parseDouble(inputsP[1])};
            points.add(adds);
        }
    }

    public Baseline(String input,int num,int lanes){
        this(input, num);
        this.maxLeft = lanes;
        this.maxRight = lanes;
    }

    @Override
    public String toString(){
        String rtn = ("Road "+num+":[");
        for(double[]a:points){
            if(rtn.contains(")"))rtn+=",";
            rtn +=("("+a[0]+","+a[1]+")");
        }
        rtn +="]";
        return rtn;
    }

    public int getNum(){
        return num;
    }
    public List<double[]> getPoints(){
        return points;
    }
    public int getMaxLeft(){
        return  maxLeft;
    }
    public int getMaxRight(){
        return  maxRight;
    }
    public double getWidth(){
        return width;
    }

}