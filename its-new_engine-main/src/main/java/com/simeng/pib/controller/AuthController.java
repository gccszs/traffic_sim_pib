package com.simeng.pib.controller;

import com.simeng.pib.model.dto.ApiResponse;
import com.simeng.pib.service.impl.SessionServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SessionServiceImpl sessionServiceImpl;

    /**
     * 创建Cookie ID用于身份识别
     */
    @GetMapping("/cookie_id")
    public ResponseEntity<ApiResponse<String>> createCookie(HttpServletResponse response) {
        String sessionId = sessionServiceImpl.createSessionId();

        // 设置Cookie - 修复跨域问题
        ResponseCookie cookie = ResponseCookie.from("id", sessionId)
                .httpOnly(true)          // 防止XSS攻击
                .secure(true)            // 仅HTTPS传输（生产环境）
                .sameSite("None")        // 允许跨站请求
                .path("/")               // 全站可用，不只是/cookie_id
                .maxAge(24 * 60 * 60)    // 24小时
                .build();

        // 添加Cookie到响应头
        response.setHeader("Set-Cookie", cookie.toString());

        log.info("Created new session ID: {}", sessionId);


        return ResponseEntity.ok(ApiResponse.success("create_cookie_id", sessionId));
    }

    /**
     * 删除会话信息
     */
    @GetMapping("/del_id_info")
    public ResponseEntity<ApiResponse<String>> deleteIdInfo(@CookieValue(value = "id", required = false) String id) {
        if (id == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_AUTH", "unable to get cookie"));
        }

        if (!sessionServiceImpl.sessionExists(id)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ERR_INVALID", "not find id in id_infos"));
        }

        boolean deleted = sessionServiceImpl.deleteSession(id);
        if (deleted) {
            log.debug("Deleted session: {}", id);
            return ResponseEntity.ok(ApiResponse.success("delete id info", id));
        } else {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ERR_DELETE", "failed to delete session"));
        }
    }
}
