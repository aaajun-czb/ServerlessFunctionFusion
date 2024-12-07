import pickle
import logging
import json

# 配置日志记录器
logging.basicConfig(filename='output.log', level=logging.INFO, format='%(message)s')
# 指定要打开的 .pkl 文件路径
pkl_file_path = './data/workflow5/algorithm/workflow5_origin_0.pkl'

try:
    # 打开并加载 .pkl 文件
    with open(pkl_file_path, 'rb') as f:
        data = pickle.load(f)
    
    # 将数据格式化为 JSON 字符串并记录到日志文件中
    formatted_data = json.dumps(data, indent=4)
    logging.info(formatted_data)
    
    print("数据已成功加载并保存到 output.log 文件中。")

except Exception as e:
    # 如果出现错误，记录错误信息
    logging.error(f"加载 .pkl 文件时出错: {e}")
    print(f"加载 .pkl 文件时出错: {e}")