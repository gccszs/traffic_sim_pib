package edu.uestc.iscssl.itsbackend.controller;

import com.alibaba.fastjson.JSONObject;
import edu.uestc.iscssl.itsbackend.domain.user.TokenEntity;
import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import edu.uestc.iscssl.itsbackend.repository.UserRepository;
import edu.uestc.iscssl.itsbackend.service.TokenService;
import edu.uestc.iscssl.itsbackend.service.UserService;
import edu.uestc.iscssl.itsbackend.utils.JWT;
import edu.uestc.iscssl.itsbackend.utils.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@Api(value = "与实验空间进行对接的相关API",description = "与实验空间对接的相关API")
public class SpaceController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserService userService;
    @Autowired
    TokenService tokenService;

    @ApiOperation(value = "",notes = "令牌验证接口，识别实验空间的用户，已登录用户验证其签名，token失效则提示重新登录")
    @RequestMapping(value = "/toAuth",method = RequestMethod.GET)
    public R testAuth(@RequestParam String token) throws UnsupportedEncodingException {
        System.out.println(token);
        token = java.net.URLDecoder.decode(token,"utf-8");
        System.out.println(token);
        if(token == null){
            throw new IncorrectCredentialsException("token失效，请重新登录");
        }
        JWT jwt = new JWT();
        token = token.replaceAll(" ","+");
        String json = jwt.verifyAndDecrypt(token, System.currentTimeMillis());
        JSONObject jsonObject = JSONObject.parseObject(json);
        System.out.println("------------------");
        System.out.println(jsonObject.getString("un")+jsonObject.getString("dis")+jsonObject.getString("id"));
        String dis = jsonObject.getString("dis");
        System.out.println(jsonObject.getString("dis"));
        if((jsonObject.get("id")==null)||(jsonObject.get("un")== null)||(jsonObject.get("dis")== null)){
            throw new IncorrectCredentialsException("用户信息有误");
        }
        UserEntity userEntity = userRepository.findUserEntityByUserName(jsonObject.getString("un"));
        if(userEntity==null){//平台还未存储的空间用户
            userService.addUser(jsonObject.getString("un"),jsonObject.getString("dis"),jsonObject.getString("id"));
            UserEntity userEntity2 = userRepository.findUserEntityByUserName(jsonObject.getString("un"));//插入用户后，再次取出userEntity2，以获得生成的userId
            TokenEntity tokenEntity=tokenService.createToken(userEntity2.getUserId());
            Map<String,Object> result = new HashMap<>();
            result.put("status",userEntity2.getStatus());
            if(userEntity2.getStatus() == UserEntity.Status.first){
                userEntity2.setStatus(UserEntity.Status.normal);
                userService.updateInfoById(userEntity2);
            }
            result.put("msg","登录成功");
            result.put("userId",userEntity2.getUserId());
            result.put("roleId",userEntity2.getRoleId());
            result.put("token",tokenEntity.getToken());
            return R.ok(result);
        }
        TokenEntity tokenEntity=tokenService.createToken(userEntity.getUserId());
        Map<String,Object> result = new HashMap<>();
        result.put("status",userEntity.getStatus());
        result.put("msg","登录成功");
        result.put("userId",userEntity.getUserId());
        result.put("roleId",userEntity.getRoleId());
        result.put("token",tokenEntity.getToken());
        return R.ok(result);
    }
}
