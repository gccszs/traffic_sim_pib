package edu.uestc.iscssl.itsbackend.service;

import edu.uestc.iscssl.itsbackend.utils.ParamInfo;

import java.util.List;

public interface ParamInfoService {
    ParamInfo addParam(ParamInfo param);
    ParamInfo findParamById(String simulationId);
    public List<ParamInfo> findParams();
}
