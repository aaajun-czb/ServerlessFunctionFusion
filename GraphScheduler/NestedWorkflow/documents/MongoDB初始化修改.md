将`entity`以及`repository`目录下的内容复制过来。

将`repository/OrderRepositoryImpl.java`中的mongoClient地址进行修改。
```java
MongoClient mongoClient = MongoClients.create("mongodb://172.17.0.1:27017");
```

在`Handler.java`中增加对应的类实例以及初始化函数。


运行`./gradlew run`即可运行。