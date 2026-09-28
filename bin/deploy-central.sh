#!/bin/bash

# 进入项目根目录（可选）
cd "$(dirname "$0")/.."

# 读取父 POM 中定义的版本号
revision=$(mvn help:evaluate -Dexpression=revision -q -DforceStdout)
if [[ -z "$revision" || "$revision" == *'${'* ]]; then
  echo "无法从 pom.xml 读取 revision，已退出。"
  exit 1
fi

# 中央仓库不接受 SNAPSHOT 版本
if [[ "$revision" == *-SNAPSHOT ]]; then
  echo "当前版本 $revision 为 SNAPSHOT，无法发布到中央仓库。"
  exit 1
fi

# 确认执行
echo "当前版本: $revision"
echo "即将执行: mvn clean deploy -Pcentral -Dgpg.keyname=223F63D22AE99F1E"
read -p "是否确认执行？(y/n): " confirm

if [[ "$confirm" != "y" ]]; then
  echo "已取消部署。"
  exit 1
fi

# 执行部署命令
mvn clean deploy -Pcentral -Dgpg.keyname=223F63D22AE99F1E
