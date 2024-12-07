import os
import pickle
import logging
import json
import random
import numpy as np

# 配置日志记录器
logging.basicConfig(filename='output.log', level=logging.INFO, format='%(message)s')

# 指定要读取的目录路径
directory_path = './data/workflow5/algorithm/'

def generate_new_data(container_name, memory_limit, 
                      cold_start_times, execution_times, 
                      num_samples, calling_container):
    # 计算前20个数据的均值和标准差
    cold_start_mean = np.mean(cold_start_times)
    cold_start_std = np.std(cold_start_times)
    execution_mean = np.mean(execution_times)
    execution_std = np.std(execution_times)
    
    # 获取最大值和最小值
    cold_start_min = min(cold_start_times)
    cold_start_max = max(cold_start_times)
    execution_min = min(execution_times)
    execution_max = max(execution_times)
    
    new_data = []
    for _ in range(num_samples):
        while True:
            cold_start_time = np.random.normal(cold_start_mean, cold_start_std)
            execution_time = np.random.normal(execution_mean, execution_std)
            if cold_start_min <= cold_start_time <= cold_start_max and execution_min <= execution_time <= execution_max:
                break
        
        total_time = cold_start_time + execution_time
        new_data.append({
            "container_name": container_name,
            "memory_limit": memory_limit,
            "cold_start_time": cold_start_time,
            "execution_time": execution_time,
            "total_time": total_time,
            "calling_container": calling_container
        })
    
    return new_data

def process_pkl_file(file_path):
    try:
        # 打开并加载 .pkl 文件
        with open(file_path, 'rb') as f:
            data = pickle.load(f)
        
        # 扩充数据
        new_data = {}
        for container_name, records in data.items():
            # 按 calling_container 分类
            calling_container_data = {}
            for record in records:
                calling_container = record['calling_container']
                if calling_container not in calling_container_data:
                    calling_container_data[calling_container] = []
                calling_container_data[calling_container].append(record)
            
            # 生成新的数据
            new_records = []
            for calling_container, container_records in calling_container_data.items():
                cold_start_times = [record['cold_start_time'] for record in container_records]
                execution_times = [record['execution_time'] for record in container_records]
                memory_limit = container_records[0]['memory_limit']  # 取第一个数据的 memory_limit
                
                # 生成新的数据
                num_samples = len(container_records) * 9  # 每个 calling_container 的数据乘以 9
                new_records.extend(generate_new_data(container_name, memory_limit, cold_start_times, execution_times, num_samples, calling_container))
            
            # 将所有数据和新的数据合并
            new_data[container_name] = records + new_records
        
        # 将新的数据保存到新的 .pkl 文件中
        new_file_path = file_path.replace('.pkl', '_expanded.pkl')
        with open(new_file_path, 'wb') as f:
            pickle.dump(new_data, f)
        
        # 将数据格式化为 JSON 字符串并记录到日志文件中
        formatted_data = json.dumps(new_data, indent=4)
        logging.info(f"Processed file: {file_path}\n{formatted_data}")
        
        print(f"数据已成功加载并保存到 {new_file_path} 文件中。")

    except Exception as e:
        # 如果出现错误，记录错误信息
        logging.error(f"处理文件 {file_path} 时出错: {e}")
        print(f"处理文件 {file_path} 时出错: {e}")

# 遍历目录中的所有 .pkl 文件
for filename in os.listdir(directory_path):
    if filename.endswith('.pkl'):
        file_path = os.path.join(directory_path, filename)
        process_pkl_file(file_path)