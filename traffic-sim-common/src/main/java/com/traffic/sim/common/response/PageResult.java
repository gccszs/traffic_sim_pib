package com.traffic.sim.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果
 * 
 * @param <T> 数据类型
 * @author traffic-sim
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 数据列表 */
    private List<T> records;
    
    /** 总记录数 */
    private Long total;
    
    /** 当前页码 */
    private Integer page;
    
    /** 每页大小 */
    private Integer size;
    
    /** 总页数 */
    private Integer pages;
    
    public PageResult(List<T> records, Long total, Integer page, Integer size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
    }
}

