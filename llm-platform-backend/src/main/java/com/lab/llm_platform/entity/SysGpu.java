package com.lab.llm_platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysGpu {
    private Long id;
    private Long nodeId;
    private Integer deviceIndex;
    private String gpuName;
    private Integer memoryTotal;
    private Integer status; // 0-空闲, 1-占用中, 2-故障/离线
    private Long currentTaskId;
    private LocalDateTime updatedAt;
}
