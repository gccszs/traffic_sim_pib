package com.simeng.pib.service;

import com.simeng.pib.model.SimInfo;
import org.springframework.stereotype.Service;

/**
 * @author ChonghaoGao
 * @date 2025/9/13 11:16)
 */
public interface SessionService {
    SimInfo getSessionInfo(String sessionId);

    boolean sessionExists(String sessionId);

    boolean deleteSession(String sessionId);

    void updateSessionInfo(String sessionId, SimInfo simInfo);

    int getSessionCount();
}
