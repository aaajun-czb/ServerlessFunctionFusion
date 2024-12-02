# trainticket workflows - Docker version
具备workflow1-5，具体见documents目录下介绍。

## 初始化工作
### mongodb部署
进入init-mongo目录。
```bash
sudo docker run --name some-mongo -d mongo:latest
./gradlew run
```
注意mongodb的ip地址，容器的ip不一定是172.17.0.1，比如我的就是172.17.0.3，自己看着修改src/.../repository里的ip地址。

初始化完验证一下。
```bash
sudo docker exec -it some-mongo mongosh
show dbs;
use ts;
show collections;
db.xxx.find()
```

### workflow1修改
将27号函数、44号函数的Proxy、Impl、Handler修改，具体来说，Proxy改造成一次执行返回结果，Impl修改MongoDB地址，Handler修改为直接请求API网关等等，总之改成执行一次容器返回结果的形式。
修改代码完成后，通过以下指令打包jar文件，并生成docker镜像。
```bash
chmod a+x ./gradlew
dos2unix ./gradlew
./gradlew clean build
sudo docker build -t 27 .
sudo docker build -t 44 .
```

手动使用curl测试网关功能。
```bash
curl -X POST http://localhost:5000/invoke -H "Content-Type: application/json" -d '{"container_name": "44", "jar_name": "get-stationid-list-by-name-list.jar", "data": ["nanjing", "shanghaihongqiao", "shanghai", "beijing", "shanghai", "beijing"]}'
curl -X POST http://localhost:5000/invoke -H "Content-Type: application/json" -d '{"container_name": "27", "jar_name": "query-orders-for-refresh.jar"}'
```

接下来，为了便于之后融合的工作，还需要再次修改代码架构，具体改造为：
调用下一个函数时，会先查看本地是否有jar包，若有本地调用，自己解析，若没有再调用http请求。

### workflow2修改
同理。
```bash
chmod a+x ./gradlew
dos2unix ./gradlew
./gradlew clean build
sudo docker build -t 23 .
sudo docker build -t 30 .
```

手动使用curl测试网关功能。
```bash
curl -X POST http://localhost:5000/invoke -H "Content-Type: application/json" -d '{"container_name": "23", "jar_name": "get-order-by-id.jar", "data": "d3c91694-d5b8-424c-9974-e14c89226e49"}'
curl -X POST http://localhost:5000/invoke -H "Content-Type: application/json" -d '{"container_name": "30", "jar_name": "calculate-refund.jar"}'
curl -X POST http://localhost:5000/invoke -H "Content-Type: application/json" -d '{"container_name": "30_23", "jar_name": "calculate-refund.jar"}'
```

### workflow3修改
同理。
```bash
chmod a+x ./gradlew
dos2unix ./gradlew
./gradlew clean build
sudo docker build -t 28 .
sudo docker build -t 29 .
sudo docker build -t 22 .
sudo docker build -t 22_23_29_28 -f Dockerfile_workflow3_22+23+29+28 .
```

手动使用curl测试网关功能。
```bash
curl -X POST http://localhost:5000/invoke -H "Content-Type: application/json" -d '{"container_name": "28", "jar_name": "drawback.jar", "data": ["test", "4396"]}'
curl -X POST http://localhost:5000/invoke -H "Content-Type: application/json" -d '{"container_name": "29", "jar_name": "save-order-info.jar"}'
curl -X POST http://localhost:5000/invoke -H "Content-Type: application/json" -d '{"container_name": "22", "jar_name": "cancel-ticket.jar", "data": ["d3c91694-d5b8-424c-9974-e14c89226e49", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"]}'
curl -X POST http://localhost:5000/invoke -H "Content-Type: application/json" -d '{"container_name": "22_23_29_28", "jar_name": "cancel-ticket.jar", "data": ["d3c91694-d5b8-424c-9974-e14c89226e49", "4d2a46c7-71cb-4cf1-b5bb-b68406d9da6f"]}'
```

## 网关+性能监控器
### 启动
在Gateway目录下运行网关服务器
```bash
sudo /home/chenzebin/anaconda3/envs/chatglm/bin/python3 api_gateway.py
```

## 图调度器
### 工作流输入结构
形如下面，async表示同步异步调用，slo代表到完成该函数时的slo要求。
```json
{
    "workflows": [
      {
        "name": "workflow1",
        "functions": [
          {
            "id": 27,
            "name": "query-orders-for-refresh",
            "dependencies": [],
            "async": false,
            "slo": 10000
          },
          {
            "id": 44,
            "name": "get-stationid-list-by-name-list",
            "dependencies": [27],
            "async": false,
            "slo": -1
          }
        ]
      }
    ]
  }
```

### 融合机制

### 调度器
