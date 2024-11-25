## 基础信息
> https://www.runoob.com/mongodb/mongodb-tutorial.html

开源文档型数据库，使用类似 JSON 的文档模型存储数据。

| SQL 术语/概念   | MongoDB 术语/概念 | 解释/说明                   |
| ----------- | ------------- | ----------------------- |
| database    | database      | 数据库                     |
| table       | collection    | 数据库表/集合                 |
| row         | document      | 数据记录行/文档                |
| column      | field         | 数据字段/域                  |
| index       | index         | 索引                      |
| table joins |               | 表连接,MongoDB不支持          |
| primary key | primary key   | 主键,MongoDB自动将_id字段设置为主键 |

- **文档（Document）**：MongoDB 的基本数据单元，通常是一个 JSON-like 的结构，可以包含多种数据类型。
- **集合（Collection）**：类似于关系型数据库中的表，集合是一组文档的容器。在 MongoDB 中，一个集合中的文档不需要有一个固定的模式。
- **数据库（Database）**：包含一个或多个集合的 MongoDB 实例。
- **BSON**：Binary JSON 的缩写，是 MongoDB 用来存储和传输文档的二进制形式的 JSON。
