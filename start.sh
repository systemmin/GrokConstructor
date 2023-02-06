#!/bin/bash

echo This is just a simple start script. Feel free to modify this according to your needs.
echo "这只是一个简单的开始脚本。请根据您的需要随意修改。"

# we build this, so this works immediately - in a real start script you obviously wouldn't do that.
# 我们构建了这个，所以它可以立即工作-在真正的启动脚本中，你显然不会这样做。
mvn clean install

# put temporary stuff into target - in a real start script you probably wouldn't do that either. :-)
# 把临时的东西放到目标中-在真正的开始脚本中，你可能也不会这么做
java -jar target/GrokConstructor-0.1.0-SNAPSHOT-standalone.jar -resetExtract -extractDirectory target/.extract -httpPort 8080 2>&1 | tee target/standalone.log

