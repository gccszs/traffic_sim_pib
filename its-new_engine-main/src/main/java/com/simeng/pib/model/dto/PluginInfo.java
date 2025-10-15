package com.simeng.pib.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 插件信息DTO
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginInfo {
    
    /**
     * 插件名称
     */
    private String pluginName;
    
    /**
     * 插件清单内容
     */
    private Map<String, Object> manifest;
    
    public PluginInfo(String pluginName, Map<String, Object> manifest) {
        this.pluginName = pluginName;
        this.manifest = manifest;
    }
}
