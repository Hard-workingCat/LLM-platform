package com.lab.llm_platform.service;

import com.lab.llm_platform.entity.BizTask;

public interface BizTaskService {
    // 提交任务（将其加入排队状态）
    Long submitTask(BizTask task);
    
    // 触发调度（由定时任务调用，扫描空闲GPU并派发任务）
    void scheduleTasks();

    // 任务完成（由底层 Docker/Python 脚本回调），释放显卡
    void completeTask(Long taskId, boolean isSuccess, String errorMsg);
}
