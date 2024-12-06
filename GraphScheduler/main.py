import json
import time
import requests
from collections import defaultdict
from merger import DockerImageBuilder

class DAG:
    def __init__(self):
        self.graph = defaultdict(list)
        self.nodes = {}
    def add_node(self, node_id, name, async_flag, slo, dependencies, memory=512):
        self.nodes[node_id] = {
            "name": name,
            "async": async_flag,
            "slo": slo,
            "memory": memory,
            "merged_try": {dependency: False for dependency in dependencies},
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
            dag.add_node(function['id'], function['name'], function['async'], function['slo'], function['dependencies'])
            for dependency in function['dependencies']:
                dag.add_edge(dependency, function['id'])
    return dag

def update_calling_maps(dag):
    calling_maps = {}
    for node_id in dag.nodes:
        functions = convert_function_string(node_id)
        for function in functions:
            calling_maps[function] = node_id
    # 发送POST请求修改网关记录的环境变量
    url = "http://127.0.0.1:5000/set_environment_variable"
    for key, value in calling_maps.items():
        response = requests.post(url, json={
            "key": key,
            "value": value
        })
        if response.status_code == 200:
            print(f"Calling Map set for {key}: {value}")
        else:
            print(f"Failed to set calling map for {key}: {response.json()['error']}")

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
    log_url = "http://127.0.0.1:5000/get_all_logs"
    save_logs_url = "http://127.0.0.1:5000/save_logs"
    clear_logs_url = "http://127.0.0.1:5000/clear_logs"
    all_logs = {}
    for _ in range(num_calls):
        response = requests.post(url, json={
            "container_name": str(entry_function_id),
            "jar_name": "get-left-trip-tickets.jar",
            "data": ["Shang Hai", "Su Zhou", "2024-12-15 00:00:00"],
            "calling_container": "0",
            })
    # 获取日志
    all_logs = requests.get(log_url)
    # 保存日志
    save_response = requests.post(save_logs_url, json={
        "filename": filename
    })
    if save_response.status_code != 200:
        print(f"Failed to save logs: {save_response.json()['error']}")
    # 清空日志
    requests.post(clear_logs_url)
    return all_logs.json()

def update_dag_with_logs(dag, all_logs, num_calls):
    for container_name, logs in all_logs.items():
        # 创建字段储存容器耗时
        node_id = container_name
        dag.nodes[node_id]['execution_time'] = {}
        dag.nodes[node_id]['cold_start_time'] = {}
        dag.nodes[node_id]['total_time'] = {}
        dag.nodes[node_id]['called_times'] = {}
        # 创建一个字典来存储每个调用容器的统计数据
        calling_container_stats = {}
        for log in logs:
            # 如果调用容器不在统计字典中，初始化它
            if log['calling_container'] not in calling_container_stats:
                calling_container_stats[log['calling_container']] = {
                    'execution_time': 0,
                    'cold_start_time': 0,
                    'total_time': 0,
                    'called_times': 0,
                }
            # 更新统计数据
            calling_container_stats[log['calling_container']]['execution_time'] += log['execution_time']
            calling_container_stats[log['calling_container']]['cold_start_time'] += log['cold_start_time']
            calling_container_stats[log['calling_container']]['total_time'] += log['total_time']
            calling_container_stats[log['calling_container']]['called_times'] += 1
        # 更新平均数据到DAG的节点中
        for calling_container, info in calling_container_stats.items():
            avg_execution_time = info['execution_time'] / num_calls
            avg_cold_start_time = info['cold_start_time'] / num_calls
            avg_total_time = info['total_time'] / num_calls
            avg_called_times = info['called_times'] / num_calls
            node_id = container_name
            dag.nodes[node_id]['execution_time'][calling_container] = avg_execution_time
            dag.nodes[node_id]['cold_start_time'][calling_container] = avg_cold_start_time
            dag.nodes[node_id]['total_time'][calling_container] = avg_total_time
            dag.nodes[node_id]['called_times'][calling_container] = avg_called_times

def find_longest_sync_functions(dag):
    max_total_time = 0
    longest_pair = None
    for node_id, info in dag.nodes.items():
        if info['async']:
            continue
        total_time_dict = info['total_time']
        for calling_container, total_time in total_time_dict.items():
            # 尝试过融合，忽略
            if info['merged_try'][calling_container]:
                continue
            if total_time > max_total_time:
                max_total_time = total_time
                longest_pair = (calling_container, node_id)
    return longest_pair

def convert_function_string(function_str):
    if '_' in function_str:
        return function_str.split('_')
    else:
        return [function_str]

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
                        original_dag.get_dependencies(merged_functions[0]),
                        original_dag.nodes[merged_functions[0]]["memory"])
    # 复制原始DAG中的所有节点到新的DAG中
    for node_id, node_info in original_dag.nodes.items():
        if node_id not in merged_functions:
            dependencies = original_dag.get_dependencies(node_id)
            for i in range(len(dependencies)):
                if dependencies[i] == merged_functions[0] or dependencies[i] == merged_functions[1]:
                    dependencies[i] = merged_image_name
            merged_dag.add_node(node_id, 
                                node_info['name'], 
                                node_info['async'], 
                                node_info['slo'], 
                                dependencies,
                                node_info['memory'])
    # 并更新依赖关系
    for node_id, dependencies in original_dag.graph.items():
        # 主调函数
        if node_id == merged_functions[0]:
            for dep in dependencies:
                merged_dag.add_edge(dep, merged_image_name)
            continue
        # 被调函数
        if node_id == merged_functions[1]:
            continue
        # 其他函数
        for dep in dependencies:
            if dep == merged_functions[0]:
                merged_dag.add_edge(merged_image_name, node_id)
            elif dep == merged_functions[1]:
                merged_dag.add_edge(merged_image_name, node_id)
            else:
                merged_dag.add_edge(dep, node_id)
    return merged_dag

