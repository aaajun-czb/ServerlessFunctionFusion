import os
import subprocess

class DockerImageBuilder:
    def __init__(self, base_image="openjdk:8-jre-slim", working_dir="/app"):
        self.base_image = base_image
        self.working_dir = working_dir

    def create_dockerfile(self, function_ids, workflow_name, jar_paths):
        dockerfile_content = [
            f"FROM {self.base_image}",
            f"WORKDIR {self.working_dir}",
        ]

        for function_id in function_ids:
            jar_path = jar_paths[function_id]
            jar_name = os.path.basename(jar_path)
            dockerfile_content.append(f"COPY {jar_path} {self.working_dir}/{jar_name}")

        dockerfile_path = f"Dockerfile_{workflow_name}_{'+'.join(map(str, function_ids))}"
        with open(dockerfile_path, 'w') as dockerfile:
            dockerfile.write("\n".join(dockerfile_content))

        return dockerfile_path

    def build_docker_image(self, dockerfile_path, image_name):
        try:
            subprocess.run(["docker", "build", "-t", image_name, "-f", dockerfile_path, "."], check=True)
            print(f"Successfully built Docker image: {image_name}")
        except subprocess.CalledProcessError as e:
            print(f"Failed to build Docker image: {e}")

# 示例使用
# sudo /home/chenzebin/anaconda3/envs/chatglm/bin/python3 merger.py
if __name__ == "__main__":
    # 假设我们有以下路径映射
    # 注意Dockerfile不能读取上级目录
    jar_paths = {
        27: "./NestedWorkflow/workflow1/27/query-orders-for-refresh/build/libs/query-orders-for-refresh.jar",
        44: "./NestedWorkflow/workflow1/44/get-stationid-list-by-name-list/build/libs/get-stationid-list-by-name-list.jar"
    }
    # 创建DockerImageBuilder实例
    builder = DockerImageBuilder()
    # 创建Dockerfile
    dockerfile_path = builder.create_dockerfile([27, 44], "workflow1", jar_paths)
    # 构建Docker镜像
    image_name = "27_44"
    builder.build_docker_image(dockerfile_path, image_name)