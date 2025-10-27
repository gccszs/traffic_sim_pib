package com.simeng.pib.service;


import java.util.List;
import java.util.Map;

/**
 * @author ChonghaoGao
 * @date 2025/9/13 11:13)
 */
public interface SimulationService {
    void initSimengByFeign(Map<String, Object> simengInfo, List<Map<String, Object>> controlViews, String id);



}
