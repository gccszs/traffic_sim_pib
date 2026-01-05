package com.traffic.sim.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户DTO
 * 
 * @author traffic-sim
 */
@Data
public class UserDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String institution;
    private Integer roleId;
    private String roleName;
    private String status;
    private Date createTime;
    private Date updateTime;
}

