package edu.uestc.iscssl.itsbackend.config;

import edu.uestc.iscssl.itsbackend.OAuth2.OAuth2Filter;
import edu.uestc.iscssl.itsbackend.OAuth2.OAuth2Realm;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 移除冲突的导入
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


@Configuration
public class ShiroConfig {


    @Bean("sessionManager")
    public SessionManager sessionManager(){
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setSessionValidationSchedulerEnabled(true);
        sessionManager.setSessionIdCookieEnabled(true);
        return sessionManager;
    }
    @Bean("securityManager")
    public SecurityManager securityManager(OAuth2Realm oAuth2Realm, SessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(oAuth2Realm);
        securityManager.setSessionManager(sessionManager);
        return securityManager;
    }

    @Bean("shiroFilter")
    public ShiroFilterFactoryBean shirFilter(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();// 定义过滤器映射
        shiroFilter.setSecurityManager(securityManager);
        
        // 直接设置自定义过滤器
        shiroFilter.getFilters().put("oauth2", new OAuth2Filter());
        Map<String, String> filterMap = new LinkedHashMap<>();
        filterMap.put("/viewPdf/**","anon");
        filterMap.put("/toAuth","anon");
        filterMap.put("/toLogin","anon");
        filterMap.put("/receiveLogin","anon");
        filterMap.put("/sentFile","anon");
        filterMap.put("/receiveFile","anon");
        filterMap.put("/sentData","anon");
        filterMap.put("/receiveData","anon");
        filterMap.put("/login", "anon");
        filterMap.put("/getCode", "anon");
        filterMap.put("/getVerify","anon");
        filterMap.put("/getStatistic","anon");
        filterMap.put("/swagger-ui.html","anon");
        filterMap.put("/webjars/**","anon");
        filterMap.put("/v2/**","anon");
        filterMap.put("/swagger*/**","anon");
        filterMap.put("/register","anon");
        filterMap.put("/**", "oauth2");

        shiroFilter.setFilterChainDefinitionMap(filterMap);

        return shiroFilter;
    }

    @Bean("lifecycleBeanPostProcessor")
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator proxyCreator = new DefaultAdvisorAutoProxyCreator();
        proxyCreator.setProxyTargetClass(true);
        return proxyCreator;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

}
