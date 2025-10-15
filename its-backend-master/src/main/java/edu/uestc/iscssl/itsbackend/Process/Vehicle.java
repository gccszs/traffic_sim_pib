package edu.uestc.iscssl.itsbackend.Process;

import com.google.gson.annotations.Expose;
import edu.uestc.iscssl.itsbackend.Process.Statistic.BO.BO_Math;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.BaseCross;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.Baseline;
import edu.uestc.iscssl.itsbackend.Process.Statistic.File.StatTask;

public class Vehicle implements Cloneable{
    @Expose
    private int vehicleID;
    @Expose
    private int roadID;
    @Expose
    private double speed;
    @Expose
    private double acceleration;
    @Expose
    private double xPosition;
    @Expose
    private double yPosition;

    protected int lane = -3;

    public Vehicle(int vehicleID, int roadID, double speed, double acceleration, double xPosition, double yPosition) {
        this.vehicleID = vehicleID;
        this.roadID = roadID;
        this.speed = speed;
        this.acceleration = acceleration;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
    }

    public int getVehicleId() {
        return vehicleID;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleID = vehicleId;
    }

    public int getRoadId() {
        return roadID;
    }

    public void setRoadId(int roadId) {
        this.roadID = roadId;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(double acceleration) {
        this.acceleration = acceleration;
    }

    public double getxPosition() {
        return xPosition;
    }

    public void setxPosition(double xPosition) {
        this.xPosition = xPosition;
    }

    public double getyPosition() {
        return yPosition;
    }

    public void setyPosition(double yPosition) {
        this.yPosition = yPosition;
    }

    /**
     * @Author: 马
     * @Description: 第一次获取车道号
     * @Date: 2019年6月3日 16:50:31
     */
    public int getLane(StatTask statTask){
        if(lane!=-3)return lane;
        lane = statTask.getMap().lane_base(this);
        return lane;
    }

    /**
     * @Author: 马
     * @Description: 获取车道号
     * @Date: 2019年6月3日 16:50:31
     */
    public int getLane(){
        return lane;
    }
    /**
     * 重新标定车辆所在道路
     * 后端传来的roadID有问题，必须重新标定
     *
     * 这个过程需要在录入车辆的时候进行一次。
     * 这个过程需要在录入车辆的时候进行一次。
     * 这个过程需要在录入车辆的时候进行一次。
     * */
    public int fixRoad(StatTask statTask){
        double[] p = new double[]{this.xPosition,this.yPosition};
        for(Baseline b:statTask.getMap().getBaselines()){
            if(BO_Math.inBand(
                    b.getPoints(),
                    p,
                    (b.getMaxLeft()*1.0)*b.getWidth(),
                    (b.getMaxRight()*1.0)*b.getWidth()
            )){
                return b.getNum();
            }
        }
        double dis = 99999.99;
        int rtn = -1;
        for (BaseCross b:statTask.getMap().getBaseCrosses()){
            double temp = BO_Math.length_p2p(b.getPoint(),new double[]{xPosition,yPosition});
            if(temp<dis){
                dis = temp;
                rtn = b.getNum();
            }
        }
        return dis<statTask.getMap().getMaxRoadRange()*1.414?rtn:-1;
    }

    /*
    * 覆盖Object
    * */


    @Override
    public int hashCode() {
        Integer rtn = getVehicleId() * 3 + 0;
        return rtn.hashCode();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new Vehicle(
                this.vehicleID,
                this.roadID,
                this.speed,
                this.acceleration,
                this.xPosition,
                this.yPosition
        ) ;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Vehicle))return false;
        return this.hashCode()==obj.hashCode();
    }

    public boolean equals(int obj) {
        Integer hash = (obj*3+0);
        return this.hashCode()==hash.hashCode();
    }

    @Override
    public String toString() {
        return "{\"vehicle\":{\"speed\":"+speed+",\"roadID\":"+roadID+",\"lane\":"+lane+",\"xPosition\":"+xPosition+",\"yPosition\":"+yPosition+"}}";
    }
}
