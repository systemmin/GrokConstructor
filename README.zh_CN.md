# grok 构造器

在线访问地址 http://grokconstructor.appspot.com/

Grok 是一组可以使用的命名正则表达式 - 例如 logstash http://logstash.net/ -
解析日志文件。 GrokDiscovery http://grokdebug.herokuapp.com/ 可以通过建议常规来帮助您
表达式。 GrokConstructor 超越了这一点，找到了许多可能的正则表达式
通过使用 groks 模式和固定字符串匹配一整套日志文件行。 这可以自动完成
（仅对小东西的用途有限），或在增量过程中。

## 如何运行

### http://grokconstructor.appspot.com/

最好的方法可能是在 http://grokconstructor.appspot.com/ 上使用它 -
还有一个很好的描述，你可以使用它
一些示例或您想要匹配的自己的日志行。

### maven启动

本地运行
```shell
mvn clean install
```
```shell
mvn clean install appengine:run
```
访问 http://localhost:9090/ .

### WAR 部署

如果你想在没有互联网连接或有应用服务器的系统上运行它，无论如何，
您还可以在Tomcat上部署创建的target/GrokConstructor-*-SNAPSHOT.war。

### JAR 运行

```shell
java -jar GrokConstructor-0.1.0-SNAPSHOT-standalone.jar
```

运行嵌入式Tomcat，使其在http://localhost:8080/ .
请注意，这将在包含
解包的webapp。您可以使用

```shell
java -jar GrokConstructor-0.1.0-SNAPSHOT-standalone.jar -h
```


### Docker 部署
如果您的服务器上没有安装JDK和maven，并且不想创建独立的可执行文件，那么您也可以在内置docker容器中运行构建和启动（由Timothy Van Heest提供http://turtlemonvh.github.io/). 请注意，这个容器在docker容器中执行maven构建，然后启动开发服务器。
```
docker build -t grokconstructor .

docker run -d -p 8080:8080 grokconstructor

```
或者，您可以使用docker compose运行它：

```
docker-compose up
```