def calculate_cost(container_total_time, container_memory, called_times):
    # Lambda 0.00001667美元/GB-s
    factor = 0.00001667
    # Lambda 每1百万次调用 0.2USD = 0.0000002美元/次
    # 忽略Step Functions状态转换的钱，那个太贵了
    total_called_times = 0
    for _, value in called_times.items():
        total_called_times += value
    total_time = 0
    for _, value in container_total_time.items():
        total_time += value
    cost = factor * container_memory/1024 * total_time/1000 + 0.00002 * total_called_times
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
        cost = calculate_cost(dag.nodes[node_id]['total_time'], dag.nodes[node_id]['memory'], dag.nodes[node_id]["called_times"])
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
            all_logs = invoke_workflow_entry(entry_function_id, num_calls, f"workflow5_merged_left_{iteration}.pkl")
            update_dag_with_logs(dag, all_logs, num_calls=num_calls)
            for node_id in dag.get_all_nodes():
                memory = dag.nodes[node_id]['memory']
                containers_cost_left[node_id] = calculate_cost(dag.nodes[node_id]['total_time'], memory, dag.nodes[node_id]["called_times"])
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
            all_logs = invoke_workflow_entry(entry_function_id, num_calls, f"workflow5_merged_right_{iteration}.pkl")
            update_dag_with_logs(dag, all_logs, num_calls=num_calls)
            for node_id in dag.get_all_nodes():
                memory = dag.nodes[node_id]['memory']
                containers_cost_right[node_id] = calculate_cost(dag.nodes[node_id]['total_time'], memory, dag.nodes[node_id]["called_times"])
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

