package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface ExperimentDataRepository extends JpaRepository<ExperimentDataEntity,Integer> {

    @Query(value = "select * from experiment_data where experiment_id = :experimentId",nativeQuery = true)
    ExperimentDataEntity fingExperimentDataById(String experimentId);

    void deleteByExperimentId(String experimentId);
}
