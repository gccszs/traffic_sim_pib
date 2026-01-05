package com.traffic.sim.plugin.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

/**
 * 用户实体
 * 
 * @author traffic-sim
 */
@Entity
@Table(name = "user")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Column(length = 100)
    private String email;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(length = 200)
    private String institution;
    
    @Column(name = "role_id")
    private Integer roleId;
    
    @Column(nullable = false, length = 20)
    private String status;
    
    @Column(name = "create_time", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
    
    @Column(name = "update_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
        if (status == null || status.isEmpty()) {
            status = "NORMAL";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}

