import json
import requests
from collections import defaultdict
from merger import DockerImageBuilder

class DAG:
    def __init__(self):
        self.graph = defaultdict(list)
        self.nodes = {}
    def add_node(self, node_id, name, async_flag, slo):
        self.nodes[node_id] = {
            "name": name,
            "async": async_flag,
            "slo": slo
        }
    def add_edge(self, from_node, to_node):
        self.graph[to_node].append(from_node)
    def get_dependencies(self, node_id):
        return self.graph[node_id]
    def get_node_info(self, node_id):
        return self.nodes.get(node_id, {})
    def get_all_nodes(self):
        return set(self.nodes.keys())

def create_dag_from_json(json_data):
    dag = DAG()
    for workflow in json_data['workflows']:
        for function in workflow['functions']:
            dag.add_node(function['id'], function['name'], function['async'], function['slo'])
            for dependency in function['dependencies']:
                dag.add_edge(dependency, function['id'])
    return dag

def get_entry_function_id(dag):
    for node_id, node_info in dag.nodes.items():
        if node_info['async'] and not dag.get_dependencies(node_id):
            return node_id
    return None

def invoke_workflow_entry(entry_function_id, num_calls=50):
    url = "http://127.0.0.1:5000/invoke"
    log_url = "http://127.0.0.1:5000/get_logs"
    clear_logs_url = "http://127.0.0.1:5000/clear_logs"
    all_logs = {}
    for _ in range(num_calls):
        response = requests.post(url, json={"container_name": str(entry_function_id)})
        if response.status_code == 200:
            # 获取最新的日志信息
            log_response = requests.get(log_url)
            if log_response.status_code == 200:
                logs = log_response.json()
                for container_name, container_info in logs.items():
                    if container_name not in all_logs:
                        all_logs[container_name] = []
                    all_logs[container_name].append(container_info)
    # 清空日志
    requests.post(clear_logs_url)
    return all_logs

def update_dag_with_logs(dag, all_logs):
    for container_name, logs in all_logs.items():
        if logs:
            execution_times = [log['execution_time'] for log in logs]
            cold_start_times = [log['cold_start_time'] for log in logs]
            memory_limit = logs[0]['memory_limit']
            avg_execution_time = sum(execution_times) / len(execution_times)
            avg_cold_start_time = sum(cold_start_times) / len(cold_start_times)
            node_id = container_name
            if node_id in dag.nodes:
                dag.nodes[node_id]['execution_time'] = avg_execution_time
                dag.nodes[node_id]['cold_start_time'] = avg_cold_start_time
                dag.nodes[node_id]['memory_limit'] = memory_limit

def find_longest_sync_functions(dag):
    sync_functions = {node_id: info["execution_time"] for node_id, info in dag.nodes.items() if not info['async']}
    sorted_functions = sorted(sync_functions.items(), key=lambda x: x[1], reverse=True)
    longest_sync_function = sorted_functions[0][0]
    calling_functions = dag.graph[longest_sync_function]
    return longest_sync_function, calling_functions[0]

def merge_images(function_ids, workflow_name, jar_paths):
    builder = DockerImageBuilder()
    dockerfile_path = builder.create_dockerfile(function_ids, workflow_name, jar_paths)
    image_name = f"{'_'.join(map(str, function_ids))}"
    builder.build_docker_image(dockerfile_path, image_name)
    return image_name

def create_merged_dag(original_dag, merged_functions, merged_image_name):
    merged_dag = DAG()
    # 添加新的融合函数节点
    merged_dag.add_node(merged_image_name, 
                        merged_image_name, 
                        original_dag.nodes[merged_functions[0]]["async"], 
                        original_dag.nodes[merged_functions[0]]["slo"])
    # 复制原始DAG中的所有节点和边到新的DAG中，并更新依赖关系
    for node_id, node_info in original_dag.nodes.items():
        if node_id not in merged_functions:
            merged_dag.add_node(node_id, node_info['name'], node_info['async'], node_info['slo'])
    for node_id, dependencies in original_dag.graph.items():
        if node_id == merged_functions[0]:
            for dep in dependencies:
                merged_dag.add_edge(dep, merged_image_name)
            continue
        if node_id == merged_functions[1]:
            continue
        for dep in dependencies:
            if dep == merged_functions[0]:
                merged_dag.add_edge(merged_image_name, node_id)
            elif dep == merged_functions[1]:
                merged_dag.add_edge(merged_image_name, node_id)
            else:
                merged_dag.add_edge(dep, node_id)
    return merged_dag

# sudo /home/chenzebin/anaconda3/envs/chatglm/bin/python3 main.py
if __name__ == '__main__':
    # 读取JSON文件创建DAG对象
    with open('./workflows1.json', 'r') as f:
        json_data = json.load(f)
    dag = create_dag_from_json(json_data)
    # 获取工作流的入口函数ID
    entry_function_id = get_entry_function_id(dag)
    # 初始化调用入口函数num_calls次
    all_logs = invoke_workflow_entry(entry_function_id, 2)
    # 将日志信息加到DAG对应的函数下面
    update_dag_with_logs(dag, all_logs)
    # 找出同步调用时间最长的函数和调用它的函数
    longest_sync_function, calling_function = find_longest_sync_functions(dag)
    merged_functions = [calling_function, longest_sync_function]
    
    # 融合这两个函数的镜像
    jar_paths = {
        '27': "./NestedWorkflow/workflow1/27/query-orders-for-refresh/build/libs/query-orders-for-refresh.jar",
        '44': "./NestedWorkflow/workflow1/44/get-stationid-list-by-name-list/build/libs/get-stationid-list-by-name-list.jar"
    }
    merged_image_name = merge_images(merged_functions, "workflow1", jar_paths)
    # 创建融合后的DAG
    merged_dag = create_merged_dag(dag, merged_functions, merged_image_name)
    # 获取工作流的入口函数ID
    entry_function_id = get_entry_function_id(merged_dag)
    # 调用入口函数num_calls次
    all_logs = invoke_workflow_entry(entry_function_id, 2)
    print(all_logs)