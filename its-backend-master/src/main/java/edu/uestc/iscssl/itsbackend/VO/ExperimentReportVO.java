package edu.uestc.iscssl.itsbackend.VO;

import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentDataEntity;
import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentEntity;
import edu.uestc.iscssl.itsbackend.domain.Experiment.ExperimentReportEntity;

import jakarta.persistence.Id;
import java.util.Date;

public class ExperimentReportVO {

    @Id
    private int id;
    private String reportid;
    private String stuName;
    private String stuId;
    private String courseName;
    private String theoryTeacher;
    private String experimentTeacher;
    private String experimentLocation;
    private Date experimentTime;
    private String experimentName;
    private String experimentObjective;
    private String experimentPrinciple;
    private String experimentContent;
    private String experimentStep;
    private String experimentResult;
    private String experimentSummary;
    private String experimentProposal;
    private Integer status;
    private String pdfAddress;
    private ExperimentDataEntity experimentData;
    private ExperimentEntity experimentEntity;
    private String userName;
    private Integer type;

    public ExperimentReportVO(ExperimentReportEntity experimentReportEntity,String userName,Integer type){
        this.setId(experimentReportEntity.getId());
        this.setReportid(experimentReportEntity.getReportid());
        this.setStuName(experimentReportEntity.getStuName());
        this.setStuId(experimentReportEntity.getStuId());
        this.setId(experimentReportEntity.getId());
        this.setCourseName(experimentReportEntity.getCourseName());
        this.setTheoryTeacher(experimentReportEntity.getTheoryTeacher());
        this.setExperimentTeacher(experimentReportEntity.getExperimentTeacher());
        this.setExperimentLocation(experimentReportEntity.getExperimentLocation());
        this.setExperimentTime(experimentReportEntity.getExperimentTime());
        this.setExperimentName(experimentReportEntity.getExperimentName());
        this.setExperimentObjective(experimentReportEntity.getExperimentObjective());
        this.setExperimentPrinciple(experimentReportEntity.getExperimentPrinciple());
        this.setExperimentContent(experimentReportEntity.getExperimentContent());
        this.setExperimentStep(experimentReportEntity.getExperimentStep());
        this.setExperimentResult(experimentReportEntity.getExperimentResult());
        this.setExperimentSummary(experimentReportEntity.getExperimentSummary());
        this.setExperimentProposal(experimentReportEntity.getExperimentProposal());
        this.setStatus(experimentReportEntity.getStatus());
        this.setPdfAddress(experimentReportEntity.getPdfAddress());
        this.setExperimentData(experimentReportEntity.getExperimentData());
        this.setExperimentEntity(experimentReportEntity.getExperimentEntity());
        this.setUserName(userName);
        this.setType(type);
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getReportid() {
        return reportid;
    }

    public void setReportid(String reportid) {
        this.reportid = reportid;
    }

    public String getStuName() {
        return stuName;
    }

    public void setStuName(String stuName) {
        this.stuName = stuName;
    }

    public String getStuId() {
        return stuId;
    }

    public void setStuId(String stuId) {
        this.stuId = stuId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTheoryTeacher() {
        return theoryTeacher;
    }

    public void setTheoryTeacher(String theoryTeacher) {
        this.theoryTeacher = theoryTeacher;
    }

    public String getExperimentTeacher() {
        return experimentTeacher;
    }

    public void setExperimentTeacher(String experimentTeacher) {
        this.experimentTeacher = experimentTeacher;
    }

    public String getExperimentLocation() {
        return experimentLocation;
    }

    public void setExperimentLocation(String experimentLocation) {
        this.experimentLocation = experimentLocation;
    }

    public Date getExperimentTime() {
        return experimentTime;
    }

    public void setExperimentTime(Date experimentTime) {
        this.experimentTime = experimentTime;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public String getExperimentObjective() {
        return experimentObjective;
    }

    public void setExperimentObjective(String experimentObjective) {
        this.experimentObjective = experimentObjective;
    }

    public String getExperimentPrinciple() {
        return experimentPrinciple;
    }

    public void setExperimentPrinciple(String experimentPrinciple) {
        this.experimentPrinciple = experimentPrinciple;
    }

    public String getExperimentContent() {
        return experimentContent;
    }

    public void setExperimentContent(String experimentContent) {
        this.experimentContent = experimentContent;
    }

    public String getExperimentStep() {
        return experimentStep;
    }

    public void setExperimentStep(String experimentStep) {
        this.experimentStep = experimentStep;
    }

    public String getExperimentResult() {
        return experimentResult;
    }

    public void setExperimentResult(String experimentResult) {
        this.experimentResult = experimentResult;
    }

    public String getExperimentSummary() {
        return experimentSummary;
    }

    public void setExperimentSummary(String experimentSummary) {
        this.experimentSummary = experimentSummary;
    }

    public String getExperimentProposal() {
        return experimentProposal;
    }

    public void setExperimentProposal(String experimentProposal) {
        this.experimentProposal = experimentProposal;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPdfAddress() {
        return pdfAddress;
    }

    public void setPdfAddress(String pdfAddress) {
        this.pdfAddress = pdfAddress;
    }

    public ExperimentDataEntity getExperimentData() {
        return experimentData;
    }

    public void setExperimentData(ExperimentDataEntity experimentData) {
        this.experimentData = experimentData;
    }

    public ExperimentEntity getExperimentEntity() {
        return experimentEntity;
    }

    public void setExperimentEntity(ExperimentEntity experimentEntity) {
        this.experimentEntity = experimentEntity;
    }
}
