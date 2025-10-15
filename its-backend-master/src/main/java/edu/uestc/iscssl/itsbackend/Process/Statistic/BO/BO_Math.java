package edu.uestc.iscssl.itsbackend.Process.Statistic.BO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: ma
 * @Description: 一些数学公式
 * @Date: 16:51:27 2019-5-20
 */
public abstract class BO_Math {
    public static final double pi = 3.14159265358979;
    public static final double PIX_M = 1.0;
    public static final double TICK_S = 1.0;
    public static final double LIQUE_INBAND = -4.0; //目前-2.1为最大的出错的值
    public static final double THRES_CLOSE = 10.0; //近距离阈值
    //车辆卡在道路段边缘容易判断错误，于是对边缘羽化一下

    /**
     * 像素/帧 换算成 千米每小时
     * */
    public static double ppt_to_kmph(double pt){
        double kmph = pt;
        kmph/=PIX_M;
        kmph*=TICK_S;
        kmph*=3600.0;
        kmph/=1000.0;
        return (kmph);
    }

    /**
     * 像素/帧方 换算成 米/秒方
     * */
    public static double ppst_to_mpss(double pt){
        double mpss = pt;
        mpss/=PIX_M;
        mpss*=TICK_S;
        mpss*=TICK_S;
        return (mpss);
    }

    /**
     * 帧速率 换算成 秒速*系数
     * */
    public static double flowpt_to_(double flow, double div){
        double to_ = flow;
        to_ *=TICK_S;
        to_ *=div;
        return to_;
    }

    /**
     * 帧速率 换算成 秒速
     * */
    public static double flowpt_to_(double flow){
        return flowpt_to_(flow,1.0);
    }

    /**
     * 帧换算成秒
     * */
    public static double t_to_s(double tick){
        return tick/TICK_S;
    }

    /**
     * 像素换算成米
     * */
    public static double p_to_m(double pix){
        return pix/PIX_M;
    }

    /**
     * 有向角度
     * @params: 起点，原点，终点
     * */
    public static double p3_ang(double[]p1,double[]o,double[]p2){
        double[] a1 = new double[]{p1[0]-o[0],p1[1]-o[1]};
        double[] a2 = new double[]{p2[0]-o[0],p2[1]-o[1]};
        double tan2;
        if(a2[0]==0) tan2 = pi/2;
        else tan2 = Math.atan(Math.abs(a2[1]/a2[0]));
        double tan1;
        if(a1[0]==0) tan1 = pi/2;
        else tan1 = Math.atan(Math.abs(a1[1]/a1[0]));

        double rtn = 0;
        if(a2[0] >= 0.0&&a2[1] >= 0.0){
            if(a1[0] >= 0.0&&a1[1] >= 0.0)
                rtn = tan2-tan1;
            else if(a1[0] < 0.0&&a1[1] >= 0.0)
                rtn = tan1 + tan2 - pi;
            else if(a1[0] < 0.0&&a1[1] < 0.0)
                rtn = tan2 - tan1 + pi;
            else
                rtn = tan1+tan2;
        }
        else if(a2[0] < 0.0&&a2[1] >= 0.0){
            if(a1[0] >= 0.0&&a1[1] >= 0.0)
                rtn = pi-tan2-tan1;
            else if(a1[0] < 0.0&&a1[1] >= 0.0)
                rtn = tan1-tan2;
            else if(a1[0] < 0.0&&a1[1] < 0.0)
                rtn = 0-tan1-tan2;
            else
                rtn = tan1 + pi - tan2;
        }
        else if(a2[0] < 0.0&&a2[1] < 0.0){
            if(a1[0] >= 0.0&&a1[1] >= 0.0)
                rtn = pi+tan2-tan1;
            else if(a1[0] < 0.0&&a1[1] >= 0.0)
                rtn = tan1+tan2;
            else if(a1[0] < 0.0&&a1[1] < 0.0)
                rtn = tan2 - tan1;
            else
                rtn = tan1 - pi + tan2;
        }
        else{
            if(a1[0] >= 0.0&&a1[1] >= 0.0)
                rtn = 0-tan2-tan1;
            else if(a1[0] < 0.0&&a1[1] >= 0.0)
                rtn = tan1 - tan2 - pi;
            else if(a1[0] < 0.0&&a1[1] < 0.0)
                rtn = pi - tan2 - tan1;
            else
                rtn = tan1 - tan2;
        }
        if (rtn>pi) rtn-=(2*pi);
        return rtn;
    }

    /**
     * 点的距离
     * @params: 判断点1，判断点2
     * */
    public static double length_p2p(double[]a,double[]b){
        double rtn = (a[0]-b[0])*(a[0]-b[0]);
        rtn += (a[1]-b[1])*(a[1]-b[1]);
        rtn = Math.sqrt(rtn);
        return rtn;
    }

    /**
     * 点到有向线段的距离
     * @params: 线段起点，线段终点，判断点
     * */
    public static double dist_p2v(double[]p1,double[]p2,double[]o){
        double ang = p3_ang(o,p1,p2);//三点组成三角形
        double ang2 = p3_ang(o,p2,p1);

        if(Math.abs(ang)>pi/2.0 ||
                Math.abs(ang2)>pi/2.0){//判断是不是钝角三角形
            double dp1 = length_p2p(p1,o);
            double dp2 = length_p2p(p2,o);
            return ang>0?(dp1<dp2?dp1:dp2):-(dp1<dp2?dp1:dp2);//是的话返回近的那个顶点距离，注意分正负
        }
        double dis = length_p2p(o,p1);
        return dis*Math.sin(ang);
    }

