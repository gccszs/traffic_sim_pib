package edu.uestc.iscssl.itsbackend.domain.user;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Table(name = "user")
@Entity
@ApiModel(value = "用戶信息")
@EntityListeners(AuditingEntityListener.class)
public class UserEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long userId;

    @ApiModelProperty(value = "用戶名 ")
    String userName;
    @ApiModelProperty(value = "密码")
    String password;
    @ApiModelProperty(value = "角色Id ")
    long roleId;
    @ApiModelProperty(value = "邮箱 ")
    String email;
    @ApiModelProperty(value = "机构名 ")
    String institution;
    @ApiModelProperty(value = "联系电话 ")
    String phoneNumber;
    @ApiModelProperty(value = "创建用户时间 ")
    @CreatedDate
    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    Timestamp createTime;
    @ApiModelProperty(value = "最后修改时间 ")
    @LastModifiedDate
    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    Timestamp updateTime;
    @ApiModelProperty(value = "最后修改人 ")
    String modefiedBy;
    @ApiModelProperty(value = "当前状态 ")
    Status status;
    @ApiModelProperty(value = "区别是否是ilab用户,以及新用户(目前将使用过注册和更改密码功能的用户其type设置为3)")
    int type;
    @ApiModelProperty(value = "实验空间的用户名称")
    String dis;
    @ApiModelProperty(value = "实验空间的")
    String ilabId;
    public enum Status{
        unNormal,normal,first,ban;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getModefiedBy() {
        return modefiedBy;
    }

    public void setModefiedBy(String modefiedBy) {
        this.modefiedBy = modefiedBy;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDis() {
        return dis;
    }

    public void setDis(String dis) {
        this.dis = dis;
    }

    public String getIlabId() {
        return ilabId;
    }

    public void setIlabId(String ilabId) {
        this.ilabId = ilabId;
    }
}
