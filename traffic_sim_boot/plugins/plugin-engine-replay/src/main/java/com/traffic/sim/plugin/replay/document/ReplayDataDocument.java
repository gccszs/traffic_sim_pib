package com.traffic.sim.plugin.replay.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * 回放数据MongoDB文档
 * 
 * @author traffic-sim
 */
@Document(collection = "replay_data")
@Data
public class ReplayDataDocument {
    
    @Id
    private String id;
    
    /**
     * 回放任务ID
     */
    private String taskId;
    
    /**
     * 仿真步数
     */
    private Long step;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 仿真数据
     */
    private Map<String, Object> simData;
    
    /**
     * 统计数据
     */
    private Map<String, Object> statistics;
}

