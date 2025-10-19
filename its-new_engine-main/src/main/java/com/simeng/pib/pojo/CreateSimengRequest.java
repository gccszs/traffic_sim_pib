package com.simeng.pib.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 创建仿真引擎请求体
 *
 * @author ChonghaoGao
 * @date 2025/10/19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSimengRequest {
    /**
     * 仿真信息
     */
    private Map<String, Object> simInfo;
    
    /**
     * 控制视图列表
     */
    private List<Map<String, Object>> controlViews;
    
    /**
     * 用户ID
     */
    private String userId;
}

