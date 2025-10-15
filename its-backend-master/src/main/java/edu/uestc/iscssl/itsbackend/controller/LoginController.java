package edu.uestc.iscssl.itsbackend.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import edu.uestc.iscssl.itsbackend.VO.UserVO;
import edu.uestc.iscssl.itsbackend.controller.agent.annotation.ApiJsonObject;
import edu.uestc.iscssl.itsbackend.controller.agent.annotation.ApiJsonProperty;
import edu.uestc.iscssl.itsbackend.domain.user.TokenEntity;
import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import edu.uestc.iscssl.itsbackend.repository.LoginLogRepository;
import edu.uestc.iscssl.itsbackend.repository.LoginRepository;
import edu.uestc.iscssl.itsbackend.repository.UserRepository;
import edu.uestc.iscssl.itsbackend.service.TokenService;
import edu.uestc.iscssl.itsbackend.service.UserService;
import edu.uestc.iscssl.itsbackend.utils.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static edu.uestc.iscssl.itsbackend.utils.UserUtils.*;

@CrossOrigin
@RestController
@Api(value = "LoginController的相关API",description = "登录、注册、用户管理")
public class LoginController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    LoginRepository loginRepository;
    @Autowired
    LoginLogRepository loginLogRepository;
    @Autowired
    TokenService tokenService;
    @Autowired
    UserService userService;

    /**
     * 生成验证码
     */
    @RequestMapping(value = "/getVerify", method = RequestMethod.GET)
    public void getVerify(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Pragma", "No-cache");//设置响应头信息，告诉浏览器不要缓存此内容
        response.setHeader("Cache-Control", "No-cache");
        response.setDateHeader("Expires", 0);
//        response.setHeader("Access-Control-Allow-Origin", "https://192.168.1.47:9528");
        response.setHeader("Access-Control-Allow-Credentials", "true");

        response.setContentType("image/jpeg");//设置相应类型,告诉浏览器输出的内容为图片
        GenerateVerifyCode randomValidateCode = new GenerateVerifyCode();
        randomValidateCode.getRandcode(request, response);//输出验证码图片方法

    }

    @ApiOperation(value = "登录",notes = "用户登录")
    @RequestMapping(path = "/login",method = RequestMethod.POST)
    public R login(@ApiJsonObject(name = "login_model", value = {
            @ApiJsonProperty(key = "userName",example = "张三",description = "用户名"),
            @ApiJsonProperty(key = "password",example = "123",description = "密码"),
            @ApiJsonProperty(key="verifyCode",example = "1234",description = "验证码")
    })HttpServletRequest request, @RequestBody Map<String,String> map) throws NoSuchAlgorithmException {
        UserEntity user = userRepository.findUserEntityByUserName(map.get("userName"));
        String verifyCode = String.valueOf(map.get("verifyCode"));
        HttpSession session = request.getSession();
        System.out.println("----本次会话session---"+session.getId());
        String random = String.valueOf(session.getAttribute(GenerateVerifyCode.RANDOMCODEKEY));
        if (!random.equals(verifyCode)) {
            return R.error(HttpStatus.UNAUTHORIZED,"验证码出错");
        }
        if (user!=null&&(user.getType()==0 || user.getType() == 3)){ //本地用户且存在
            String newpwd;
            if(user.getType()==0){
                newpwd = new SimpleHash("MD5",map.get("password")).toHex();
            }else{
                newpwd = new SimpleHash("MD5",map.get("password"),map.get("userName")).toHex();
            }
            if (user.getStatus()== UserEntity.Status.ban){
                //解除封禁
                boolean isUnseal = userService.loginUnseal(user, request);
                if(!isUnseal) {
                    if(!user.getPassword().equals(newpwd)){
                        loginLogRepository.addLoginLog(user.getUserId(), CommonUtil.getIpAddr(request), new Timestamp(new Date().getTime()));
                    }
                    return R.error(HttpStatus.NOT_ACCEPTABLE, "您已被临时封禁");
                }
            }
            if(!user.getPassword().equals(newpwd)) {
                boolean isBan = userService.loginCheckBan(user, request);
                if(isBan) return R.error(HttpStatus.NOT_ACCEPTABLE,"您已被临时封禁");
                //密码错误登录入库
                loginLogRepository.addLoginLog(user.getUserId(), CommonUtil.getIpAddr(request), new Timestamp(new Date().getTime()));
                return R.error(HttpStatus.UNAUTHORIZED,"账号或密码问题");
            }
            //判断是否被拉黑
            if (user.getStatus()== UserEntity.Status.unNormal){
                return R.error(HttpStatus.NOT_ACCEPTABLE,"您已被拉黑");
            }
            TokenEntity tokenEntity=tokenService.createToken(user.getUserId());
            Map<String,Object> result = new HashMap<>();
            result.put("status",user.getStatus());
            if(user.getStatus() == UserEntity.Status.first){
                user.setStatus(UserEntity.Status.normal);
                userService.updateInfoById(user);
            }
            result.put("msg","交通平台登录成功");
            result.put("userId",user.getUserId());
            result.put("roleId",user.getRoleId());
            result.put("token",tokenEntity.getToken());
            return R.ok(result);
        }
        JSONObject jsonObject = JwtSentData.sentLoginInfo(map.get("userName"),map.get("password"));//用户不存在，或用户存在但type为2
        if((Integer)jsonObject.get("code")!=0){ //验证不通过
            return R.error(HttpStatus.UNAUTHORIZED,"账号或密码问题");
        }
        UserEntity userEntity = userRepository.findUserEntityByUserName(jsonObject.getString("username"));//新用户添加以后，需要再次查询，获取生成的userId，以生成token
        if(userEntity==null){//用户不存在时，添加新用户到用户表
            userService.addUser(jsonObject.getString("username"),jsonObject.getString("name"),null);
        }
        TokenEntity tokenEntity=tokenService.createToken(userEntity.getUserId());
        Map<String,Object> result = new HashMap<>();
        result.put("status",userEntity.getStatus());
        if(userEntity.getStatus() == UserEntity.Status.first){
            userEntity.setStatus(UserEntity.Status.normal);
            userService.updateInfoById(userEntity);
        }
        result.put("msg","实验空间登录成功");
        result.put("userId",userEntity.getUserId());
        result.put("roleId",userEntity.getRoleId());
        result.put("token",tokenEntity.getToken());
        return R.ok(result);
    }

    /**
     * 当前登录用户查看个人信息
     * @return
     */
    @ApiOperation(value = "用户查看个人信息",notes = "无参数,但要将保存的token保存到Headers中进行验证")
    @GetMapping("/getUser")
    @RequiresPermissions(logical = Logical.AND, value = {"user:select"})
    public R selectUser() {
        UserEntity user = getUser();
        user.setPassword(null);
        if(user.getType()==2){
            user.setUserName(user.getDis());//dis中存储着实验空间用户的真实名称，而不是唯一的ID
        }
        try {
            if(!StringUtils.isEmpty(user.getPhoneNumber())){
                user.setPhoneNumber(CommonUtil.decryption(user.getPhoneNumber()));
            }
            if(!StringUtils.isEmpty(user.getEmail())) {
                user.setEmail(CommonUtil.decryption(user.getEmail()));
            }
            if(!StringUtils.isEmpty(user.getInstitution())) {
                user.setInstitution(CommonUtil.decryption(user.getInstitution()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Map<String,Object> result = new HashMap<>();
        result.put("msg","You are getting your personal information");
        result.put("UserInfo",user);
        return R.ok(result);
    }

    /**
     * 管理员查找用户信息
     * @return
     */
    @ApiOperation(value = "查找用户",notes = "需要管理员权限")
    @GetMapping("/getUser1")
    @RequiresPermissions(logical = Logical.AND, value = {"user:select1"})
    public R selectUserByName(@RequestParam String userName) {
        UserVO user = null;
        try {
            user = userService.selectByName(userName);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpStatus.FORBIDDEN,e.getMessage());
        }
        if (user == null){
            return R.error(HttpStatus.NOT_FOUND,"该用户不存在");
        }
        Map<String,Object> result = new HashMap<>();
        result.put("msg","You are getting "+user.getUserName()+"'s information");
        result.put("UserInfo",user);
        return R.ok(result);
    }

    /**
     * 管理员查看所有用户信息
     * @return
     */
    @ApiOperation(value = "查看所有用户",notes = "无参数，需要管理员权限，携带token")
    @GetMapping("/getUsers")
    @RequiresPermissions(logical = Logical.AND, value = {"user:select1"})
    public R getUsers() {
        List<UserEntity> users = null;
        try {
            users = userService.getUsers();
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpStatus.FORBIDDEN,e.getMessage());
        }
        if (users == null){
            return R.error(HttpStatus.NOT_FOUND,"该用户不存在");
        }
        Map<String,Object> result = new HashMap<>();
        result.put("msg","You are getting all users' information");
        result.put("UserInfoList",users);
        return R.ok(result);
    }
    /**
     * 用户列表（分页）
     * @return
     */
    @ApiOperation(value = "分页，有条件地查看用户",notes = "需要管理员权限")
    @ResponseBody
    @GetMapping("/getUsersPage")
    @RequiresPermissions(logical = Logical.AND, value = {"user:select1"})
    public R find(@RequestParam (required = false) String userName
            , @RequestParam(required = false,defaultValue = "normal") UserVO.Status status
            , @RequestParam(required = false,defaultValue = "1") Integer page
            , @RequestParam(required = false,defaultValue = "10") int limit
            , @RequestParam(required = false,defaultValue = "1") int order ) {
        List<UserVO> users = new ArrayList<UserVO>();
        if (userName != null && !userName.equals("")){
            //List<UserEntity> users1 =;
            UserVO user = null;
            user = userService.selectByName(userName);
            if (user == null){
                Map<String,Object> result = new HashMap<>();
                result.put("msg","There is no information");
                result.put("UserInfoList",users);
                return R.ok(result);
            }else{
                try {
                    if(!StringUtils.isEmpty(user.getPhoneNumber())){
                        user.setPhoneNumber(CommonUtil.decryption(user.getPhoneNumber()));
                    }
                    if(!StringUtils.isEmpty(user.getEmail())) {
                        user.setEmail(CommonUtil.decryption(user.getEmail()));
                    }
                    if(!StringUtils.isEmpty(user.getInstitution())) {
                        user.setInstitution(CommonUtil.decryption(user.getInstitution()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            users.add(0,user);

            Map<String,Object> result = new HashMap<>();
            result.put("msg","You are getting users' information");
            result.put("UserInfoList",users);
            return R.ok(result);
        }
        //Page<UserEntity> users = null;
        Integer count = (Integer) userService.countByStatus(status);

        try {
            users = userService.getUserByPage(page-1,status,limit,order).getContent();
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpStatus.FORBIDDEN,e.getMessage());
        }
        for(UserVO userVO : users){
            try {
                if(!StringUtils.isEmpty(userVO.getPhoneNumber())){
                    userVO.setPhoneNumber(CommonUtil.decryption(userVO.getPhoneNumber()));
                }
                if(!StringUtils.isEmpty(userVO.getEmail())) {
                    userVO.setEmail(CommonUtil.decryption(userVO.getEmail()));
                }
                if(!StringUtils.isEmpty(userVO.getInstitution())) {
                    userVO.setInstitution(CommonUtil.decryption(userVO.getInstitution()));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Map<String,Object> result = new HashMap<>();
        result.put("msg","You are getting users' information");
        result.put("UserInfoList",users);
        result.put("count",count);
        return R.ok(result);
    }

    @ApiOperation(value = "（用户、管理员）修改密码",notes = "考虑旧密码错误")
    @PostMapping("/updatePassword")
    @RequiresPermissions(logical = Logical.AND, value = {"user:update"})
    public R updatePassword(@ApiJsonObject(name = "updatePassword", value = {
            @ApiJsonProperty(key = "oldPassword",example = "123",description = "旧密码"),
            @ApiJsonProperty(key = "newPassword",example = "123456",description = "角新密码"),
    }) @RequestBody Map<String,String> map) {
        String oldpwd;
        if(getUser().getType() == 3){
            oldpwd = new SimpleHash("MD5",map.get("oldPassword"),getUserName()).toHex();
        }else{
            oldpwd = new SimpleHash("MD5",map.get("oldPassword")).toHex();
        }
        if(!oldpwd.equals(getUser().getPassword())){
            return R.error(HttpStatus.NOT_FOUND,"旧密码错误");
        }
        UserEntity userEntity = getUser();
        if(!PasswordCheckUtil.evalPassword((map.get("newPassword"))) || map.get("newPassword").contains(userEntity.getUserName())
                || map.get("newPassword").contains(userEntity.getEmail().substring(0, userEntity.getEmail().indexOf("@")))
                || map.get("newPassword").contains(userEntity.getPhoneNumber())){
            return R.error(HttpStatus.NOT_ACCEPTABLE,"密码不符合要求");
        }
        Map<String,Object> result = new HashMap<>();
        userEntity.setPassword(map.get("newPassword"));
        userEntity.setModefiedBy(getUserName());
        try {
            userService.updatePwdById(userEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR,"Change failed");
        }
        result.put("msg","Change Success");
        return R.ok(result);
    }

    /**
     * 当前用户修改个人信息||管理员修改用户信息
     * @return
     */
    @ApiOperation(value = "（用户、管理员）更新自己的信息",notes = "考虑新的用户名已经被占用")
    @PostMapping("/updateUser")
    @RequiresPermissions(logical = Logical.AND, value = {"user:update"})
    public R updateUser(@ApiJsonObject(name = "updateUser", value = {
            @ApiJsonProperty(key = "userName",example = "张三",description = "用户名"),
            @ApiJsonProperty(key = "email",example = "123@163.com",description = "邮箱"),
            @ApiJsonProperty(key = "institution",example = "UESTC",description = "所属机构"),
            @ApiJsonProperty(key = "phoneNumber",example = "110",description = "电话")
    }) @RequestBody Map<String,String> map) {
        if(!getUserName().equals(map.get("userName"))){
            if(userService.countByUserName(map.get("userName"))>0){
                return R.error(HttpStatus.NOT_IMPLEMENTED,"用户名已存在");
            }
        }
        try {
            if(userRepository.countByEmail(CommonUtil.encryption(map.get("email"))) > 0){
                return R.error(HttpStatus.NOT_ACCEPTABLE,"邮箱被占用");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return  R.error(HttpStatus.INTERNAL_SERVER_ERROR,"邮箱被占用");
        }
        UserEntity userEntity = getUser();
        Map<String,Object> result = new HashMap<>();
        userEntity.setUserName(map.get("userName"));
        userEntity.setEmail(map.get("email"));
        userEntity.setInstitution(map.get("institution"));
        userEntity.setPhoneNumber(map.get("phoneNumber"));
        userEntity.setModefiedBy(map.get("userName"));

        try {
            userService.updateInfoById(userEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR,"Change failed");
        }
        result.put("msg","Change Success");
        return R.ok(result);
    }

    /**
     * 管理员添加用户信息
     * @param map
     * @return
     */
    @ApiOperation(value = "添加用户",notes = "考虑新的用户名已经被占用")
    @PostMapping("/addUser")
    @RequiresPermissions(logical = Logical.AND, value = {"user:add"})
    public R addUser(@ApiJsonObject(name = "addUser", value = {
            @ApiJsonProperty(key = "userName",example = "test8",description = "用户名"),
            @ApiJsonProperty(key = "password",example = "123",description = "密码"),
            @ApiJsonProperty(key = "roleId",example = "2",description = "角色"),
            @ApiJsonProperty(key = "email",example = "123@163.com",description = "邮箱"),
            @ApiJsonProperty(key = "institution",example = "UESTC",description = "所属机构"),
            @ApiJsonProperty(key = "phoneNumber",example = "110",description = "电话")
    })@RequestBody Map<String,String> map) {
        Map<String,Object> result = new HashMap<>();
        if(userService.countByUserName(map.get("userName"))>0){
            return R.error(HttpStatus.NOT_ACCEPTABLE,"用户名已存在");
        }
        try {
            if(userRepository.countByEmail(CommonUtil.encryption(map.get("email"))) > 0){
                return R.error(HttpStatus.NOT_ACCEPTABLE,"邮箱被占用");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return  R.error(HttpStatus.INTERNAL_SERVER_ERROR,"邮箱被占用");
        }
        if(!PasswordCheckUtil.evalPassword((map.get("password")))
                || map.get("password").contains(map.get("userName"))
                || map.get("password").contains(map.get("email").substring(0, map.get("email").indexOf("@")))
                || map.get("password").contains(map.get("phoneNumber"))){
            return R.error(HttpStatus.NOT_ACCEPTABLE,"密码不符合要求");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(map.get("userName"));
        userEntity.setRoleId(Long.valueOf(map.get("roleId")));
        userEntity.setPassword(map.get("password"));
        userEntity.setEmail(map.get("email"));
        userEntity.setInstitution(map.get("institution"));
        userEntity.setPhoneNumber(map.get("phoneNumber"));
        userEntity.setModefiedBy(getUserName());

        try {
            userService.addUser(userEntity);
        } catch (Exception e) {
            e.printStackTrace();
            //result.put("msg",e.getMessage());
            return R.error(e.getMessage());
        }
        result.put("msg","Add Success");
        return R.ok(result);
    }

    /**
     * 删除用户
     * @param
     * @return
     */
    @ApiOperation(value = "管理员拉黑和撤销拉黑用户，拉黑status设置为normal，撤销拉黑status设置为unNormal")
    @DeleteMapping("/deleteUser")
    @RequiresPermissions(logical = Logical.AND, value = {"user:delete"})
    public R deleteUser(@RequestParam(value = "userId",required = true) Long userId,
                        @RequestParam(value = "status",required = true) String status) {
        try {
            userService.deleteUserById(userId,status,getUserName());
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(e.getMessage());
        }
        Map<String,Object> result = new HashMap<>();
        result.put("msg","Change Success");
        return R.ok(result);
    }

    /**
     * 用户注册
     * @param map(userEntity)
     * @return
     */
    @ApiOperation(value = "用户注册",notes = "考虑新的用户名已经被占用")
    @PostMapping("/register")
    public R register(@ApiJsonObject(name = "register", value = {
            @ApiJsonProperty(key = "userName",example = "test8",description = "用户名"),
            @ApiJsonProperty(key = "password",example = "123",description = "密码"),
            @ApiJsonProperty(key = "roleId",example = "1",description = "角色"),
            @ApiJsonProperty(key = "email",example = "123@163.com",description = "邮箱"),
            @ApiJsonProperty(key = "institution",example = "UESTC",description = "所属机构"),
            @ApiJsonProperty(key = "phoneNumber",example = "110",description = "电话"),
            @ApiJsonProperty(key = "code",example = "123456",description = "验证码")
    })@RequestBody Map<String,String> map, HttpServletRequest request) {
        Map<String,Object> result = new HashMap<>();
        if(!userService.checkCode(map.get("email"), map.get("code"), request)){
            return R.error(HttpStatus.NOT_ACCEPTABLE,"验证码不正确");
        }
        try {
            if(userRepository.countByEmail(CommonUtil.encryption(map.get("email"))) > 0){
                return R.error(HttpStatus.NOT_ACCEPTABLE,"邮箱被占用");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return  R.error(HttpStatus.INTERNAL_SERVER_ERROR,"邮箱被占用");
        }
        if(userService.countByUserName(map.get("userName"))>0){
            return R.error(HttpStatus.NOT_ACCEPTABLE,"用户名已存在");
        }
        if(Long.valueOf(map.get("roleId"))!=1) {
            return R.error(HttpStatus.NOT_ACCEPTABLE,"注册失败");
        }
        if(!PasswordCheckUtil.evalPassword((map.get("password")))
                || map.get("password").contains(map.get("userName"))
                || map.get("password").contains(map.get("email").substring(0, map.get("email").indexOf("@")))
                || map.get("password").contains(map.get("phoneNumber"))){
            return R.error(HttpStatus.NOT_ACCEPTABLE,"密码不符合要求");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(map.get("userName"));
        userEntity.setRoleId(Long.valueOf(map.get("roleId")));
        userEntity.setPassword(map.get("password"));
        userEntity.setEmail(map.get("email"));
        userEntity.setInstitution(map.get("institution"));
        userEntity.setPhoneNumber(map.get("phoneNumber"));
        userEntity.setModefiedBy(map.get("userName"));
        userEntity.setStatus(UserEntity.Status.first);

        try {
            userService.addUser(userEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(e.getMessage());
        }
        result.put("msg","register Success");
        return R.ok(result);
    }


    @ApiOperation(value = "邮箱获取验证码",notes = "考虑唯一邮箱验证")
    @RequestMapping(value = "/getCode",method =RequestMethod.POST)
    public R getCode(@ApiJsonObject(name = "map", value = {
            @ApiJsonProperty(key = "email",example = "123@163.com",description = "邮箱")
    })@RequestBody Map<String,String> map, HttpServletRequest request){
        if(StringUtils.isEmpty(map.get("email"))){
            return  R.error(HttpStatus.NOT_ACCEPTABLE,"参数不正确");
        }
        int count = 0;
        try {
            count = userRepository.countByEmail(CommonUtil.encryption(map.get("email")));
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpStatus.INTERNAL_SERVER_ERROR,"验证码获取失败");
        }
        if(count > 0){
            return R.error(HttpStatus.NOT_ACCEPTABLE,"邮箱被占用");
        }
        userService.getCode(map.get("email"), request);
        return R.ok("发送成功");
    }

    /**
     * 用户登出
     * @return
     */
    @ApiOperation(value = "用户登出",notes = "无参数,但要将保存的token保存到Headers中进行验证")
    @RequestMapping(value = "/logout",method =RequestMethod.POST)
    public R logout(){
        tokenService.logout(getUserId());
        return  R.ok("登出成功");
    }

    /**
     * 重置所有用户密码
     * @return
     */
//    @ApiOperation(value = "resetPwd",notes = "重置所有用户密码，管理权限")
//    @RequestMapping(value = "/resetPwd",method =RequestMethod.GET)
    public R resetPwd(){
        List<UserEntity> userEntityList = userService.getUsers();
        String pwd = "Its123456789";
        for(UserEntity user : userEntityList){
            String newpwd;
            if(user.getType()==0){
                newpwd = new SimpleHash("MD5",pwd).toHex();
            }else{
                newpwd = new SimpleHash("MD5",pwd,user.getUserName()).toHex();
            }
            user.setPassword(newpwd);
            userService.updateInfoById(user);
        }
        return  R.ok("重置为" + pwd + "成功");
    }
}