# 融合镜像包路径
jar_paths = {
    '01': "./NestedWorkflow/workflow5/01/get-left-trip-tickets/build/libs/get-left-trip-tickets.jar",
    '03': "./NestedWorkflow/workflow5/03/get-route-by-routeid/build/libs/get-route-by-routeid.jar",
    '04': "./NestedWorkflow/workflow5/04/get-traintype-by-traintypeid/build/libs/get-traintype-by-traintypeid.jar",
    '07': "./NestedWorkflow/workflow5/07/query-for-station-id-by-station-name/build/libs/query-for-station-id-by-station-name.jar",
    '08': "./NestedWorkflow/workflow5/08/get-left-ticket-of-interval/build/libs/get-left-ticket-of-interval.jar",
    '09': "./NestedWorkflow/workflow5/09/get-route-by-tripid/build/libs/get-route-by-tripid.jar",
    '10': "./NestedWorkflow/workflow5/10/get-sold-tickets/build/libs/get-sold-tickets.jar",
    '11': "./NestedWorkflow/workflow5/11/get-traintype-by-tripid/build/libs/get-traintype-by-tripid.jar",
    '12': "./NestedWorkflow/workflow5/12/query-config-entity-by-config-name/build/libs/query-config-entity-by-config-name.jar"
}

# sudo /home/chenzebin/anaconda3/envs/chatglm/bin/python3 main.py
if __name__ == '__main__':
    # 读取JSON文件创建DAG对象
    with open('./workflows5.json', 'r') as f:
        json_data = json.load(f)
    dag = create_dag_from_json(json_data)
    '''循环融合直到所有函数都尝试过融合'''
    count = 0
    while True:
        # 检查DAG所有同步节点是否都被尝试融合
        break_flag = True
        for node_id, info in dag.nodes.items():
            # 如果 info["async"] 为 False 且 info["merged_try"] 中至少有一个值为 False
            # 则 break_flag 被设置为 False，并且立即跳出循环。
            if not info["async"] and any(value == False for value in info["merged_try"].values()):
                break_flag = False
                break
        if break_flag:
            break
        '''进行一次融合尝试'''
        print(f"尝试第{count+1}次融合")
        # 获取工作流的入口函数ID
        entry_function_id = get_entry_function_id(dag)
        # 初始化调用入口函数num_calls次
        update_calling_maps(dag)
        set_memory_limit_for_all_functions(dag)
        all_logs = invoke_workflow_entry(entry_function_id, 20, f"workflow5_origin_{count}.pkl")
        # 将日志信息加到DAG对应的函数下面
        update_dag_with_logs(dag, all_logs, num_calls=20)
        # 找出同步调用时间最长的函数和调用它的函数
        longest_sync_function_pair = find_longest_sync_functions(dag)
        merged_functions = []
        merged_functions.extend(convert_function_string(longest_sync_function_pair[0]))
        merged_functions.extend(convert_function_string(longest_sync_function_pair[1]))
        # 融合镜像(需要原始函数的列表)
        merged_image_name = merge_images(merged_functions, "workflow5", jar_paths)
        # 创建融合后的DAG(提供DAG的函数的列表)
        merged_dag = create_merged_dag(dag, longest_sync_function_pair, merged_image_name)
        # 获取工作流的入口函数ID
        entry_function_id = get_entry_function_id(merged_dag)
        # 调用入口函数num_calls次
        update_calling_maps(merged_dag)
        set_memory_limit_for_all_functions(merged_dag)
        all_logs = invoke_workflow_entry(entry_function_id, 20, f"workflow5_merged_{count}.pkl")
        # 将日志信息加到DAG对应的函数下面
        update_dag_with_logs(merged_dag, all_logs, num_calls=20)
        # 比较端到端Cost是否下降
        before_cost = 0
        after_cost = 0
        for node_id, info in dag.nodes.items():
            before_cost += calculate_cost(info["total_time"], info["memory"], info["called_times"])
        for node_id, info in merged_dag.nodes.items():
            after_cost += calculate_cost(info["total_time"], info["memory"], info["called_times"])
        if before_cost >= after_cost:
            dag = merged_dag
        else:
            for node_id, info in dag.nodes.items():
                if node_id==longest_sync_function_pair[1]:
                    info["merged_try"][longest_sync_function_pair[0]] = True
        count += 1
    
    # 对融合后的DAG内存做出修改
    gradient_descent_optimization_for_dag(dag, entry_function_id, memory_step=128, 
    max_iterations=5, threshold_count=1, slo_runtime=None, num_calls=20)