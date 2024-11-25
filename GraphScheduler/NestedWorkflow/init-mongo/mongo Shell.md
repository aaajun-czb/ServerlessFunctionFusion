> https://docs.mongoing.com/the-mongo-shell/mongo-shell-quick-reference

MongoDB Shell 是 MongoDB 自带的交互式 Javascript shell，用来对 MongoDB 进行操作和管理的交互式环境。

## 配置

> https://www.mongodb.com/docs/mongodb-shell/install/

```shell
# 1. Import the public key used by the package management system.
wget -qO- https://www.mongodb.org/static/pgp/server-7.0.asc | sudo tee /etc/apt/trusted.gpg.d/server-7.0.asc
# 2. create a list file for MongoDB
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
# 3. reload package database
sudo apt-get update
# 4. install 
sudo apt-get install -y mongodb-mongosh
# 5. confirm
mongosh --version
```

## 指令交互

`mongosh`默认会链接到 test 文档（数据库）。

### 命令助手

| 命令               | 描述           |
| ---------------- | ------------ |
| show dbs         | 显示所有数据库      |
| use `db`         | 切换当前数据库到`db` |
| show collections | 显示当前数据库的所有集合 |

### 基本JavaScript操作

| 命令                                        | 描述                             |
| ----------------------------------------- | ------------------------------ |
| db.`collection`.find()                    | 显示当前数据库的名为`collection`的集合的所有文档 |
| db.`collection`.find().count()            | 显示当前数据库的名为`collection`的集合的文档数量 |
| db.`collection`.insertOne({'name':'jlx'}) | 插入一个文档，`collection`不存在时会新建     |
| db.`collection`.deleteOne({'name':'jlx'}) | 删除一个文档                         |
| db.`collection`.drop()                    | 完全删除集合                         |
| db.dropDatabase()                         | 删除当前数据库                        |
