package edu.uestc.iscssl.itsbackend.domain.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

@Table(name = "login_log")
@Entity
@ApiModel(value = "登录信息记录")
public class LoginLogEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name = "user_id")
    @ApiModelProperty(value = "用户id")
    long userId;

    @ApiModelProperty(value = "错误ip")
    String ip;

    @ApiModelProperty(value = "最后一次错误登录时间 ")
    @LastModifiedDate
    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    @Column(name = "update_time")
    Timestamp updateTime;

    public LoginLogEntity() {
    }

    public LoginLogEntity(String ipAddr, long userId) {
        this.ip = ipAddr;
        this.userId = userId;
        this.updateTime = new Timestamp(new Date().getTime());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
}
