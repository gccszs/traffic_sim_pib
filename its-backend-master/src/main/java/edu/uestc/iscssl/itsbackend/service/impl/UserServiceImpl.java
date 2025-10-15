package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.VO.UserVO;
import edu.uestc.iscssl.itsbackend.config.CacheConfig;
import edu.uestc.iscssl.itsbackend.domain.user.LoginRecordEntity;
import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import edu.uestc.iscssl.itsbackend.repository.LoginLogRepository;
import edu.uestc.iscssl.itsbackend.repository.LoginRepository;
import edu.uestc.iscssl.itsbackend.repository.UserRepository;
import edu.uestc.iscssl.itsbackend.repository.UserVORepository;
import edu.uestc.iscssl.itsbackend.service.UserService;
import edu.uestc.iscssl.itsbackend.utils.CommonUtil;
import edu.uestc.iscssl.itsbackend.utils.DateUtil;
import edu.uestc.iscssl.itsbackend.utils.MailUtil;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * @Author: wangwei
 * @Description:
 * @Date: 16:59 2019/4/15
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    LoginRepository loginRepository;
    @Autowired
    LoginLogRepository loginLogRepository;
    @Autowired
    UserVORepository userVORepository;
    @Autowired
    private JavaMailSenderImpl mailSender;

    CacheConfig cacheConfig = CacheConfig.getInstance();

    @Value("${spring.mail.username}")
    private String sender;

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public void updatePwdById(UserEntity userEntity) {
        String newpwd = new SimpleHash("MD5",userEntity.getPassword(),userEntity.getUserName()).toHex();
        userEntity.setPassword(newpwd);
        //目前不使用saveAndFlush，是必须传递user所有的属性，而导致create_time和update_time为空。
        //userRepository.saveAndFlush(userEntity);
        userRepository.updateInfoById(userEntity.getUserId(),userEntity.getUserName(),userEntity.getPassword(),userEntity.getEmail(),userEntity.getInstitution(),userEntity.getPhoneNumber(), userEntity.getModefiedBy());
    }
    @Override
    public void updateInfoById(UserEntity userEntity) {
        //目前不使用saveAndFlush，是必须传递user所有的属性，而导致create_time和update_time为空。
        try {
            if(!StringUtils.isEmpty(userEntity.getEmail())) {
                userEntity.setEmail(CommonUtil.encryption(userEntity.getEmail()));
            }
            if(!StringUtils.isEmpty(userEntity.getPhoneNumber())) {
                userEntity.setPhoneNumber(CommonUtil.encryption(userEntity.getPhoneNumber()));
            }
            if(!StringUtils.isEmpty(userEntity.getInstitution())) {
                userEntity.setInstitution(CommonUtil.encryption(userEntity.getInstitution()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        userRepository.saveAndFlush(userEntity);
        //userRepository.updateInfoById(userEntity.getUserId(),userEntity.getUserName(),userEntity.getPassword(),userEntity.getEmail(),userEntity.getInstitution(),userEntity.getPhoneNumber(), userEntity.getModefiedBy());
    }

    @Override
    public void deleteUserById(long id,String status,String modefiedBy) {
        if (status.equals("unNormal")){
            userRepository.deleteByStatus(id,0,modefiedBy);
        }
        //userRepository.deleteUserEntitiesByUserId(id);
        //将其状态置恢复“正常”
        else{
            userRepository.deleteByStatus(id,1,modefiedBy);
        }

    }

    /**
     * 添加用户
     * @param userEntity
     */
    @Override
    public void addUser(UserEntity userEntity) {
        //MD5加密
        String salt = userEntity.getUserName();
        String newpwd = new SimpleHash("MD5",userEntity.getPassword(),salt).toHex();
        //String newpwd = new SimpleHash("MD5",userEntity.getPassword()).toHex();
        userEntity.setPassword(newpwd);
        userEntity.setType(3);
        try {
            if(userEntity.getEmail() != null){
                userEntity.setEmail(CommonUtil.encryption(userEntity.getEmail()));
            }
            if(userEntity.getPhoneNumber() != null){
                userEntity.setPhoneNumber(CommonUtil.encryption(userEntity.getPhoneNumber()));
            }
            if(userEntity.getInstitution() != null){
                userEntity.setInstitution(CommonUtil.encryption(userEntity.getInstitution()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        userRepository.saveAndFlush(userEntity);
        //userRepository.addInfoById(userEntity.getUserName(),userEntity.getPassword(),userEntity.getRoleId(),userEntity.getEmail(),userEntity.getInstitution(),userEntity.getPhoneNumber(),userEntity.getModefiedBy(),1);
    }
    @Override
    public void addUser(String userName,String dis, String ilabId) {
        UserEntity userEntity = new UserEntity();
        userEntity.setRoleId(1);
        userEntity.setUserName(userName);
        userEntity.setStatus(UserEntity.Status.first);
        userEntity.setInstitution("仿真实验平台");
        userEntity.setDis(dis);
        userEntity.setIlabId(ilabId);
        userEntity.setType(2);//实验空间用户为2，平台用户为0
        userRepository.saveAndFlush(userEntity);
        //userRepository.addInfoById(userEntity.getUserName(),userEntity.getPassword(),userEntity.getRoleId(),userEntity.getEmail(),userEntity.getInstitution(),userEntity.getPhoneNumber(),userEntity.getModefiedBy(),1);
    }
    @Override
    public long countByUserName(String username) {
        return userRepository.countByUserName(username);
    }

    @Override
    public Integer countByStatus(UserVO.Status status) {
        return userVORepository.countByStatus(status);
    }

    @Override
    public UserVO selectByName(String username) {
        return userVORepository.findUserVOByUserName(username);
    }

    @Override
    public UserEntity selectById(long userId) {
        return userRepository.findUserEntityByUserId(userId);
    }

    @Override
    public List<UserEntity> getUsers() {
        List<UserEntity> users = userRepository.findAll();
        for(UserEntity user: users){
            user.setPassword(null);
            try {
                if(user.getPhoneNumber() != null){
                    user.setPhoneNumber(CommonUtil.decryption(user.getPhoneNumber()));
                }
                if(user.getEmail() != null){
                    user.setEmail(CommonUtil.decryption(user.getEmail()));
                }
                if(user.getInstitution() != null){
                    user.setInstitution(CommonUtil.decryption(user.getInstitution()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return users;
    }

    /**
     * 分页查询
     * @param page
     * @param status
     * @param size
     * @param order
     * @return
     */
    @Override
    public Page<UserVO> getUserByPage(Integer page, UserVO.Status status, Integer size, Integer order) {
/*        if(status == null){
            status = 1;
        }*/
        if (page == null){
            page = 0;
        }
        if (size == null) {
            size = 5;
        }
        if (order == null){
            order =1;
        }
        PageRequest pageable = null;
        if(order == 1){
            pageable = PageRequest.of(page, size, Sort.Direction.ASC, "userId");
        }
        else if(order == 0){
            pageable = PageRequest.of(page, size, Sort.Direction.DESC, "userId");
        }
        if (status.ordinal() == 1){
            Specification<UserVO> spec =  new Specification<UserVO>(){
                @Override
                public jakarta.persistence.criteria.Predicate toPredicate(Root<UserVO> root, CriteriaQuery<?> query, CriteriaBuilder cb){
                    Path<UserVO.Status> status = root.get("status");
                    Predicate p = cb.equal(status,1);//status为1的数据，即status=1
                    return (jakarta.persistence.criteria.Predicate) p;
                }
            };
            Page<UserVO> users = userVORepository.findAll(spec,pageable);

            return users;
        }
        Specification<UserVO> spec =  new Specification<UserVO>(){
            @Override
    public jakarta.persistence.criteria.Predicate toPredicate(Root<UserVO> root, CriteriaQuery<?> query, CriteriaBuilder cb){
        Path<UserVO.Status> status = root.get("status");
        Predicate p = cb.equal(status,0);//过滤s状态为0的数据，即status=0
        return (jakarta.persistence.criteria.Predicate) p;
            }
        };
        Page<UserVO> users = userVORepository.findAll(spec,pageable);

        return users;

    }

    @Override
    public int getTotalUserNumber() {
        return userRepository.countByRoleId(1l);
    }

    /**
     * 登录验证是否被ban
     * @param user
     * @param request
     * @return
     */
    @Override
    public boolean loginCheckBan(UserEntity user, HttpServletRequest request) {
        LoginRecordEntity loginEntity = loginRepository.findLoginRecordByUserId(user.getUserId());
        if(null == loginEntity){
            loginEntity = new LoginRecordEntity(CommonUtil.getIpAddr(request), user.getUserId());
            loginRepository.addLoginEntity(loginEntity.getIp(), loginEntity.getUserId(), loginEntity.getErrorCount(), loginEntity.getUpdateTime());
            return false;
        }else{
            //第一次到第五次密码错误登录
            if(loginEntity.getErrorCount() < 5){
                boolean isSameDay = DateUtil.isSameDay(loginEntity.getUpdateTime().getTime(),new Timestamp(new Date().getTime()).getTime());
                if(isSameDay){
                    loginEntity.setErrorCount(loginEntity.getErrorCount() + 1);
                }else{
                    loginEntity.setErrorCount(1L);
                }
                loginEntity.setUpdateTime(new Timestamp(new Date().getTime()));
                loginEntity.setIp(CommonUtil.getIpAddr(request));
                loginRepository.updateLoginEntityById(loginEntity.getIp(), loginEntity.getUpdateTime(), loginEntity.getErrorCount(), loginEntity.getId());
                if(loginEntity.getErrorCount() > 4){
                    userRepository.updateStatusById(3, user.getUserName(), user.getUserId());
                    loginLogRepository.addLoginLog(user.getUserId(), CommonUtil.getIpAddr(request), new Timestamp(new Date().getTime()));
                    return true;
                }
                return false;
            }else{
                //第五次以后密码错误登录
                loginEntity.setErrorCount(5L);
                loginEntity.setUpdateTime(new Timestamp(new Date().getTime()));
                loginEntity.setIp(CommonUtil.getIpAddr(request));
                loginRepository.updateLoginEntityById(loginEntity.getIp(), loginEntity.getUpdateTime(), loginEntity.getErrorCount(), loginEntity.getId());
                userRepository.updateStatusById(3, user.getUserName(), user.getUserId());
                return true;
            }
        }
    }

    /**
     * 解除封禁
     *
     * @param user
     * @param request
     * @return
     */
    @Override
    public boolean loginUnseal(UserEntity user, HttpServletRequest request) {
        LoginRecordEntity loginEntity = loginRepository.findLoginRecordByUserId(user.getUserId());
        LocalDateTime now = LocalDateTime.now();
        Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        double day = (new Timestamp(date.getTime()).getTime() - loginEntity.getUpdateTime().getTime()) / (1000 * 60 * 60 * 24.0);
        if(day > 1){
            //相隔一天以上登录解封
            loginRepository.updateLoginEntityById(CommonUtil.getIpAddr(request), new Timestamp(date.getTime()), 0L, loginEntity.getId());
            userRepository.updateStatusById(1, user.getUserName(), user.getUserId());
            return true;
        }
        //相隔一天但不足一天
        boolean isSameDay = DateUtil.isSameDay(loginEntity.getUpdateTime().getTime(), new Timestamp(date.getTime()).getTime());
        if(!isSameDay){
            double hour = (new Timestamp(date.getTime()).getTime() - loginEntity.getUpdateTime().getTime()) / (1000 * 60 * 60.0);
            // 相邻两天短时间内强制封禁2小时
            if(hour < 2){
                return false;
            }
            loginRepository.updateLoginEntityById(CommonUtil.getIpAddr(request), new Timestamp(date.getTime()), 0L, loginEntity.getId());
            userRepository.updateStatusById(1, user.getUserName(), user.getUserId());
            return true;
        }
        return false;
    }

    /**
     * 获取邮箱验证码
     *
     * @param email
     * @param request
     * @return
     */
    @Override
    public String getCode(String email, HttpServletRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("验证码");
        String code = MailUtil.getCode();
        Date sendTime = new Date();
        message.setText("尊敬的用户:\n"
                + "\n您好!"
                + "\n本次请求的邮件验证码为:" + code + ",本验证码5分钟内有效，请及时输入。（请勿泄露此验证码）\n"
                + "\n如非本人操作，请忽略该邮件。\n(这是一封自动发送的邮件，请不要直接回复）");
        message.setFrom(sender);
        message.setTo(email);
        mailSender.send(message);
        HttpSession session = request.getSession();
        session.removeAttribute(email);
        session.removeAttribute(code);
        session.setAttribute(email, code);
        session.setAttribute(code, sendTime.getTime());
        cacheConfig.save(email, code);
        cacheConfig.save(code, String.valueOf(sendTime.getTime()));
        logger.debug("email:{},code:{},time:{},sessionId:{}", email, code, sendTime, session.getId());
        return code;
    }

    /**
     * 验证验证码
     *
     * @param code
     * @param email
     * @param request
     * @return
     */
    @Override
    public boolean checkCode(String email, String code, HttpServletRequest request) {
        HttpSession session = request.getSession();
        if(null == cacheConfig.getValue(email) || null == cacheConfig.getValue(cacheConfig.getValue(email))){
            return false;
        }
        String successCode = cacheConfig.getValue(email);
        Long sendTime = Long.parseLong(cacheConfig.getValue(successCode));
        if(!successCode.equals(code)){
            return false;
        }
        if(MailUtil.getMinute(new Date(sendTime), new Date()) > 5){
            //验证码已过期  清除session的信息
            cacheConfig.remove(email);
            cacheConfig.remove(successCode);
            return false;
        }
        return true;
    }
}
