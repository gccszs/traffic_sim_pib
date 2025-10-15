package edu.uestc.iscssl.itsbackend.service.impl;

import edu.uestc.iscssl.itsbackend.domain.user.TokenEntity;
import edu.uestc.iscssl.itsbackend.repository.TokenRepository;
import edu.uestc.iscssl.itsbackend.service.TokenService;
import edu.uestc.iscssl.itsbackend.utils.TokenGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
@Service
public class TokenServiceImpl implements TokenService {
    @Autowired
    TokenRepository tokenRepository;

    //12小时后过期
    private final static int EXPIRE = 3600 * 12;

    public TokenEntity queryByUserId(Long userId){
        return tokenRepository.queryByUserId(userId);
    }

    public TokenEntity queryByToken(String token){
        return tokenRepository.queryByToken(token);
    }
    public TokenEntity saveToken(TokenEntity tokenEntity){
        return tokenRepository.save(tokenEntity);
    }

    public TokenEntity createToken(long userId) {
        String token = TokenGenerator.generateValue();
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + EXPIRE * 1000);
        TokenEntity tokenEntity = queryByUserId(userId);
        if(tokenEntity == null){
            tokenEntity = new TokenEntity();
            tokenEntity.setUserId(userId);
            tokenEntity.setToken(token);
            tokenEntity.setUpdateTime(now);
            tokenEntity.setExpireTime(expireTime);
            saveToken(tokenEntity);
        }else{
            tokenEntity.setToken(token);
            tokenEntity.setUpdateTime(now);
            tokenEntity.setExpireTime(expireTime);
            saveToken(tokenEntity);
        }
        return tokenEntity;


    }

    public void logout(long userId) {
        TokenEntity tokenEntity = queryByUserId(userId);
        tokenEntity.setToken(null);
        tokenEntity.setExpireTime(null);
        tokenEntity.setUpdateTime(null);
        saveToken(tokenEntity);
    }
}
