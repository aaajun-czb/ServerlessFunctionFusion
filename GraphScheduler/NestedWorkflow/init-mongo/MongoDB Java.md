> https://www.runoob.com/mongodb/mongodb-java.html
## MongoDB JDBC 驱动

JDBC: Java Database Connectivity

从[官网](https://www.mongodb.com/try/download/jdbc-driver)下载JDBC Driver，下载jar，比如`mongodb-jdbc-2.1.2-all.jar`。
注意不同版本之间的差异，可见[驱动文档](https://mongodb.github.io/mongo-java-driver/)。

其他版本存在运行问题，下载[mongo-java-driver-3.2.2.jar](https://repo1.maven.org/maven2/org/mongodb/mongo-java-driver/3.2.2/mongo-java-driver-3.2.2.jar "mongo-java-driver-3.2.2.jar")，可以正常运行。

```java
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class MongoDBJDBC{
   public static void main( String args[] ){
      try{   
       // 连接到 mongodb 服务
         MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
       
         // 连接到数据库
         MongoDatabase mongoDatabase = mongoClient.getDatabase("mycol");  
       System.out.println("Connect to database successfully");
        
      }catch(Exception e){
        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
     }
   }
}
```

将 MongoDB JDBC 启动包 `mongo-java-driver-3.2.2.jar` 放在本地目录下
```
your_project/
├── mongo-java-driver-3.2.2.jar
└── MongoDBJDBC.java
```

```shell
# 编译
javac -cp .:mongo-java-driver-3.2.2.jar MongoDBJDBC.java
# 运行
java -cp .:mongo-java-driver-3.2.2.jar MongoDBJDBC
```

