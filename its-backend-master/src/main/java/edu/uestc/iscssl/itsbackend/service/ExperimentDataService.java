package edu.uestc.iscssl.itsbackend.service;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentDataEntity;


public interface ExperimentDataService {
    ExperimentDataEntity insertExperimentData(ExperimentDataEntity experimentData);

    ExperimentDataEntity fingExperimentDataById(String experimentId);

    void deleteExperimentDataByExperimentId(String simulationId);
}
