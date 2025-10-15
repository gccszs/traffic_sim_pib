package edu.uestc.iscssl.itsbackend.service;

import edu.uestc.iscssl.itsbackend.domain.user.TokenEntity;

public interface TokenService {
    public TokenEntity queryByUserId(Long userId);

    public TokenEntity queryByToken(String token);
    public TokenEntity saveToken(TokenEntity tokenEntity);

    public  TokenEntity createToken(long userId) ;
    public void logout(long userId) ;
}
