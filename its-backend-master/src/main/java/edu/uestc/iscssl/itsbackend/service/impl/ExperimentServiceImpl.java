package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentEntity;
import edu.uestc.iscssl.itsbackend.repository.ExperimentRepository;
import edu.uestc.iscssl.itsbackend.service.ExperimentService;
import edu.uestc.iscssl.itsbackend.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
@Transactional
@Service
public class ExperimentServiceImpl implements ExperimentService {

    @Autowired
    private ExperimentRepository experimentRepository;
    @Override
    public List<String> findExperimentId(Long userId) {
        return experimentRepository.findExperimentId(userId);
    }

    @Override
    public String findExperimentReportId(String reportid) {
        return experimentRepository.findExperimentReportId(reportid);
    }

    @Override
    public int findCount(long userId) {
        return experimentRepository.findCount(userId);
    }

    @Override
    public ExperimentEntity findExperimentEntityByExperimentId(String experimentId) {
        List<ExperimentEntity> list = experimentRepository.findByExperimentId(experimentId);
        if(list.size() == 0){//实验报告中的实验id，在实验记录表中不存在；（理想情况下不会出现）
            return null;
        }
        return list.get(0);
    }

    @Override
    public ExperimentEntity saveExperiment(ExperimentEntity experimentEntity) {
        return experimentRepository.save(experimentEntity);
    }

    @Override
    public List<Map<Integer, Integer>> countTypeByUserId(long userId) {
        return experimentRepository.countTypeByUserId(userId);
    }

    @Override
    public List<ExperimentEntity> findByExperimentId(String experimentId) {
        return experimentRepository.findByExperimentId(experimentId);
    }

    @Override
    public Page<ExperimentEntity> findByExperimentNameLike(String experimentName, Pageable pageable, boolean isManager) {
        if (isManager)
            return experimentRepository.findByExperimentNameContainingAndSimIdNotNull(experimentName,pageable);
        else{
            return experimentRepository.findByExperimentNameContainingAndUserIdAndSimIdNotNull(experimentName, UserUtils.getUserId(),pageable);
        }
    }

    @Override
    public Page<ExperimentEntity> findAll(Pageable pageable) {
        return experimentRepository.findAllBySimIdNotNull(pageable);
    }

    @Override
    public Page<ExperimentEntity> findByUserId(long userId, Pageable pageable) {
        return experimentRepository.findByUserIdAndSimIdNotNull(userId,pageable);
    }

    @Override
    public void deleteExperimentByExperimentId(String experimentId) {
        List<ExperimentEntity> experimentEntities = experimentRepository.findByExperimentId(experimentId);
        if (experimentEntities != null)
            for (ExperimentEntity experimentEntity : experimentEntities)
                experimentRepository.deleteExperimentEntityByExperimentId(experimentId);
    }

    @Override
    public List<Map<Integer, Integer>> countTypes() {
        return experimentRepository.countTypes();
    }

    @Override
    public int getExperimentNumberByUserId(long userId) {
        return experimentRepository.countByUserId(userId);
    }
}
