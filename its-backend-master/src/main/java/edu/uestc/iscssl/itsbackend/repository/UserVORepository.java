package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.VO.UserVO;
import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;

public interface UserVORepository extends JpaRepository<UserVO,Long>, JpaSpecificationExecutor<UserVO> {
    @Query(value = "select user_id, role_id, user_name, email, institution, status, phone_number, create_time,update_time, modefied_by from user where user_name = :userName",nativeQuery = true)
    UserVO findUserVOByUserName(@Param("userName") String username);

    int countByStatus(UserVO.Status status);
}
