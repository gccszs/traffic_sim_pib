package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.domain.user.LoginRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;

@Repository
public interface LoginRepository extends JpaRepository<LoginRecordEntity,Long>, JpaSpecificationExecutor<LoginRecordEntity> {

    @Query(value = "select id, user_id, error_count, ip, update_time from login_record where user_id = :userId", nativeQuery = true)
    LoginRecordEntity findLoginRecordByUserId(Long userId);

    @Modifying
    @Query(value = "update login_record set ip = :ip,update_time = :updateTime,error_count = :errorCount where id = :id",nativeQuery = true)
    @Transactional
    void updateLoginEntityById(@Param("ip")String ip, @Param("updateTime")Timestamp updateTime, @Param("errorCount")Long errorCount, @Param("id")Long id);

    @Modifying
    @Query(value = "insert into login_record (user_id, error_count, ip, update_time) values (:userId, :errorCount, :ip, :updateTime)",nativeQuery = true)
    @Transactional
    void addLoginEntity(@Param("ip")String ip, @Param("userId")Long userId, @Param("errorCount")Long errorCount, @Param("updateTime")Timestamp updateTime);
}
