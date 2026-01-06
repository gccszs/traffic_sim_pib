package com.traffic.sim.plugin.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

/**
 * 角色实体
 * 
 * @author traffic-sim
 */
@Entity
@Table(name = "role")
@Data
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;
    
    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private String roleCode;
    
    @Column(length = 500)
    private String description;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private List<Permission> permissions;
}

