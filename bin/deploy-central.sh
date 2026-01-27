#!/bin/bash

# 进入项目根目录（可选）
cd "$(dirname "$0")/.."

# 提示用户输入版本号
read -p "请输入版本号（如 1.0.4）: " revision

# 检查输入
if [[ -z "$revision" ]]; then
  echo "版本号不能为空，已退出。"
  exit 1
fi

# 确认执行
echo "即将执行: mvn clean deploy -Drevision=$revision"
read -p "是否确认执行？(y/n): " confirm

if [[ "$confirm" != "y" ]]; then
  echo "已取消部署。"
  exit 1
fi

# 执行部署命令
mvn clean deploy -Pcentral -Drevision="$revision"
