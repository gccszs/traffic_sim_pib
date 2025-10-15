package edu.uestc.iscssl.itsbackend.domain.Experiment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "experiment_data")
@ApiModel(value = "实验数据对象")
public class ExperimentDataEntity implements Serializable {

    private static final long serialVersionUID = 822452293998233593L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ApiModelProperty(value = "实验数据序号")
    private int id;

    @Lob
    @ApiModelProperty(value = "车流统计")
    @Column(name = "vehicle_statistical",columnDefinition = "text")
    private String vehicleStatistical;

    @Lob
    @ApiModelProperty(value = "车辆参数")
    @Column(name = "vehicle_param",columnDefinition = "text")
    private String vehicleParam;

    @Lob
    @ApiModelProperty(value = "排队信息")
    @Column(name = "line_info",columnDefinition = "text")
    private String lineInfo;

    @Lob
    @ApiModelProperty(value = "拥堵指数")
    @Column(name = "congestion_index",columnDefinition = "text")
    private String congestionIndex;

    @Lob
    @ApiModelProperty(value = "停车信息")
    @Column(name = "park_info",columnDefinition = "text")
    private String parkInfo;

    @Lob
    @ApiModelProperty(value = "通行能力")
    @Column(name = "traffic_capacity",columnDefinition = "text")
    private String trafficCapacity;

    @ApiModelProperty(value = "实验id")
    @Column(name = "experiment_id")
    private String experimentId;


    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVehicleStatistical() {
        return vehicleStatistical;
    }

    public void setVehicleStatistical(String vehicleStatistical) {
        this.vehicleStatistical = vehicleStatistical;
    }

    public String getVehicleParam() {
        return vehicleParam;
    }

    public void setVehicleParam(String vehicleParam) {
        this.vehicleParam = vehicleParam;
    }

    public String getLineInfo() {
        return lineInfo;
    }

    public void setLineInfo(String lineInfo) {
        this.lineInfo = lineInfo;
    }

    public String getCongestionIndex() {
        return congestionIndex;
    }

    public void setCongestionIndex(String congestionIndex) {
        this.congestionIndex = congestionIndex;
    }

    public String getParkInfo() {
        return parkInfo;
    }

    public void setParkInfo(String parkInfo) {
        this.parkInfo = parkInfo;
    }

    public String getTrafficCapacity() {
        return trafficCapacity;
    }

    public void setTrafficCapacity(String trafficCapacity) {
        this.trafficCapacity = trafficCapacity;
    }
}
