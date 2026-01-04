#!/bin/bash
# 项目根目录（脚本所在目录的上一级目录）
BASE_DIR=$(cd $(dirname $0)/..; pwd)
# 项目jar包名称（替换为你的实际项目名-版本号）
JAR_NAME=myApp-1.0.0.jar
# 配置文件目录
CONF_DIR=${BASE_DIR}/conf
# 依赖包目录
LIB_DIR=${BASE_DIR}/lib
# JVM启动参数
JVM_OPTS="-Xms512m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"

# 调试：打印关键路径，确认是否正确
echo "项目根目录：${BASE_DIR}"
echo "依赖目录：${LIB_DIR}"
echo "项目jar包：${LIB_DIR}/${JAR_NAME}"

# 关键：构建classpath，包含lib目录下所有jar包（含项目自身jar）
CLASSPATH="${LIB_DIR}/*"

# 检查jar包是否存在
if [ ! -f "${LIB_DIR}/${JAR_NAME}" ]; then
    echo "错误：项目jar包 ${LIB_DIR}/${JAR_NAME} 不存在！"
    exit 1
fi

# 检查启动类对应的字节码是否在jar包中（可选，手动验证）
echo "正在验证项目jar包中是否包含启动类..."
jar -tf "${LIB_DIR}/${JAR_NAME}" | grep "com/example/myApp/MyAppApplication.class"
if [ $? -ne 0 ]; then
    echo "警告：项目jar包中未找到启动类 com.example.myApp.MyAppApplication 的字节码！"
fi

# 启动Spring Boot项目（指定classpath + 主启动类）
echo "正在启动 ${JAR_NAME} ..."
nohup java ${JVM_OPTS} \
  -cp "${CLASSPATH}" \
  -Dspring.config.location=${CONF_DIR}/ \
  com.example.myApp.MyAppApplication \
  > ${BASE_DIR}/nohup.out 2>&1 &

# 打印启动日志
echo "启动命令已执行，日志文件：${BASE_DIR}/nohup.out"
sleep 3
tail -n 20 ${BASE_DIR}/nohup.out