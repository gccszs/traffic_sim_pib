package edu.uestc.iscssl.itsbackend.OAuth2;

import edu.uestc.iscssl.itsbackend.domain.user.PermissionEntity;
import edu.uestc.iscssl.itsbackend.domain.user.RoleEntity;
import edu.uestc.iscssl.itsbackend.domain.user.TokenEntity;
import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import edu.uestc.iscssl.itsbackend.repository.PermissionRepository;
import edu.uestc.iscssl.itsbackend.repository.RoleRepository;
import edu.uestc.iscssl.itsbackend.repository.UserRepository;
import edu.uestc.iscssl.itsbackend.service.ShiroService;
import edu.uestc.iscssl.itsbackend.service.TokenService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class OAuth2Realm extends AuthorizingRealm {
    @Autowired
    private ShiroService shiroService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PermissionRepository permissionRepository;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token;
    }


    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        UserEntity user = (UserEntity) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        RoleEntity roleEntity = roleRepository.findRoleEntityByRoleId(user.getRoleId());
        String role = roleEntity.getRoleName();
        info.addRole(role);

        List<PermissionEntity> list = permissionRepository.findPermissionEntitiesByRoleEntities(roleEntity);
        Set<String> permission = list.stream().map(permissionEntity->{return permissionEntity.getPermissionName();}).collect(Collectors.toSet());
        info.addStringPermissions(permission);
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String accessToken = (String) token.getPrincipal();
        TokenEntity tokenEntity = tokenService.queryByToken(accessToken);
        if(tokenEntity == null || tokenEntity.getExpireTime().getTime() < System.currentTimeMillis()){
            throw new IncorrectCredentialsException("token失效，请重新登录");
        }
        UserEntity user = userRepository.findUserEntityByUserId(tokenEntity.getUserId());

        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, accessToken, getName());
        return info;
    }
}
