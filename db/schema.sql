-- ==========================================================
-- 实验室大模型训练与推理平台 - 核心数据库结构 (MySQL)
-- ==========================================================

CREATE DATABASE IF NOT EXISTS `llm_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `llm_platform`;

-- 1. 用户表 (User)
-- 实验室成员统一管理，区分管理员（导师/负责同学）和普通学生
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '学号/用户名',
    `password_hash` VARCHAR(128) NOT NULL COMMENT '密码哈希',
    `real_name` VARCHAR(64) COMMENT '真实姓名',
    `role` TINYINT NOT NULL DEFAULT 1 COMMENT '0-管理员, 1-普通学生',
    `email` VARCHAR(128) COMMENT '邮箱，用于任务完成通知',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户信息表';


-- 2. 节点与显卡管理 (Node & GPU)
-- 记录服务器节点和具体 GPU 的状态，用于调度系统
CREATE TABLE IF NOT EXISTS `sys_node` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `hostname` VARCHAR(128) NOT NULL COMMENT '服务器主机名',
    `ip_address` VARCHAR(64) NOT NULL COMMENT '服务器IP',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '0-离线, 1-在线',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='服务器节点表';

CREATE TABLE IF NOT EXISTS `sys_gpu` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `node_id` BIGINT NOT NULL COMMENT '所属节点ID',
    `device_index` INT NOT NULL COMMENT '显卡编号 (0, 1, 2, ...)',
    `gpu_name` VARCHAR(64) COMMENT '显卡型号 (如 RTX 4090, A100)',
    `memory_total` INT COMMENT '总显存 (MB)',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-空闲, 1-占用中, 2-故障/离线',
    `current_task_id` BIGINT DEFAULT NULL COMMENT '当前正在运行的任务ID',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_node_status` (`node_id`, `status`)
) ENGINE=InnoDB COMMENT='GPU状态信息表';


-- 3. 数据集管理 (Dataset)
-- 统一管理大家的数据集，避免重复上传和乱放
CREATE TABLE IF NOT EXISTS `biz_dataset` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL COMMENT '数据集名称',
    `description` TEXT COMMENT '数据集描述',
    `storage_path` VARCHAR(255) NOT NULL COMMENT '服务器上的绝对路径',
    `format` VARCHAR(32) COMMENT '格式 (jsonl, csv, etc.)',
    `creator_id` BIGINT NOT NULL COMMENT '上传者/创建者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='数据集管理表';


-- 4. 镜像环境管理 (Environment / Image)
-- 标准化环境配置，解决"我能跑你不能跑"的问题
CREATE TABLE IF NOT EXISTS `biz_environment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL COMMENT '环境名称 (如 PyTorch 2.1 + CUDA 12.1)',
    `docker_image` VARCHAR(255) NOT NULL COMMENT 'Docker镜像标签',
    `type` TINYINT NOT NULL DEFAULT 0 COMMENT '0-训练环境, 1-推理环境',
    `is_public` TINYINT NOT NULL DEFAULT 1 COMMENT '1-所有人可用, 0-私有',
    `creator_id` BIGINT COMMENT '创建者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='运行环境(镜像)管理表';


-- 5. 模型版本与产物管理 (Model Artifact)
-- 管理基础模型（如 Llama3-8B）和微调产出模型
CREATE TABLE IF NOT EXISTS `biz_model` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL COMMENT '模型名称',
    `version` VARCHAR(64) NOT NULL COMMENT '版本号',
    `base_model_id` BIGINT COMMENT '基于哪个模型微调 (null为基础模型)',
    `storage_path` VARCHAR(255) NOT NULL COMMENT '权重存储路径',
    `format` VARCHAR(32) COMMENT '权重格式 (safetensors, bin, onnx, trt)',
    `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
    `source_task_id` BIGINT COMMENT '由哪个训练任务产出',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='模型资产表';


-- 6. 核心：任务队列调度表 (Task Queue)
-- 解决抢卡排队问题，所有任务都在此排队
CREATE TABLE IF NOT EXISTS `biz_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `user_id` BIGINT NOT NULL COMMENT '提交人',
    `task_type` TINYINT NOT NULL COMMENT '0-训练/微调, 1-推理/部署',
    
    -- 资源与环境要求
    `env_id` BIGINT NOT NULL COMMENT '所需环境(镜像)ID',
    `gpu_required` INT NOT NULL DEFAULT 1 COMMENT '需要几张卡',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级(越大越高)',
    
    -- 任务参数与关联
    `dataset_id` BIGINT COMMENT '使用的数据集ID',
    `base_model_id` BIGINT COMMENT '使用的基础模型ID',
    `hyperparameters` JSON COMMENT '超参数配置 (JSON格式)',
    `command` TEXT COMMENT '启动命令/入口脚本',
    
    -- 状态流转
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-排队中, 1-运行中, 2-已完成, 3-失败, 4-已取消',
    `error_msg` TEXT COMMENT '失败时的报错信息',
    
    -- 运行时信息
    `allocated_gpus` VARCHAR(128) COMMENT '实际分配的GPU (JSON数组)',
    `log_path` VARCHAR(255) COMMENT '日志文件路径',
    `output_model_id` BIGINT COMMENT '任务产出的模型ID (训练任务)',
    `inference_url` VARCHAR(255) COMMENT '服务调用地址 (推理任务)',
    
    -- 时间戳
    `submitted_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `started_at` DATETIME COMMENT '开始执行时间',
    `finished_at` DATETIME COMMENT '结束时间',
    
    INDEX `idx_status_priority` (`status`, `priority`)
) ENGINE=InnoDB COMMENT='任务队列与调度表';
