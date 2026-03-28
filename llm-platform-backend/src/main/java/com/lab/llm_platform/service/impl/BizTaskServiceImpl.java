package com.lab.llm_platform.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.llm_platform.entity.BizTask;
import com.lab.llm_platform.entity.SysGpu;
import com.lab.llm_platform.mapper.BizTaskMapper;
import com.lab.llm_platform.mapper.SysGpuMapper;
import com.lab.llm_platform.service.BizTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BizTaskServiceImpl implements BizTaskService {

    private final BizTaskMapper taskMapper;
    private final SysGpuMapper gpuMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BizTaskServiceImpl(BizTaskMapper taskMapper, SysGpuMapper gpuMapper) {
        this.taskMapper = taskMapper;
        this.gpuMapper = gpuMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitTask(BizTask task) {
        task.setStatus(0); // 0-排队中
        task.setSubmittedAt(LocalDateTime.now());
        taskMapper.insert(task);
        log.info("新任务已提交，进入排队状态，任务ID: {}", task.getId());
        
        // 尝试立即调度一次
        scheduleTasks();
        
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized void scheduleTasks() {
        // 1. 获取所有排队中的任务（按优先级降序）
        List<BizTask> pendingTasks = taskMapper.selectByStatusOrderByPriorityDesc(0);
        if (pendingTasks.isEmpty()) {
            return; // 没有排队任务
        }

        // 2. 获取所有空闲的 GPU
        List<SysGpu> freeGpus = gpuMapper.selectByStatus(0); // 0-空闲
        int freeGpuCount = freeGpus.size();
        
        log.info("开始调度：排队任务数 = {}, 空闲GPU数 = {}", pendingTasks.size(), freeGpuCount);

        // 3. 遍历任务队列，尝试分配资源
        for (BizTask task : pendingTasks) {
            Integer requiredGpuCount = task.getGpuRequired();
            
            // 如果空闲卡不够满足当前任务，跳过（或者你可以实现更复杂的调度算法，比如避免小任务饿死大任务）
            if (freeGpuCount < requiredGpuCount) {
                log.info("任务 {} 需要 {} 张卡，当前空闲 {} 张卡，资源不足等待下一轮", task.getId(), requiredGpuCount, freeGpuCount);
                continue;
            }

            // 4. 分配 GPU
            List<SysGpu> allocatedGpusForThisTask = freeGpus.subList(0, requiredGpuCount);
            List<Long> gpuIds = allocatedGpusForThisTask.stream().map(SysGpu::getId).collect(Collectors.toList());
            List<Integer> deviceIndices = allocatedGpusForThisTask.stream().map(SysGpu::getDeviceIndex).collect(Collectors.toList());

            // 5. 更新 GPU 状态为占用中 (status = 1)
            for (SysGpu gpu : allocatedGpusForThisTask) {
                gpuMapper.updateStatusAndTaskId(gpu.getId(), 1, task.getId());
            }

            // 6. 更新任务状态为运行中 (status = 1)
            try {
                task.setStatus(1);
                task.setAllocatedGpus(objectMapper.writeValueAsString(deviceIndices));
                task.setStartedAt(LocalDateTime.now());
                taskMapper.updateById(task);
            } catch (JsonProcessingException e) {
                log.error("JSON 序列化失败", e);
            }

            // 7. 更新空闲 GPU 列表（扣除已分配的）
            freeGpus = freeGpus.subList(requiredGpuCount, freeGpus.size());
            freeGpuCount -= requiredGpuCount;

            log.info("成功为任务 {} 分配了 {} 张显卡，分配的设备编号: {}", task.getId(), requiredGpuCount, deviceIndices);
            
            // TODO: 在这里可以调用真正的 Shell 命令或 Docker API 来启动训练脚本！
            // 模拟：打印一条将要执行的 docker run 命令
            String dockerCmd = String.format("docker run --gpus '\"device=%s\"' my-pytorch-image:latest python train.py", 
                    deviceIndices.stream().map(String::valueOf).collect(Collectors.joining(",")));
            log.info(">> 准备执行命令: {}", dockerCmd);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, boolean isSuccess, String errorMsg) {
        BizTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 1) {
            log.warn("任务不存在或未处于运行中状态，任务ID: {}", taskId);
            return;
        }

        // 1. 释放占用的 GPU (重置 status=0, current_task_id=null)
        // 简单实现：找到所有 current_task_id = taskId 的 GPU，恢复它们
        // 这里为了简化，我们在 Mapper 里加一个根据 taskId 更新的 SQL 会更好，或者遍历查询出来的所有 GPU 判断。
        // 但我们直接遍历所有的显卡，找到对应的任务 id（实际项目中应在 Mapper 增加 update 方法）
        List<SysGpu> allGpus = gpuMapper.selectAll();
        for (SysGpu gpu : allGpus) {
            if (taskId.equals(gpu.getCurrentTaskId())) {
                gpuMapper.updateStatusAndTaskId(gpu.getId(), 0, null);
            }
        }

        // 2. 更新任务状态为完成或失败
        task.setStatus(isSuccess ? 2 : 3);
        task.setFinishedAt(LocalDateTime.now());
        task.setErrorMsg(errorMsg);
        taskMapper.updateById(task);

        log.info("任务 {} 执行完毕，结果：{}。已释放其占用的显卡。", taskId, isSuccess ? "成功" : "失败");
    }
}
