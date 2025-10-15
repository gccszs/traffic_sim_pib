package edu.uestc.iscssl.itsbackend.domain.Experiment;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "experiment_report")
@ApiModel(value = "实验报告对象")
public class ExperimentReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "实验报告序号")
    @Column(name = "id")
    private int id;

    @ApiModelProperty(value = "实验报告id")
    @Column(name = "reportid")
    private String reportid;

    @ApiModelProperty(value = "学生姓名")
    @Column(name = "stu_name")
    private String stuName;

    @ApiModelProperty(value = "学号")
    @Column(name = "stu_id")
    private String stuId;

    @ApiModelProperty(value = "课程名称")
    @Column(name = "course_name")
    private String courseName;

    @ApiModelProperty(value = "理论教师")
    @Column(name = "theory_teacher")
    private String theoryTeacher;

    @ApiModelProperty(value = "实验教师")
    @Column(name = "experiment_teacher")
    private String experimentTeacher;

    @ApiModelProperty(value = "实验地点")
    @Column(name = "experiment_location")
    private String experimentLocation;

    @ApiModelProperty(value = "实验时间")
    @Column(name = "experiment_time")
    private Date experimentTime;

    @ApiModelProperty(value = "实验名称")
    @Column(name = "experiment_name")
    private String experimentName;

    @Lob
    @ApiModelProperty(value = "实验目的")
    @Column(name = "experiment_objective",columnDefinition = "text")
    private String experimentObjective;

    @Lob
    @ApiModelProperty(value = "实验原理" )
    @Column(name = "experiment_principle",columnDefinition = "text")
    private String experimentPrinciple;

    @Lob
    @ApiModelProperty(value = "实验内容")
    @Column(name = "experiment_content",columnDefinition = "text")
    private String experimentContent;

    @Lob
    @ApiModelProperty(value = "实验步骤")
    @Column(name = "experiment_step",columnDefinition = "text")
    private String experimentStep;

    @Lob
    @ApiModelProperty(value = "实验结果及分析")
    @Column(name = "experiment_result",columnDefinition = "text")
    private String experimentResult;

    @Lob
    @ApiModelProperty(value = "总结及心得体会")
    @Column(name = "experiment_summary",columnDefinition = "text")
    private String experimentSummary;

    @Lob
    @ApiModelProperty(value = "对本实验过程及方法、手段的改进建议")
    @Column(name = "experiment_proposal",columnDefinition = "text")
    private String experimentProposal;

    @ApiModelProperty(value = "实验报告填写进度")
    @Column(name = "status")
    private Integer status;

    @ApiModelProperty(value = "实验报告pdf地址")
    @Column(name="pdf_address")
    private String pdfAddress;

    @OneToOne(cascade = {CascadeType.REMOVE,CascadeType.PERSIST,CascadeType.MERGE})
    @JoinColumn(name = "experimentDataId",referencedColumnName = "id")
    private ExperimentDataEntity experimentData;

    @OneToOne(cascade = {CascadeType.REMOVE,CascadeType.PERSIST,CascadeType.MERGE})
    @JoinColumn(name = "experimentId",referencedColumnName = "id")
    private ExperimentEntity experimentEntity;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}
