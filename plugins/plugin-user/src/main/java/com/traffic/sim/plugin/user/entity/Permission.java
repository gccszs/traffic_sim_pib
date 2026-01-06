package com.traffic.sim.plugin.user.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 权限实体
 * 
 * @author traffic-sim
 */
@Entity
@Table(name = "permission")
@Data
public class Permission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;
    
    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String permissionCode;
    
    @Column(length = 500)
    private String description;
}

