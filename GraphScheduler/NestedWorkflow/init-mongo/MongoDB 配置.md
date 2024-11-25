
## Ubuntu
> https://www.runoob.com/mongodb/mongodb-linux-install.html
> Use this: https://www.mongodb.com/docs/manual/tutorial/install-mongodb-on-ubuntu-tarball/
> https://www.mongodb.com/zh-cn/docs/manual/tutorial/install-mongodb-on-ubuntu/

### 1. 安装相关依赖

```shell
sudo apt-get install libcurl4 libgssapi-krb5-2 libldap-2.5-0 libwrap0 libsasl2-2 libsasl2-modules libsasl2-modules-gssapi-mit openssl liblzma5
```

### 2. MongoDB 源码安装

MongoDB 源码下载地址：[https://www.mongodb.com/try/download/community](https://www.mongodb.com/try/download/community)
下载对应版本的tgz安装包。

```shell
 # 下载
tar -zxvf mongodb-linux-x86_64-ubuntu2204-7.0.11.tgz
 # 将压缩包拷贝到制定目录
sudo mv mongodb-linux-x86_64-ubuntu2204-7.0.11 /usr/local/mongodb7
```

将`/usr/local/mongodb7/bin`放入PATH中：
- [[../../../操作系统相关/Ubuntu/Ubuntu#修改环境变量PATH|Ubuntu 配置PATH]]

### 3. 创建数据库目录

默认情况下 MongoDB 启动后会初始化以下两个目录：
- 数据存储目录：`/var/lib/mongodb`
- 日志文件目录：`/var/log/mongodb`

启动前可以先创建这两个目录并设置当前用户有读写权限：
```shell
sudo mkdir -p /var/lib/mongo
sudo mkdir -p /var/log/mongodb
sudo chown `whoami` /var/lib/mongo     # 设置权限
sudo chown `whoami` /var/log/mongodb   # 设置权限
```

### 4. 启动MongoDB

启动MongoDB服务：
```shell
mongod --dbpath /var/lib/mongo --logpath /var/log/mongodb/mongod.log --fork
```

==若docker也要访问，需要设置ip为`0.0.0.0`==

```shell
mongod --bind_ip 0.0.0.0 --dbpath /var/lib/mongo --logpath /var/log/mongodb/mongod.log --fork
```

可选参数：
- `--bind_ip`: 监听的ip，默认是`localhost`
- `port`：监听的端口，默认是`27017`


查看`/var/log/mongodb/mongod.log`日志，来确认启动:
```
[initandlisten] waiting for connections on port 27017
```

### 5. 交互

安装[[mongo Shell#配置|mongosh]](MongoDB Shell)，在terminal中输入`mongosh`可进入交互CLI，输入[[mongo Shell#指令交互|指令]]进行操作。

```shell
mongosh
db.jlx.insert({x:10})
db.jlx.find()
```

### 6. 停止MongoDB
停止MongoDB:
```shell
mongod --dbpath /var/lib/mongo --logpath /var/log/mongodb/mongod.log --shutdown
```