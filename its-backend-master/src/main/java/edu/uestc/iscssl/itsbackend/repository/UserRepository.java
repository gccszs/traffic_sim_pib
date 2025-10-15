package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
@Repository
public interface UserRepository extends JpaRepository<UserEntity,Long>, JpaSpecificationExecutor<UserEntity> {
    UserEntity findUserEntityByUserName(String userName);
    UserEntity findUserEntityByUserId(Long id);
    int countByRoleId(long roleId);

    /**报错：
     * java.sql.SQLException: Can not issue data manipulation statements with executeQuery().
     * 解决方式：
     * 在修改方法上面添加@Modifying 注解
     * 报错：
     * javax.persistence.TransactionRequiredException: Executing an update/delete query
     * 解决方式：
     * 在UserRepository.java类上添加@Transactional注解
     */
    @Modifying
    @Query(value = "update user set user_name = :userName,password = :password,email = :email,institution = :institution,phone_number = :phoneNumber, modefied_by = :modefiedBy where user_id = :userId",nativeQuery = true)
    @Transactional
    void updateInfoById(@Param("userId") Long id, @Param("userName") String userName, @Param("password") String password,@Param("email") String email,@Param("institution") String institution,@Param("phoneNumber")String phoneNumber,@Param("modefiedBy")String modefiedBy);

    /**
     * 删除
     * @param userId
     */
    @Transactional
    void deleteUserEntitiesByUserId(Long userId);
    /**
     * 删除2
     * @param userId
     */
    @Modifying
    @Query(value = "update user set status = :status, modefied_by = :modefiedBy where user_id = :userId",nativeQuery = true)
    @Transactional
    void deleteByStatus(@Param("userId") Long userId,@Param("status") int status,@Param("modefiedBy") String modefiedBy);

    @Modifying
    @Query(value = "insert into user (role_id,user_name,password,email,institution,phone_number,modefied_by,status) values (:roleId,:userName,:password,:email,:institution,:phoneNumber,:modefiedBy,:status)",nativeQuery = true)
    @Transactional
    void addInfoById(@Param("userName") String userName, @Param("password") String password,@Param("roleId") Long roleId,@Param("email") String email,@Param("institution") String institution,@Param("phoneNumber")String phoneNumber,@Param("modefiedBy")String modefiedBy,@Param("status")int status);

    @Override
    <S extends UserEntity> S saveAndFlush(S s);

    int countByUserName(String username);

    int countByStatus(UserEntity.Status status);

    @Modifying
    @Query(value = "update user set status = :status, modefied_by = :userName where user_id = :userId",nativeQuery = true)
    @Transactional
    void updateStatusById(int status, String userName, long userId);

    int countByEmail(String email);

    //List<UserEntity> findAll1();

    //List<UserEntity> findAll();

}
