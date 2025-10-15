package edu.uestc.iscssl.itsbackend.service;


import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ExperimentService {
    List<String> findExperimentId(Long userId);

    String findExperimentReportId(String reportid);

    int findCount(long userId);
    ExperimentEntity findExperimentEntityByExperimentId(String experimentId);
    //插入及更新实验记录
    ExperimentEntity saveExperiment(ExperimentEntity experimentEntity);
    List<Map<Integer,Integer>> countTypeByUserId(long userId);
    //根据实验Id查找实验
    List<ExperimentEntity> findByExperimentId(String experimentId);
    //根据实验名查找实验
    Page<ExperimentEntity> findByExperimentNameLike(String experimentName, Pageable pageable, boolean isManager);
    //根据分页查找实验
    Page<ExperimentEntity> findAll(Pageable pageable);
    //根据用户ID和分页查找实验列表
    Page<ExperimentEntity> findByUserId(long userId,Pageable pageable);
    //管理员统计各类型的次数
    List<Map<Integer,Integer>> countTypes();
    void deleteExperimentByExperimentId(String experimentId);

    int getExperimentNumberByUserId(long userId);
}
