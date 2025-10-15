package edu.uestc.iscssl.itsbackend.utils;

import edu.uestc.iscssl.itsbackend.domain.user.UserEntity;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

public class UserUtils {
    public static boolean checkManager(){
        if (getRoleId()==2)
            return true;
        return false;
    }
    public static long getRoleId(){
        return getUser().getRoleId();
    }
    public static long getUserId(){

        return getUser().getUserId();
    }
    public static String getUserName(){
        return getUser().getUserName();
    }
    public static UserEntity getUser(){
        Subject subject = SecurityUtils.getSubject();
        return  (UserEntity)subject.getPrincipals().getPrimaryPrincipal();
    }

    //
}
