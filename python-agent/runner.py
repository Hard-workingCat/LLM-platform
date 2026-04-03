import os
import sys
import time
import requests
import subprocess
import argparse
import json

# Java 后端的地址
BACKEND_URL = "http://localhost:8080"

def report_status(task_id, is_success, error_msg=""):
    """向 Java 后端汇报任务执行结果"""
    url = f"{BACKEND_URL}/api/task/complete/{task_id}"
    payload = {
        "isSuccess": str(is_success).lower()
    }
    
    try:
        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] 正在向后端汇报任务状态: task_id={task_id}, success={is_success}")
        response = requests.post(url, params=payload)
        response.raise_for_status()
        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] 汇报成功: {response.text}")
    except Exception as e:
        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] 汇报失败: {str(e)}")

def run_training_task(task_id, gpu_devices, command):
    """
    执行实际的训练命令 (模拟使用 Docker 或直接运行)
    """
    print(f"\n{'='*50}")
    print(f"🚀 开始执行任务 ID: {task_id}")
    print(f"🖥️ 分配的 GPU 编号: {gpu_devices}")
    print(f"📜 执行命令: {command}")
    print(f"{'='*50}\n")
    
    # 构造实际的执行环境 (例如设置 CUDA_VISIBLE_DEVICES)
    env = os.environ.copy()
    env["CUDA_VISIBLE_DEVICES"] = gpu_devices
    
    start_time = time.time()
    
    try:
        # 这里为了演示，我们使用 subprocess 执行一个模拟的 shell 命令
        # 实际使用中，command 可能是 "torchrun --nproc_per_node=2 train.py ..."
        process = subprocess.Popen(
            command,
            shell=True,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            universal_newlines=True
        )
        
        # 实时打印输出日志
        for line in process.stdout:
            print(f"[TASK-{task_id} LOG] {line.strip()}")
            
        process.wait()
        
        end_time = time.time()
        print(f"\n✅ 任务 {task_id} 执行结束！耗时: {end_time - start_time:.2f} 秒。退出码: {process.returncode}")
        
        if process.returncode == 0:
            # 成功执行
            report_status(task_id, True)
        else:
            # 执行出错
            report_status(task_id, False, f"退出码非零: {process.returncode}")
            
    except Exception as e:
        print(f"\n❌ 任务 {task_id} 启动失败: {str(e)}")
        report_status(task_id, False, str(e))

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="LLM Platform Python Runner")
    parser.add_argument("--task-id", type=int, required=True, help="要执行的任务 ID")
    parser.add_argument("--gpus", type=str, required=True, help="分配的 GPU 设备列表，例如 '0,1'")
    parser.add_argument("--command", type=str, default="echo '模拟训练过程...' && sleep 5 && echo '训练完成'", help="要执行的 Shell/Docker 命令")
    
    args = parser.parse_args()
    
    run_training_task(args.task_id, args.gpus, args.command)
