package com.lab.llm_platform.mapper;

import com.lab.llm_platform.entity.SysGpu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysGpuMapper {
    List<SysGpu> selectAll();
    
    // 获取指定状态的 GPU（调度器找空闲显卡需要用到）
    List<SysGpu> selectByStatus(Integer status);
    
    // 更新 GPU 状态和关联的任务 ID
    int updateStatusAndTaskId(@Param("id") Long id, @Param("status") Integer status, @Param("taskId") Long taskId);
}
