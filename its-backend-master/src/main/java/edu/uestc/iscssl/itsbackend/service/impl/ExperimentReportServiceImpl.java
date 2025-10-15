package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentReportEntity;
import edu.uestc.iscssl.itsbackend.repository.ExperimentReportRepository;
import edu.uestc.iscssl.itsbackend.service.ExperimentReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
@Transactional
@Service
public class ExperimentReportServiceImpl implements ExperimentReportService {

    @Autowired
    private ExperimentReportRepository experimentReportRepository;

    //生成实验报告
    @Override
    public ExperimentReportEntity insertExperimentReport(ExperimentReportEntity experimentReportEntity) {
        return experimentReportRepository.save(experimentReportEntity);
    }

    @Override
    public void deleteExperimentReport(Integer id) {
        experimentReportRepository.deleteById(id);
    }

    @Override
    public void deleteExperimentReportByReportId(String reportId) {
        ExperimentReportEntity experimentReportEntity = experimentReportRepository.findExperimentReportById(reportId);
        if (experimentReportEntity != null)
            experimentReportRepository.deleteExperimentReportEntityByReportid(reportId);
    }

    @Override
    public Page<ExperimentReportEntity> findExperimentReportByIdList(List<String> idList,Integer pageNumber,Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber-1,pageSize,Sort.Direction.DESC,"experimentTime");
        return experimentReportRepository.findExperimentReportEntitiesByReportidIn(idList,pageable);
    }

    @Override
    public Page<ExperimentReportEntity> findExperimentReportByIdListAndExperimentName(List<String> idList, Integer pageNumber, Integer pageSize, String experimentName) {
        Pageable pageable = PageRequest.of(pageNumber-1,pageSize,Sort.Direction.DESC,"experimentTime");
        return experimentReportRepository.findExperimentReportEntitiesByReportidInAndExperimentNameContaining(idList,experimentName,pageable);
    }

    @Override
    public Page<ExperimentReportEntity> findExperimentReport(Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber-1,pageSize,Sort.Direction.DESC,"experimentTime");
        return experimentReportRepository.findAll(pageable);
    }

    @Override
    public ExperimentReportEntity findExperimentReportById(String experimentReportId) {
        return experimentReportRepository.findExperimentReportById(experimentReportId);
    }

    @Override
    public void updateExperimentReport(String filePath,String id) {
        experimentReportRepository.updateExperimentReport(filePath,id);
    }

    //修改实验报告
    @Override
    public int modifyExperimentReport(ExperimentReportEntity experimentReportEntity,String reportid) {
        experimentReportRepository.modifyExperimentReport(experimentReportEntity,reportid);
        if(experimentReportEntity.getExperimentName()==null || experimentReportEntity.getExperimentObjective()==null
                || experimentReportEntity.getExperimentPrinciple()==null || experimentReportEntity.getExperimentContent()==null
                || experimentReportEntity.getExperimentStep()==null || experimentReportEntity.getExperimentResult()==null
                || experimentReportEntity.getExperimentSummary()==null || experimentReportEntity.getExperimentProposal()==null){
            experimentReportEntity.setStatus(2);
        }else {
            experimentReportEntity.setStatus(1);
        }
        return experimentReportRepository.modifyExperimentReportStatus(experimentReportEntity.getStatus(),reportid);
    }

    @Override
    public ExperimentReportEntity findExperiment(String reportid) {
        return experimentReportRepository.findExperiment(reportid);
    }

}
