# grok 构造器

在线访问地址 http://grokconstructor.appspot.com/

Grok 是一组可以使用的命名正则表达式 - 例如 logstash http://logstash.net/ -
解析日志文件。 GrokDiscovery http://grokdebug.herokuapp.com/ 可以通过建议常规来帮助您
表达式。 GrokConstructor 超越了这一点，找到了许多可能的正则表达式
通过使用 groks 模式和固定字符串匹配一整套日志文件行。 这可以自动完成
（仅对小东西的用途有限），或在增量过程中。

## 如何运行

### http://grokconstructor.appspot.com/

The best way is probably to use it on http://grokconstructor.appspot.com/ - 
there is also a good description, and you can use it on
some examples or for your own log lines you want to match.

### Appengine Devserver, started from maven

To run locally, build with
mvn clean install
and start with
mvn clean install appengine:run
. It runs on http://localhost:9090/ .

### Deploy as a WAR

If you want to run it on a system without internet connection or that has an application server, anyway,
you can also deploy the created target/GrokConstructor-*-SNAPSHOT.war e.g. on a Tomcat.

### Standalone Executable

java -jar GrokConstructor-0.1.0-SNAPSHOT-standalone.jar

runs an embedded Tomcat that makes it available at http://localhost:8080/ .
Please be aware that this creates a directory .extract in the current directory that contains the 
unpacked webapp. You can print additional arguments (such as ports, unpack location) with

java -jar GrokConstructor-0.1.0-SNAPSHOT-standalone.jar -h

### With Docker

If you don't have a JDK installation and maven installed on your server and don't want to create a standalone executable otherwise, you can also run the build and startup within a on-build docker container (courtesy of Timothy Van Heest http://turtlemonvh.github.io/). Please note that this container executes the maven build within the docker container and then starts the development server.
```
docker build -t grokconstructor .

docker run -d -p 8080:8080 grokconstructor

```
Alternatively, you can run it with docker-compose:

```
docker-compose up
```
