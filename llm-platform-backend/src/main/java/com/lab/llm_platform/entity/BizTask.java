package com.lab.llm_platform.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BizTask {
    private Long id;
    private String taskName;
    private Long userId;
    private Integer taskType; // 0-训练/微调, 1-推理/部署
    
    // 资源与环境要求
    private Long envId;
    private Integer gpuRequired;
    private Integer priority;
    
    // 任务参数与关联
    private Long datasetId;
    private Long baseModelId;
    private String hyperparameters; // JSON 字符串，后续可使用 TypeHandler 映射
    private String command;
    
    // 状态流转
    private Integer status; // 0-排队中, 1-运行中, 2-已完成, 3-失败, 4-已取消
    private String errorMsg;
    
    // 运行时信息
    private String allocatedGpus; // JSON 数组，后续可使用 TypeHandler 映射
    private String logPath;
    private Long outputModelId;
    private String inferenceUrl;
    
    // 时间戳
    private LocalDateTime submittedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
