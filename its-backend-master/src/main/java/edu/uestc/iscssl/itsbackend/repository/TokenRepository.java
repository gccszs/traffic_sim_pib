package edu.uestc.iscssl.itsbackend.repository;

import edu.uestc.iscssl.itsbackend.domain.user.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<TokenEntity,Long> {
    TokenEntity queryByUserId(Long userId);
    TokenEntity queryByToken(String token);
    TokenEntity save(TokenEntity tokenEntity);

}
