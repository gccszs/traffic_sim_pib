package com.simeng.pib.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * WebSocket消息格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketInfo {

    /**
     * 消息类型：frontend, eng, backend
     */
    private String type;

    /**
     * 操作类型：hello, hi, start, pause, err等
     */
    private String ope;

    /**
     * 时间戳
     */
    private Long time;

    /**
     * 消息数据
     */
    private Map<String, Object> data;

    public WebSocketInfo(String type, String ope, Long time) {
        this.type = type;
        this.ope = ope;
        this.time = time;
    }
}
