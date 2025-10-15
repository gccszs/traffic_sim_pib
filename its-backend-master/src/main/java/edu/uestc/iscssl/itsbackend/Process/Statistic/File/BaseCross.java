package edu.uestc.iscssl.itsbackend.Process.Statistic.File;

/**
 * Created by cv_shandong on 2019/7/1.
 */
public class BaseCross {
    private double[] point;
    private int num;
    private double width;//这个宽度目前还用不上。。。
    private int max;

    public BaseCross(double x, double y, int num){
        this.point = new double[]{x,y};
        this.num = num;
        this.width = 10.0;
        this.max = 4;
    }

    public BaseCross(double x, double y, int num, int lanes){
        this(x,y, num);
        this.max = lanes;
    }

    @Override
    public String toString(){
        String rtn = ("Cross "+num+":");
            rtn +=("("+point[0]+","+point[1]+")");
        return rtn;
    }

    public int getNum(){
        return num;
    }
    public double[] getPoint(){
        return point;
    }
    public int getMax(){
        return  max;
    }
    public double getWidth(){
        return width;
    }
}