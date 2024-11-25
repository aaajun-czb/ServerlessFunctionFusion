> https://github.com/FudanSELab/serverless-trainticket


需要至少两个Host，一个用来当作NFS的服务端，其他node作为NFS的客服端。

serverless函数之间通过基于openfaas的url启动。

workflow通信方法改动：
- 方法1：
	部署阶段：
	a. 通过一个controller将所有workflow中的函数启动，收集function name和docker url的对应关系
	b. 将对应关系通过url发给所有docker。
	启动阶段：
	c. controller按照一定频率发送url请求，启动workflow。第一个函数接收url后执行，并通过url启动后续docker。
	d. 最后返回结果给controller。
- 方法2：
	将所有启动信息通过一个外部存储来接收和发送。

NFS的改动：
- 方法1：挂载同一个路径作为共享文件系统
- 方法2：额外使用一个Host作为NFS的服务端，其他作为客户端

## 简介

本项目使用开源函数计算框架 OpenFaaS、基于 Serverless 架构提取并改造开源微服务系统 TrainTicket 中高并发的订票业务，部署并运行在 Kubernetes 集群中。

并不包含原项目中的所有41个微服务。

![[attachments/system_architecture.png]]

应该更类似初始文章中的24个微服务架构
![[attachments/Pasted image 20240529221429.png]]

secuirty相关的内容较少，可以考虑提取相关内容。

## 函数调用关系

1. 函数编号&函数调用关系确定
   根据函数中的`String function09_URI = "gateway.openfaas:8080/function/get-route-by-tripid.openfaas-fn";`来给函数编号以及调用关系。
   技巧：在vscode中搜索`funciton.._URI`，开启正则来筛选，不用每个函数点开去看；
2. 绘制关系图像
   使用GPT和C++来处理数据。并使用https://visjs.org/来绘制网络图。

![[attachments/ce08ffbb377595e90b9a73a64cf41870.png]]![[attachments/fbf5e203853775e10d5c8cf750f7c0ce.png]]


### 消息传递方式

#### service

`service`目录下的方法中使用`okhttp3.Request`将消息打包发送给下游应用。使用`okhttp3.Response`接收返回值，之后提取body转换成string后，再封装成`mResponse`作为返回值。
```java
okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("http://" + function44_URI)
                    .post(body)
                    .build();
okhttp3.Response response = client.newCall(request).execute();
ret = response.body().string();
mResponse mRes = JsonUtils.json2Object(ret, mResponse.class);
```

#### handler

`Handler`中接收`com.openfaas.model.IRequest`，返回`com.openfaas.model.IResponse`。
内部将`Handler`的输入`IRequest`的Body从json转换为对应的obj之后作为参数使用；函数的返回值`mResponse`的obj转换为json，作为`Handler`输出`IResponse`的Body。

实际上就是封装了一下。

## debug

### Gradle
project中没有`gradlew`，因此本地使用`build.gradle`中提示的`gradle-4.8.1`来生成`gradlew wrapper`。

使用`gradle wrapper`进行构建时，会因为找不到`model`project出错。
```groovy
// build.gradle
dependencies {
	compile project(':model')
}
```
替换为以下依赖即可：
```
compile 'com.openfaas:model:0.1.1'
```




