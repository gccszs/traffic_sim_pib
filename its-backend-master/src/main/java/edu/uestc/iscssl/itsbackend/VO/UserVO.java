package edu.uestc.iscssl.itsbackend.VO;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 13:28 2019/6/19
 */

@Entity
@Table(name = "view")
public class UserVO implements Serializable{
    @Id
    @Column(name = "user_id")
    long userId;
    @Column(name = "role_id")
    long roleId;
    @Column(name = "user_name")
    String userName;
    @Column(name = "email")
    String email;
    @Column(name = "institution")
    String institution;
    @Column(name = "phone_number")
    String phoneNumber;
    @Column(name = "create_time")
    Timestamp createTime;
    @Column(name = "update_time")
    Timestamp updateTime;
    @Column(name = "modefied_by")
    String modefiedBy;
    @Column(name = "status")
    UserVO.Status status;

    public enum Status{
        unNormal,normal,first,ban;
    }
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
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

    public String getModefiedBy() {
        return modefiedBy;
    }

    public void setModefiedBy(String modefiedBy) {
        this.modefiedBy = modefiedBy;
    }

    public UserVO.Status getStatus() {
        return status;
    }

    public void setStatus(UserVO.Status status) {
        this.status = status;
    }
}
