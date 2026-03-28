package com.lab.llm_platform.scheduler;

import com.lab.llm_platform.service.BizTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TaskScheduler {

    private final BizTaskService taskService;

    public TaskScheduler(BizTaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 每 10 秒钟执行一次调度器，扫描排队任务和空闲显卡
     */
    @Scheduled(fixedRate = 10000)
    public void schedulePendingTasks() {
        log.info("【调度器】开始扫描排队任务...");
        taskService.scheduleTasks();
    }
}
