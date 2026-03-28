package com.lab.llm_platform.controller;

import com.lab.llm_platform.entity.BizTask;
import com.lab.llm_platform.service.BizTaskService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/task")
@CrossOrigin(origins = "*") // 允许前端跨域调用
public class BizTaskController {

    private final BizTaskService taskService;

    public BizTaskController(BizTaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 提交新的训练/推理任务
     */
    @PostMapping("/submit")
    public String submitTask(@RequestBody BizTask task) {
        Long taskId = taskService.submitTask(task);
        return "任务提交成功，进入排队。任务ID: " + taskId;
    }

    /**
     * 手动触发调度器（实际生产中这应该是一个定时任务 @Scheduled）
     */
    @PostMapping("/trigger-schedule")
    public String triggerSchedule() {
        taskService.scheduleTasks();
        return "调度触发成功，请查看控制台日志";
    }

    /**
     * 模拟：任务执行完毕回调（正常是由底层 Python/Docker 脚本执行完调用的）
     */
    @PostMapping("/complete/{taskId}")
    public String completeTask(@PathVariable Long taskId, @RequestParam(defaultValue = "true") boolean isSuccess) {
        taskService.completeTask(taskId, isSuccess, isSuccess ? null : "Simulated Failure");
        return "任务 " + taskId + " 回调完成，状态：" + (isSuccess ? "成功" : "失败");
    }
}
