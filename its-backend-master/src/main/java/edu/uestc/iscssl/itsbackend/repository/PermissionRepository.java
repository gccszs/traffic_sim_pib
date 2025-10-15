package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.domain.user.PermissionEntity;
import edu.uestc.iscssl.itsbackend.domain.user.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 22:30 2019/4/14
 */
public interface PermissionRepository extends JpaRepository<PermissionEntity,RoleEntity> {
    @Query(nativeQuery = true)
    List<PermissionEntity> findPermissionEntitiesByRoleEntities(RoleEntity roleEntity);

}

