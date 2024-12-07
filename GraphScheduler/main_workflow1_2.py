import json
import time
import requests
from collections import defaultdict
from merger import DockerImageBuilder

class DAG:
    def __init__(self):
        self.graph = defaultdict(list)
        self.nodes = {}
    def add_node(self, node_id, name, async_flag, slo, memory=512):
        self.nodes[node_id] = {
            "name": name,
            "async": async_flag,
            "slo": slo,
            "memory": memory,
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

def set_memory_limit_for_all_functions(dag):
    url = "http://127.0.0.1:5000/set_memory_limit"
    for node_id, info in dag.nodes.items():
        image_name = node_id
        memory = info["memory"]
        response = requests.post(url, json={
            "image_name": image_name,
            "memory_limit": memory
        })
        if response.status_code == 200:
            print(f"Memory limit set for {image_name}: {memory} MB")
        else:
            print(f"Failed to set memory limit for {image_name}: {response.json()['error']}")
            
def invoke_workflow_entry(entry_function_id, num_calls=50, filename="default.pkl"):
    # print(f"invoke {entry_function_id}")
    url = "http://127.0.0.1:5000/invoke"
    log_url = "http://127.0.0.1:5000/get_logs"
    save_logs_url = "http://127.0.0.1:5000/save_logs"
    clear_logs_url = "http://127.0.0.1:5000/clear_logs"
    all_logs = {}
    for _ in range(num_calls):
        response = requests.post(url, json={
            "container_name": str(entry_function_id),
            "jar_name": "query-orders-for-refresh.jar"
            # "jar_name": "calculate-refund.jar"
            })
        if response.status_code == 200:
            # 获取最新的日志信息，等待个几秒，防止容器信息还没完全记录导致缺少log
            time.sleep(5)
            log_response = requests.get(log_url)
            if log_response.status_code == 200:
                logs = log_response.json()
                # print(logs)
                for container_name, container_info in logs.items():
                    if container_name not in all_logs:
                        all_logs[container_name] = []
                    all_logs[container_name].append(container_info)
    # 保存日志
    save_response = requests.post(save_logs_url, json={
        "filename": filename
    })
    if save_response.status_code != 200:
        print(f"Failed to save logs: {save_response.json()['error']}")
    # 清空日志
    requests.post(clear_logs_url)
    return all_logs

def update_dag_with_logs(dag, all_logs):
    for container_name, logs in all_logs.items():
        if logs:
            execution_times = [log['execution_time'] for log in logs]
            cold_start_times = [log['cold_start_time'] for log in logs]
            total_times = [log['total_time'] for log in logs]
            avg_execution_time = sum(execution_times) / len(execution_times)
            avg_cold_start_time = sum(cold_start_times) / len(cold_start_times)
            avg_total_time = sum(total_times) / len(total_times)
            node_id = container_name
            if node_id in dag.nodes:
                dag.nodes[node_id]['execution_time'] = avg_execution_time
                dag.nodes[node_id]['cold_start_time'] = avg_cold_start_time
                dag.nodes[node_id]['total_time'] = avg_total_time

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
    # new_momory = original_dag.nodes[merged_functions[0]]["memory"]+original_dag.nodes[merged_functions[1]]["memory"]
    # if new_momory > 1024:
    #     new_momory = 1024
    merged_dag.add_node(merged_image_name, 
                        merged_image_name, 
                        original_dag.nodes[merged_functions[0]]["async"], 
                        original_dag.nodes[merged_functions[0]]["slo"],
                        original_dag.nodes[merged_functions[0]]["memory"])
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

def calculate_cost(container_total_time, container_memory, num_calls):
    factor = 0.00001667
    # Lambda 0.00001667美元/GB-s
    cost = factor * container_memory/1024 * container_total_time/1000 + 0.000002 * num_calls
    return cost

def gradient_descent_optimization_for_dag(
    dag, entry_function_id, memory_step, 
    max_iterations=10, threshold_count=1, slo_runtime=None, num_calls=200):
    """
    使用梯度下降算法优化整个工作流的内存配置。

    :param dag: DAG 对象
    :param entry_function_id: 入口函数 ID
    :param memory_list: 可用的内存配置列表
    :param memory_step: 内存步长
    :param max_iterations: 最大迭代次数
    :param threshold_count: 局部最小值的阈值计数
    :param slo_runtime: 服务级别目标（SLO）运行时间
    :param num_calls: 每次调用入口函数的次数
    :return: 最优内存配置
    """
    # 每个节点的遇到的极值点
    step_counts = {node_id: 0 for node_id in dag.get_all_nodes()}  # 每个节点的 step_count
    # 每个函数不同memory的cost、runtime记录
    containers_cost = {}
    containers_runtime = {}
    # 保存初始化成本
    for node_id in dag.get_all_nodes():
        cost = calculate_cost(dag.nodes[node_id]['total_time'], dag.nodes[node_id]['memory'], num_calls)
        containers_cost[node_id] = {dag.nodes[node_id]['memory']: cost}
        runtime = dag.nodes[node_id]['total_time']
        containers_runtime[node_id] = {dag.nodes[node_id]['memory']: runtime}
    for iteration in range(max_iterations):
        # 保存原成本
        containers_memory_origin = {}
        containers_cost_origin = {}
        containers_runtime_origin = {}
        for node_id in dag.get_all_nodes():
            memory = dag.nodes[node_id]['memory']
            containers_memory_origin[node_id] = memory
            containers_cost_origin[node_id] = containers_cost[node_id][memory]
            containers_runtime_origin[node_id] = containers_runtime[node_id][memory]
        # 计算左侧邻居的成本
        containers_memory_left = {}
        containers_cost_left = {}
        containers_runtime_left = {}
        # 先判断左侧是否需要运行，已经跑过了就不用跑
        left_need_run = False
        for node_id in dag.get_all_nodes():
            if step_counts[node_id] >= threshold_count:
                containers_memory_left[node_id] = containers_memory_origin[node_id]
                continue  # 达到局部最小值阈值，跳过该节点的进一步优化
            left_neighbor_memory = containers_memory_origin[node_id] - memory_step
            if left_neighbor_memory <= 128:
                left_neighbor_memory = 128
            containers_memory_left[node_id] = left_neighbor_memory
            if left_neighbor_memory not in containers_cost[node_id]:
                left_need_run = True # 只要有一个函数需要更改内存，就跑
        if left_need_run:
            for node_id in dag.get_all_nodes():
                dag.nodes[node_id]['memory'] = containers_memory_left[node_id]
            set_memory_limit_for_all_functions(dag)
            all_logs = invoke_workflow_entry(entry_function_id, num_calls, f"workflow2_merged_left_{iteration}.pkl")
            update_dag_with_logs(dag, all_logs)
            for node_id in dag.get_all_nodes():
                memory = dag.nodes[node_id]['memory']
                containers_cost_left[node_id] = calculate_cost(dag.nodes[node_id]['total_time'], memory, num_calls)
                containers_runtime_left[node_id] = dag.nodes[node_id]['total_time']
                containers_cost[node_id][memory] = containers_cost_left[node_id]
                containers_runtime[node_id][memory] = containers_runtime_left[node_id]
        else:
            for node_id in dag.get_all_nodes():
                memory = containers_memory_left[node_id]
                containers_cost_left[node_id] = containers_cost[node_id][memory]
                containers_runtime_left[node_id] = containers_runtime[node_id][memory]

        # 计算右侧邻居的成本
        containers_memory_right = {}
        containers_cost_right = {}
        containers_runtime_right = {}
        # 先判断右侧是否需要运行，已经跑过了就不用跑
        right_need_run = False
        for node_id in dag.get_all_nodes():
            if step_counts[node_id] >= threshold_count:
                containers_memory_right[node_id] = containers_memory_origin[node_id]
                continue  # 达到局部最小值阈值，跳过该节点的进一步优化
            right_neighbor_memory = containers_memory_origin[node_id] + memory_step
            if right_neighbor_memory >= 1024:
                right_neighbor_memory = 1024
            containers_memory_right[node_id] = right_neighbor_memory
            if right_neighbor_memory not in containers_cost[node_id]:
                right_need_run = True # 只要有一个函数需要更改内存，就跑
        if right_need_run:
            for node_id in dag.get_all_nodes():
                dag.nodes[node_id]['memory'] = containers_memory_right[node_id]
            set_memory_limit_for_all_functions(dag)
            all_logs = invoke_workflow_entry(entry_function_id, num_calls, f"workflow2_merged_right_{iteration}.pkl")
            update_dag_with_logs(dag, all_logs)
            for node_id in dag.get_all_nodes():
                memory = dag.nodes[node_id]['memory']
                containers_cost_right[node_id] = calculate_cost(dag.nodes[node_id]['total_time'], memory, num_calls)
                containers_runtime_right[node_id] = dag.nodes[node_id]['total_time']
                containers_cost[node_id][memory] = containers_cost_right[node_id]
                containers_runtime[node_id][memory] = containers_runtime_right[node_id]
        else:
            for node_id in dag.get_all_nodes():
                memory = containers_memory_right[node_id]
                containers_cost_right[node_id] = containers_cost[node_id][memory]
                containers_runtime_right[node_id] = containers_runtime[node_id][memory]

        # 根据成本选择下一步的方向
        local_min_points = []
        for node_id in dag.get_all_nodes():
            if step_counts[node_id] >= threshold_count:
                continue  # 达到局部最小值阈值，跳过该节点的进一步优化
            if containers_memory_left[node_id] < containers_memory_origin[node_id]:
                # 左侧有成本
                if containers_cost_left[node_id] < containers_cost_origin[node_id] and containers_cost_left[node_id] < containers_cost_right[node_id]:
                    # 向左侧移动
                    if containers_memory_left[node_id] <= 128:
                        # 不能再小了，就这样吧
                        step_counts[node_id] = threshold_count
                        dag.nodes[node_id]['memory'] = 128
                        dag.nodes[node_id]['total_time'] = containers_runtime_left[node_id]
                    else:
                        dag.nodes[node_id]['memory'] = containers_memory_left[node_id]
                        dag.nodes[node_id]['total_time'] = containers_runtime_left[node_id]
                elif containers_cost_right[node_id] < containers_cost_origin[node_id] and containers_cost_right[node_id] < containers_cost_left[node_id]:
                    # 向右侧移动
                    if containers_memory_right[node_id] >= 1024:
                        # 不能再大了，就这样吧
                        step_counts[node_id] = threshold_count
                        dag.nodes[node_id]['memory'] = 1024
                        dag.nodes[node_id]['total_time'] = containers_runtime_right[node_id]
                    else:
                        dag.nodes[node_id]['memory'] = containers_memory_right[node_id]
                        dag.nodes[node_id]['total_time'] = containers_runtime_right[node_id]
                else:
                    # 找到局部最小点
                    step_counts[node_id] += 1
                    dag.nodes[node_id]['memory'] = containers_memory_origin[node_id]
                    dag.nodes[node_id]['total_time'] = containers_runtime_origin[node_id]
                    local_min_points.append(node_id)
            else:
                # 左侧没成本
                if containers_cost_right[node_id] < containers_cost_origin[node_id]:
                    # 向右侧移动
                    if containers_memory_right[node_id] >= 1024:
                        # 不能再大了，就这样吧
                        step_counts[node_id] = threshold_count
                        dag.nodes[node_id]['memory'] = 1024
                        dag.nodes[node_id]['total_time'] = containers_runtime_right[node_id]
                    else:
                        dag.nodes[node_id]['memory'] = containers_memory_right[node_id]
                        dag.nodes[node_id]['total_time'] = containers_runtime_right[node_id]
                else:
                    # 找到最后一个局部最小点
                    step_counts[node_id] = threshold_count
                    dag.nodes[node_id]['memory'] = containers_memory_origin[node_id]
                    dag.nodes[node_id]['total_time'] = containers_runtime_origin[node_id]

        # 检查是否超过SLO

        # 更新局部最小点，就向内存更小的方向
        for node_id in local_min_points:
            memory = containers_memory_left[node_id]
            dag.nodes[node_id]['memory'] = memory
            dag.nodes[node_id]['total_time'] = containers_runtime_left[node_id]

    return dag

# sudo /home/chenzebin/anaconda3/envs/chatglm/bin/python3 main.py
if __name__ == '__main__':
    # 读取JSON文件创建DAG对象
    with open('./workflows1.json', 'r') as f:
        json_data = json.load(f)
    dag = create_dag_from_json(json_data)
    # 获取工作流的入口函数ID
    entry_function_id = get_entry_function_id(dag)
    # 初始化调用入口函数num_calls次
    set_memory_limit_for_all_functions(dag)
    all_logs = invoke_workflow_entry(entry_function_id, 20, "workflow1_origin.pkl")
    # 将日志信息加到DAG对应的函数下面
    update_dag_with_logs(dag, all_logs)
    # 找出同步调用时间最长的函数和调用它的函数
    longest_sync_function, calling_function = find_longest_sync_functions(dag)
    merged_functions = [calling_function, longest_sync_function]
    
    # 融合这两个函数的镜像
    jar_paths = {
        '27': "./NestedWorkflow/workflow1/27/query-orders-for-refresh/build/libs/query-orders-for-refresh.jar",
        '44': "./NestedWorkflow/workflow1/44/get-stationid-list-by-name-list/build/libs/get-stationid-list-by-name-list.jar",
        '30': "./NestedWorkflow/workflow2/30/calculate-refund/build/libs/calculate-refund.jar",
        '23': "./NestedWorkflow/workflow3/23/get-order-by-id/build/libs/get-order-by-id.jar",
        '22': "./NestedWorkflow/workflow3/22/cancel-ticket/build/libs/cancel-ticket.jar",
        '28': "./NestedWorkflow/workflow3/28/drawback/build/libs/drawback.jar",
        '29': "./NestedWorkflow/workflow3/29/save-order-info/build/libs/save-order-info.jar"
    }
    merged_image_name = merge_images(merged_functions, "workflow1", jar_paths)
    # 创建融合后的DAG
    merged_dag = create_merged_dag(dag, merged_functions, merged_image_name)
    # print(merged_dag.nodes)
    # 获取工作流的入口函数ID
    entry_function_id = get_entry_function_id(merged_dag)
    # 调用入口函数num_calls次
    set_memory_limit_for_all_functions(merged_dag)
    all_logs = invoke_workflow_entry(entry_function_id, 20, "workflow1_merged.pkl")
    # 将日志信息加到DAG对应的函数下面
    update_dag_with_logs(merged_dag, all_logs)
    
    # 对融合后的DAG内存做出修改
    gradient_descent_optimization_for_dag(merged_dag, entry_function_id, memory_step=128, 
    max_iterations=5, threshold_count=1, slo_runtime=None, num_calls=20)