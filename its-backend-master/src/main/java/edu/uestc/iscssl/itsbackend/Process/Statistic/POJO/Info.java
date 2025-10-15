package edu.uestc.iscssl.itsbackend.Process.Statistic.POJO;

import edu.uestc.iscssl.itsbackend.Process.Phase;
import edu.uestc.iscssl.itsbackend.Process.Vehicle;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 15:14 2019/5/9
 */
public class Info {

    private int step;
    List<Vehicle> vehicles;
    List<Phase> phases;

    public Info(int step){
        this.step = step;
        vehicles = new ArrayList<Vehicle>();
        phases = new ArrayList<Phase>();
    }

    public int getStep() {
        return step;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public List<Phase> getPhases() {
        return phases;
    }

    public void push(Vehicle vehicle){
        vehicles.add(vehicle);
    }

    public void push(Phase phase){
        phases.add(phase);
    }


    public String Config(){
        String rtn = "Step: ";
        rtn += this.step;
        rtn +="[vehicles: ";
        rtn += vehicles.size();
        rtn +=" , phases: ";
        rtn += phases.size();
        rtn +="]";
        return rtn;

    }

    public static String Configs(List<Info> list){
        String rtn="";
        for(Info i:list)rtn += (i.Config()+"\n");
        rtn +=("\n"+list.size()+" Steps");
        return rtn;


    }

}
