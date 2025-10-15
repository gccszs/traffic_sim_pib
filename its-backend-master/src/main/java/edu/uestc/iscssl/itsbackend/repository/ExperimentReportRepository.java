package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;
import java.util.List;
public interface ExperimentReportRepository extends JpaRepository<ExperimentReportEntity,Integer> , JpaSpecificationExecutor<ExperimentReportEntity> {

    @Query(value = "select * from experiment_report where reportid = :experimentReportId",nativeQuery = true)
    ExperimentReportEntity findExperimentReportById(String experimentReportId);

    @Query(value = "select * from experiment_report where reportid in (:idList) limit :offset,:pageSize",nativeQuery = true)
    List<ExperimentReportEntity> findExperimentReportByIdList(@Param(value = "idList") List<Integer> idList, @Param("offset") int offset, @Param("pageSize") int pageSize);
    Page<ExperimentReportEntity> findExperimentReportEntitiesByReportidIn(List<String> reportid, Pageable pageable);
    Page<ExperimentReportEntity> findExperimentReportEntitiesByReportidInAndExperimentNameContaining(List<String> reportid, String experimentName, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "update experiment_report set pdf_address = :filePath where reportid = :id",nativeQuery = true)
    void updateExperimentReport(@Param("filePath") String filePath,@Param("id") String id);

    @Modifying
    @Transactional
    @Query(value = "update experiment_report set stu_name = :#{#ER.stuName},stu_id = :#{#ER.stuId},course_name = :#{#ER.courseName},theory_teacher = :#{#ER.theoryTeacher},experiment_teacher = :#{#ER.experimentTeacher},experiment_location = :#{#ER.experimentLocation},experiment_name = :#{#ER.experimentName},experiment_objective = :#{#ER.experimentObjective},experiment_principle = :#{#ER.experimentPrinciple},experiment_content = :#{#ER.experimentContent},experiment_step = :#{#ER.experimentStep},experiment_result = :#{#ER.experimentResult},experiment_summary = :#{#ER.experimentSummary},experiment_proposal = :#{#ER.experimentProposal},status = :#{#ER.status},pdf_address = :#{#ER.pdfAddress} where reportid = :reportid",nativeQuery = true)
    int modifyExperimentReport(@Param("ER") ExperimentReportEntity experimentReportEntity, @Param("reportid") String reportid);

    @Modifying
    @Transactional
    @Query(value = "update experiment_report set status = :status where reportid = :id",nativeQuery = true)
    int modifyExperimentReportStatus(Integer status,String id);

    @Query(value = "select * from experiment_report where reportid = :reportid",nativeQuery = true)
    ExperimentReportEntity findExperiment(String reportid);
    void deleteExperimentReportEntityByReportid(String reportid);
}
