package com.traffic.sim.plugin.auth.service;

import com.traffic.sim.plugin.auth.config.AuthPluginProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务
 * 
 * @author traffic-sim
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {
    
    private final AuthPluginProperties authProperties;
    
    /**
     * 存储验证码的Map，key为captchaId，value为验证码值和过期时间
     */
    private final Map<String, CaptchaInfo> captchaStore = new ConcurrentHashMap<>();
    
    /**
     * 验证码字符集
     */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    
    /**
     * 生成验证码
     */
    public CaptchaResult generateCaptcha() {
        if (!authProperties.getCaptcha().getEnabled()) {
            return null;
        }
        
        String captchaId = generateCaptchaId();
        String code = generateCode();
        
        // 存储验证码信息
        long expireTime = System.currentTimeMillis() + 
            authProperties.getCaptcha().getExpireSeconds() * 1000L;
        captchaStore.put(captchaId, new CaptchaInfo(code, expireTime));
        
        // 生成验证码图片
        byte[] imageBytes = generateImage(code);
        
        return new CaptchaResult(captchaId, imageBytes);
    }
    
    /**
     * 验证验证码
     */
    public boolean validateCaptcha(String captchaId, String captcha) {
        if (!authProperties.getCaptcha().getEnabled()) {
            return true;
        }
        
        if (captchaId == null || captcha == null) {
            return false;
        }
        
        CaptchaInfo info = captchaStore.get(captchaId);
        if (info == null) {
            return false;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() > info.getExpireTime()) {
            captchaStore.remove(captchaId);
            return false;
        }
        
        // 验证码不区分大小写
        boolean valid = info.getCode().equalsIgnoreCase(captcha);
        
        // 验证后删除
        captchaStore.remove(captchaId);
        
        return valid;
    }
    
    /**
     * 生成验证码ID
     */
    private String generateCaptchaId() {
        return "captcha_" + System.currentTimeMillis() + "_" + 
            new Random().nextInt(10000);
    }
    
    /**
     * 生成验证码字符串
     */
    private String generateCode() {
        Random random = new Random();
        int length = authProperties.getCaptcha().getLength();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }
    
    /**
     * 生成验证码图片
     */
    private byte[] generateImage(String code) {
        int width = authProperties.getCaptcha().getWidth();
        int height = authProperties.getCaptcha().getHeight();
        
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 填充背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        // 绘制边框
        g.setColor(Color.GRAY);
        g.drawRect(0, 0, width - 1, height - 1);
        
        // 绘制验证码
        Random random = new Random();
        g.setFont(new Font("Arial", Font.BOLD, height - 10));
        int x = 10;
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
            g.drawString(String.valueOf(code.charAt(i)), x, height - 5);
            x += width / (code.length() + 1);
        }
        
        // 绘制干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.drawLine(random.nextInt(width), random.nextInt(height),
                      random.nextInt(width), random.nextInt(height));
        }
        
        g.dispose();
        
        // 转换为字节数组
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("生成验证码图片失败", e);
            return new byte[0];
        }
    }
    
    /**
     * 清理过期的验证码
     */
    public void cleanExpiredCaptcha() {
        long now = System.currentTimeMillis();
        captchaStore.entrySet().removeIf(entry -> entry.getValue().getExpireTime() < now);
    }
    
    /**
     * 验证码信息
     */
    private static class CaptchaInfo {
        private final String code;
        private final long expireTime;
        
        public CaptchaInfo(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
        
        public String getCode() {
            return code;
        }
        
        public long getExpireTime() {
            return expireTime;
        }
    }
    
    /**
     * 验证码结果
     */
    public static class CaptchaResult {
        private final String captchaId;
        private final byte[] imageBytes;
        
        public CaptchaResult(String captchaId, byte[] imageBytes) {
            this.captchaId = captchaId;
            this.imageBytes = imageBytes;
        }
        
        public String getCaptchaId() {
            return captchaId;
        }
        
        public byte[] getImageBytes() {
            return imageBytes;
        }
    }
}

