package edu.uestc.iscssl.itsbackend.domain.Experiment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "experiment")
@ApiModel(value = "实验对象")
public class ExperimentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "实验id")
    @Column(name = "id")
    private Integer id;

    @ApiModelProperty(value = "实验id")
    @Column(name = "experiment_id")
    private String experimentId;

    @ApiModelProperty(value = "实验名称")
    @Column(name = "experiment_name")
    private String experimentName;

    @ApiModelProperty(value = "实验类型")
    @Column(name = "type")
    private Integer type;

    @ApiModelProperty(value = "实验地图")
    @Column(name = "map_name")
    private String mapName;

    @ApiModelProperty(value = "用户id")
    @Column(name = "user_id")
    private Long userId;

    @ApiModelProperty(value = "仿真id")
    @Column(name = "sim_id")
    private String simId;

    @ApiModelProperty(value = "实验报告id")
    @Column(name = "report_id")
    private String reportId;

    @ApiModelProperty(value = "创建时间")
    @Column(name = "create_time")
    private Date createTime;

//    @OneToMany
//    @JoinColumn(name = "id")
//    private ExperimentReportEntity reportVO;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getSimId() {
        return simId;
    }

    public void setSimId(String simId) {
        this.simId = simId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }
}
