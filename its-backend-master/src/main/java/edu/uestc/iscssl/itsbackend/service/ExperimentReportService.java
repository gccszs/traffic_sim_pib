package edu.uestc.iscssl.itsbackend.service;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentReportEntity;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ExperimentReportService {

    ExperimentReportEntity insertExperimentReport(ExperimentReportEntity experimentReportEntity);

    void deleteExperimentReport(Integer id);
    void deleteExperimentReportByReportId(String reportId);
    Page<ExperimentReportEntity> findExperimentReportByIdList(List<String> idList, Integer pageNumber, Integer pageSize);
    Page<ExperimentReportEntity> findExperimentReportByIdListAndExperimentName(List<String> idList, Integer pageNumber, Integer pageSize, String experimentName);
    Page<ExperimentReportEntity> findExperimentReport(Integer pageNumber, Integer pageSize);
    ExperimentReportEntity findExperimentReportById(String experimentReportId);

    void updateExperimentReport(String filePath,String id);

    int modifyExperimentReport(ExperimentReportEntity experimentReportEntity, String reportid);

    ExperimentReportEntity findExperiment(String reportid);
}
