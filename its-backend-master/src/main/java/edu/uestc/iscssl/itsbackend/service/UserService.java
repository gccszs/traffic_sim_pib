package edu.uestc.iscssl.itsbackend.service;

import edu.uestc.iscssl.itsbackend.VO.UserVO;
import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import org.springframework.data.domain.Page;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 16:17 2019/4/15
 */
public interface UserService {

    void updateInfoById(UserEntity userEntity);
    void updatePwdById(UserEntity userEntity);
    void deleteUserById(long id,String status,String modefiedBy);
    void addUser(UserEntity userEntity);
    void addUser(String userName,String dis,String ilabId);
    long countByUserName(String username);
    Integer countByStatus(UserVO.Status status);

    UserVO selectByName(String username);
    UserEntity selectById(long userId);
    List<UserEntity> getUsers();
    Page<UserVO> getUserByPage(Integer page, UserVO.Status status, Integer limit, Integer order);
    int getTotalUserNumber();

    boolean loginCheckBan(UserEntity user, HttpServletRequest request);

    boolean loginUnseal(UserEntity user, HttpServletRequest request);

    String getCode(String email, HttpServletRequest request);

    boolean checkCode(String email, String code, HttpServletRequest request);
}
