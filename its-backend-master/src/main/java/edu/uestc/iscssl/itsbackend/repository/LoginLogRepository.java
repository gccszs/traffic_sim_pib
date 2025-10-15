package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.domain.user.LoginLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLogEntity,Long>, JpaSpecificationExecutor<LoginLogEntity> {

    @Modifying
    @Query(value = "insert into login_log (user_id,ip,update_time) values (:userId,:ip,:updateTime)",nativeQuery = true)
    @Transactional
    void addLoginLog(@Param("userId")long userId, @Param("ip")String ip, @Param("updateTime") Timestamp updateTime);
}
