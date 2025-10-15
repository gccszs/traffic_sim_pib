package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.repository.mongo.ParamInfoRepository;
import edu.uestc.iscssl.itsbackend.service.ParamInfoService;
import edu.uestc.iscssl.itsbackend.utils.ParamInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParamInfoServiceImpl implements ParamInfoService {

    @Autowired
    private ParamInfoRepository paramInfoRepository;
    @Override
    public ParamInfo addParam(ParamInfo param) {
        return paramInfoRepository.save(param);
    }

    @Override
    public ParamInfo findParamById(String simulationId) {
        return paramInfoRepository.findParamInfoBySimulationId(simulationId).get();
    }
    @Override
    public List<ParamInfo> findParams() {
        return paramInfoRepository.findAll();
    }
}
