package edu.uestc.iscssl.itsbackend.controller;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentDataEntity;
import edu.uestc.iscssl.itsbackend.service.ExperimentDataService;
import edu.uestc.iscssl.itsbackend.utils.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@Api("与实验数据相关的接口")
public class ExperimentDataController {

    @Autowired
    private ExperimentDataService experimentDataService;

    @ApiOperation(value = "存储实验数据图像")
    @PostMapping(value = "saveExperimentData")
    public R insertExperimentData(@RequestBody ExperimentDataEntity experimentDataEntity) {
        Map<String,Object> result = new HashMap<>();
        if(experimentDataService.fingExperimentDataById(experimentDataEntity.getExperimentId()) == null) {
            ExperimentDataEntity dataEntity = experimentDataService.insertExperimentData(experimentDataEntity);
            if(dataEntity !=null){
                result.put("msg","Save ExperimentData Success");
            }else {
                result.put("msg","Save ExperimentData Failed");
            }
        }else{
         result.put("msg","The ExperimentData Already Exist,Save Failed");
        }
        return R.ok(result);
    }
}
