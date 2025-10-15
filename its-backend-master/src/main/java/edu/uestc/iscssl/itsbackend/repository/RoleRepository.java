package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.domain.user.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 22:08 2019/4/14
 */
public interface RoleRepository extends JpaRepository<RoleEntity,Long>{

    RoleEntity findRoleEntityByRoleId(Long roleId);
}