    /**
     * 点到折线的距离
     * @params: 折线序列（数组），判断点
     * */
    public static double dist_p2vs(double[][]v,double[]o){
        if(v.length<2)return length_p2p(v[0],o);
        double d = 999999.99;
        double dp;
        for(int i=0;i<v.length-1;i++){
            dp = dist_p2v(v[i],v[i+1],o);
            if(Math.abs(dp)<Math.abs(d))d=dp;
        }
        return d;
    }

    /**
     * 点到折线的距离
     * @params: 折线序列，判断点
     * */
    public static double dist_p2vs(List<double[]>v, double[]o){
        double[][]vs = new double[v.size()][];
        for(int i=0;i<v.size();i++){
            vs[i]=v.get(i);
        }
        return dist_p2vs(vs,o);
    }

    /**
     * 折线长
     * @params: 折线点序列
     * */
    public static double pArray_length(List<double[]>v){
        double rtn = 0.0;
        for(int i=1;i<v.size();i++){
            rtn += length_p2p(v.get(i-1),v.get(i));
        }
        return rtn;
    }

    /**
     * 将一个点推离原点    *
     * @params: 被推点，距离，角度
     * @return: 得到的点
     * */
    public static double[] pushForward(double[]a,double dis,double ang){
        double[]ap = a.clone();
        ap[0]+=(dis*Math.cos(ang));
        ap[1]+=(dis*Math.sin(ang));
        return ap;
    }


    /**
     * 判断点o在a盒子里
     * 注意，a盒子点序列必须是逆时针排列，而且不能有交叉和优角
     * @params: 盒子点序列，判断点
     * */
    public static boolean inBoxBeta(List<double[]>a,double[]o){
        int asize = a.size();
        if(asize<3)return false;
        int i;
        for(i=1;i<asize;i++){
            if(dist_p2v(a.get(i-1),a.get(i),o)<LIQUE_INBAND)return false;
        }
        if(dist_p2v(a.get(i-1),a.get(0),o)<LIQUE_INBAND)return false;
        return true;
    }

    /**
     * 判断o在一个条状区域内
     * @params: 线段起点，线段终点，判断点，条宽左，条宽右，起点偏角，终点偏角
     * */
    public static boolean inBar(double[]a,double[]b,double[]o,double width,double widthR,double Aa,double Ab){
        double[] origin = a.clone();
        origin[0]+=1.0;
        double oab = p3_ang(origin,a,b);
        double[]aR = pushForward(a, width/Math.cos(Aa),       oab-pi/2-Aa);
        double[]aL = pushForward(a, widthR/Math.cos(Aa)*(-1.0),oab-pi/2-Aa);
        double[]bR = pushForward(b, width/Math.cos(Ab),       oab-pi/2-Ab);
        double[]bL = pushForward(b, widthR/Math.cos(Ab)*(-1.0),oab-pi/2-Ab);
        List<double[]> aRR = new ArrayList<double[]>();
        aRR.add(aL);
        aRR.add(bL);
        aRR.add(bR);
        aRR.add(aR);

        return inBoxBeta(aRR,o);
    }

    /**
     * 判断o在一个折带条状区域内
     * @param a 折带主线坐标
     * @param o 判断点
     * @param width 条宽左
     * @param widthR 条宽右
     * */
    public static boolean inBand(List<double[]>a ,double[]o ,double width,double widthR){
        if(a.size()<2)return false;
        int siz = a.size();
        double pa = 0.0;
        double pb;
        int i;
        for(i=1;i<siz-1;i++){
            pb = pi - p3_ang(a.get(i-1),a.get(i),a.get(i+1));
            pb/=2.0;
            if(inBar(a.get(i-1),a.get(i),o,width,widthR,pa,pb))return true;
            pa = -pb;
        }
        pb = 0.0;
        if(inBar(a.get(i-1),a.get(i),o,width,widthR,pa,pb))return true;
        return false;

    }

    /**
     * 向量的模
     */
    public static double lgth(double... xxx){
        double rtn=0.0;
        for(double x:xxx){
            rtn+=(x*x);
        }
        rtn = Math.sqrt(rtn);
        return rtn;

    }

    /**
     * 求点集中最长的折线连串(一种)，两个点足够近则成串，单点长度为0
     * @param input 输入点集
     * @param THRES 距离阈值
     */
    public static double maxQueue(Collection<double[]> input, double THRES){
        if(input.size()<2)return 0.0;//空集，单点长度为0
        List<double[]> inputTemp = new ArrayList<double[]>(input);//待统计的点
        double[] p1 = inputTemp.get(0);//设置点串的收尾，刚开始为同一点
        double[] p2 = inputTemp.get(0);
        double rtn = 0.0;
        inputTemp.remove(0);//移除一个被判断的点，避免重复判断
        int i=0;
        while (i<inputTemp.size()){//依次判断每一个点，直到找不到相近点
            double[] p = inputTemp.get(i);
            double t1 = length_p2p(p1,p);
            if(t1<THRES){ //判断与首相近的情况
                p1 = p;//更改首点
                rtn += t1;//折线距离增加
                inputTemp.remove(i);//移除被判断点
                i=0;//重新开始遍历
                continue;
            }
            double t2 = length_p2p(p2,p);//与尾相近的情况，同上
            if(t2<THRES){
                p2 = p;
                rtn += t2;
                inputTemp.remove(i);
                i=0;
                continue;
            }
            i++;//检查下一个点
        }
        double cmp = maxQueue(inputTemp,THRES);//将剩余待判断点再进行一次判断
        return cmp>rtn?cmp:rtn;//将所有被判断结果比较，取最大值
    }

    public static double maxQueue(Collection<double[]> input){
        return maxQueue(input,THRES_CLOSE);
    }


}