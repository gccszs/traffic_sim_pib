package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentDataEntity;
import edu.uestc.iscssl.itsbackend.repository.ExperimentDataRepository;
import edu.uestc.iscssl.itsbackend.service.ExperimentDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class ExperimentDataServiceImpl implements ExperimentDataService {

    @Autowired
    private ExperimentDataRepository experimentDataRepository;

    @Override
    public ExperimentDataEntity insertExperimentData(ExperimentDataEntity experimentData) {
        return experimentDataRepository.save(experimentData);
    }

    @Override
    public ExperimentDataEntity fingExperimentDataById(String experimentId) {
        return experimentDataRepository.fingExperimentDataById(experimentId);
    }

    @Override
    public void deleteExperimentDataByExperimentId(String simulationId) {
        ExperimentDataEntity experimentDataEntity = experimentDataRepository.fingExperimentDataById(simulationId);
        if (experimentDataEntity != null)
            experimentDataRepository.deleteByExperimentId(simulationId);
    }
}
