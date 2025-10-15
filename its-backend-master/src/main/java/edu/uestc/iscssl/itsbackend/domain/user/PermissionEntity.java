package edu.uestc.iscssl.itsbackend.domain.user;

import jakarta.persistence.*;
import java.util.Set;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 22:31 2019/4/14
 */
@Table(name = "permission")
@Entity
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long permissionId;
    String permissionName;
    @ManyToMany(targetEntity = RoleEntity.class,cascade = CascadeType.PERSIST, fetch=FetchType.LAZY)
    @JoinTable(name="role_permission", joinColumns={@JoinColumn(name="permission_id")}, inverseJoinColumns={@JoinColumn(name="role_id")})
    private Set<RoleEntity> roleEntities;

    public long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(long permissionId) {
        this.permissionId = permissionId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
}
