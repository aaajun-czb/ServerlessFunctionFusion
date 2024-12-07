from flask import Flask, request, jsonify
from ServerlessFunctionFusion.Gateway.container_manager_ver2 import ContainerManager

app = Flask(__name__)
container_manager = ContainerManager()

@app.route('/invoke', methods=['POST'])
def invoke_container():
    data = request.json
    container_name = data.get('container_name')
    jar_name = data.get('jar_name')
    input_data = data.get('data')
    calling_container = data.get('calling_container')  # 获取 calling_container 字段
    print(f"container_name:{container_name}, calling_container:{calling_container}")
    if not container_name:
        return jsonify({"error": "container_name is required"}), 400
    try:
        # 运行容器并获取输出
        output = container_manager.run_container(container_name, jar_name, 
                                                 input_data, calling_container)
        return jsonify(output)
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    
@app.route('/save_logs', methods=['POST'])
def save_logs():
    data = request.json
    filename = data.get('filename', 'container_logs.pkl')
    try:
        container_manager.save_logs_to_pickle(filename)
        return jsonify({"message": f"Logs saved to {filename}"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    
@app.route('/get_all_logs', methods=['GET'])
def get_all_logs():
    try:
        logs = container_manager.get_all_container_logs()
        return jsonify(logs)
    except Exception as e:
        return jsonify({"error": str(e)}), 500    
    
@app.route('/get_latest_logs', methods=['GET'])
def get_latest_logs():
    try:
        logs = container_manager.get_lastest_container_logs()
        return jsonify(logs)
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    
@app.route('/clear_logs', methods=['POST'])
def clear_logs():
    try:
        container_manager.clear_logs()
        return jsonify({"message": "Logs cleared successfully"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500
    
@app.route('/set_memory_limit', methods=['POST'])
def set_memory_limit():
    data = request.json
    image_name = data.get('image_name')
    memory_limit = data.get('memory_limit')
    if not image_name or not memory_limit:
        return jsonify({"error": "image_name and memory_limit are required"}), 400
    try:
        container_manager.set_image_memory_setting(image_name, memory_limit)
        return jsonify({"message": f"Memory limit set for {image_name}: {memory_limit} MB"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/set_environment_variable', methods=['POST'])
def set_environment_variable():
    data = request.json
    key = data.get('key')
    value = data.get('value')
    if not key or not value:
        return jsonify({"error": "key and value are required"}), 400
    try:
        container_manager.set_environment_variable(key, value)
        return jsonify({"message": f"Environment variable {key} set to {value}"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# sudo /home/chenzebin/anaconda3/envs/chatglm/bin/python3 api_gateway.py
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)