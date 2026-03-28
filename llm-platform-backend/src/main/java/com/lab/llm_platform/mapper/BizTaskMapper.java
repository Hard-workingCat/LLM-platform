package com.lab.llm_platform.mapper;

import com.lab.llm_platform.entity.BizTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BizTaskMapper {
    int insert(BizTask bizTask);
    int updateById(BizTask bizTask);
    BizTask selectById(Long id);
    
    // 查询指定状态的任务，按优先级降序、提交时间升序排序（调度器需要用到）
    List<BizTask> selectByStatusOrderByPriorityDesc(Integer status);
}
