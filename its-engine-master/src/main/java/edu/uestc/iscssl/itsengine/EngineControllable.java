package edu.uestc.iscssl.itsengine;

import edu.uestc.iscssl.common.params.webParm;


public interface EngineControllable {
    boolean SimInit(String mapFilePath, String simId, String kafkaAddress, Structures.WebParamStructure.ByReference webParamStructure);
    boolean SetSimStep(Integer simStep);
    void SimStopRel();
    int BlockThreadSt();
    void NewSignalPlan(int crossID,int cycleTime,int ewStraight,int snStraight,int snLeft);
    boolean SetVehicleGeneratingModel(int LinkID, int model, int demand, double extraParams);
    void SetVehicleFollowingModel(int modelID);
}
