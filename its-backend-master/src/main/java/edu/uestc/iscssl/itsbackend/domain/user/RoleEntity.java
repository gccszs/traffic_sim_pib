package edu.uestc.iscssl.itsbackend.domain.user;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Set;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 12:23 2019/4/14
 */
@Table(name = "role")
@Entity
public class RoleEntity  implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long roleId;
    String roleName;

    @ManyToMany(targetEntity = PermissionEntity.class, cascade = CascadeType.PERSIST, fetch=FetchType.LAZY)
    @JoinTable(name="role_permission",joinColumns={@JoinColumn(name="role_id")},inverseJoinColumns={@JoinColumn(name="permission_id")})
    private Set<PermissionEntity> permissionEntities;

    public long getRoleId() {
        return roleId;
    }

    public void setRoleId(long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
