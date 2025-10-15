package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.repository.mongo.DataInfoRepository;
import edu.uestc.iscssl.itsbackend.service.DataInfoService;
import edu.uestc.iscssl.itsbackend.utils.DataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataInfoServiceImpl implements DataInfoService {

    @Autowired
    private DataInfoRepository dataInfoRepository;
    @Override
    public DataInfo addDataInfo(DataInfo dataInfo) {
        return dataInfoRepository.save(dataInfo);

    }
}
