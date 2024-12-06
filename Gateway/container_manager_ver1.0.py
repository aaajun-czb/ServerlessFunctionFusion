import os
import pickle
import docker
import subprocess
import re
import json
from dateutil import parser

class ContainerManager:
    def __init__(self):
        self.client = docker.from_env()
        self.output_dir = "./tmp/"   
        # 私有变量，记录整个运行期间调用的容器信息
        self._container_logs = {}
        # 私有变量，记录每个容器镜像的内存设置
        self._image_memory_settings = {}

    def get_host_ip(self):
        # 获取主机的IP地址
        try:
            output = subprocess.check_output(['ip', 'route', 'get', '1']).decode().split()
            return output[6]
        except subprocess.CalledProcessError:
            return '127.0.0.1'  # 默认返回环回地址
        
    def get_container_ip(self, container_name):
        # 获取指定容器的IP地址
        container = self.client.containers.get(container_name)
        network_settings = container.attrs['NetworkSettings']
        ip_address = network_settings['IPAddress']
        return ip_address

    def set_image_memory_setting(self, image_name, memory_limit):
        """设置容器镜像的内存设置"""
        self._image_memory_settings[image_name] = memory_limit

    def run_container(self, image_name, jar_name, input_data):
        try:
            host_ip = self.get_host_ip()
            mongo_ip = self.get_container_ip('some-mongo')
            # 如果未指定镜像的默认内存
            if image_name not in self._image_memory_settings:
                self._image_memory_settings[image_name] = 512
            memory_limit = self._image_memory_settings.get(image_name, 512)
            # 构建命令行参数
            command = ["java", "-jar", jar_name]
            if input_data:
                command.extend(input_data)

            # 运行容器并添加主机IP到extra_hosts
            container = self.client.containers.run(
                image_name, 
                command=command,  # 使用构建的命令行参数
                detach=True, 
                network_mode='host',
                extra_hosts={
                    'host.docker.internal': host_ip,
                    'some-mongo': mongo_ip
                },
                cpu_quota=int(memory_limit/1024 * 100000),
                cpu_period=100000,
                mem_limit=f"{memory_limit}m",
            )

            container.wait()  # 等待容器完成
            # 获取容器的输出
            output = container.logs().decode('utf-8')
            print(output)
            # 获取容器的启动和停止时间
            container.reload()
            start_time_str = container.attrs['State']['StartedAt']
            end_time_str = container.attrs['State']['FinishedAt']
            # 使用 dateutil 解析时间字符串
            start_time = parser.isoparse(start_time_str)
            end_time = parser.isoparse(end_time_str)
            # 计算冷启动时间和执行时间
            execution_time_str = re.search(r'Execution Time: (\d+) ms', output)
            execution_time = int(execution_time_str.group(1))
            total_time = 1000*(end_time - start_time).total_seconds()
            cold_start_time = total_time - execution_time
            # 删除容器
            container.remove()
            # 记录容器信息
            container_info = {
                "container_name": image_name,
                "memory_limit": memory_limit,
                "cold_start_time": cold_start_time,
                "execution_time": execution_time,
                "total_time": total_time
            }
            # 将日志信息添加到对应 container_name 的列表中
            if image_name not in self._container_logs:
                self._container_logs[image_name] = []
            self._container_logs[image_name].append(container_info)

            # 解析输出并提取有用的信息
            response_status = re.search(r'Response Status: (\d+)', output)
            response_message = re.search(r'Response Message: (.+)', output)
            response_data = extract_response_data(output)
            result = {
                "status": int(response_status.group(1)) if response_status else None,
                "message": response_message.group(1) if response_message else None,
                "data": response_data
            }
            return result

        except docker.errors.ContainerError as e:
            raise Exception(f"Container error: {str(e)}")
        except docker.errors.ImageNotFound as e:
            raise Exception(f"Image not found: {str(e)}")
        except Exception as e:
            raise Exception(f"Unexpected error: {str(e)}")

    def get_container_logs(self):
        """获取每个容器的最后一次调用的容器信息"""
        latest_logs = {}
        for container_name, logs in self._container_logs.items():
            if logs:
                latest_logs[container_name] = logs[-1]
        return latest_logs

    def get_image_memory_settings(self):
        """获取每个容器镜像的内存设置"""
        return dict(self._image_memory_settings)
    
    def save_logs_to_pickle(self, filename="container_logs.pkl"):
        """将日志保存为pickle文件"""
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)
        file_path = os.path.join(self.output_dir, filename)
        with open(file_path, 'wb') as f:
            pickle.dump(self._container_logs, f)
        print(f"Logs saved to {file_path}")
        
    def clear_logs(self):
        """清空日志"""
        self._container_logs = {}
        
def extract_response_data(output):
    # 使用正则表达式匹配 Response Data: 后面的内容，直到换行符
    response_data_match = re.search(r'Response Data: (.*?)\n', output, re.DOTALL)
    if response_data_match:
        response_data_str = response_data_match.group(1).strip()
        try:
            # 尝试将字符串解析为 JSON 对象
            response_data = json.loads(response_data_str)
            return response_data
        except json.JSONDecodeError:
            # 如果解析失败，返回原始字符串
            return response_data_str
    return None