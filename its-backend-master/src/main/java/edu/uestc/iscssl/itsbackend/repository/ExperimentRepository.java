package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
@Repository
public interface ExperimentRepository extends JpaRepository<ExperimentEntity,Integer> {

    @Query(value = "select report_id from experiment where user_id = :userId",nativeQuery = true)
    List<String> findExperimentId(Long userId);

    @Query(value = "select report_id from experiment where id = :reportid",nativeQuery = true)
    String findExperimentReportId(String reportid);

    @Query(value = "select count(report_id) from experiment where user_id = :userId",nativeQuery = true)
    int findCount(long userId);

    @Query(value = "select type,count(type) AS num from experiment where user_id = :userId group by type",nativeQuery = true)
    List<Map<Integer, Integer>> countTypeByUserId(@Param("userId") long userId);

    @Query(value = "select type,count(type) AS num from experiment group by type",nativeQuery = true)
    List<Map<Integer, Integer>> countTypes();

    List<ExperimentEntity> findByExperimentId(String experimentId);
    Page<ExperimentEntity> findByExperimentNameContainingAndSimIdNotNull(String ExperimentName, Pageable pageable);
    Page<ExperimentEntity> findByExperimentNameContainingAndUserIdAndSimIdNotNull(String ExperimentName,long userId,Pageable pageable);
    Page<ExperimentEntity> findByUserIdAndSimIdNotNull(long userId,Pageable pageable);
    Page<ExperimentEntity> findAllBySimIdNotNull(Pageable pageable);
    void deleteExperimentEntityByExperimentId(String experimentId);

    @Query(value = "SELECT COUNT(DISTINCT experiment_id) FROM experiment WHERE user_id = :userId", nativeQuery = true)
    int countByUserId(@Param("userId") long userId);
}
