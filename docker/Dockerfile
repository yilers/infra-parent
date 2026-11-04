# 从仓库根目录构建：docker build -f docker/Dockerfile -t infra-parent:local .
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
# .dockerignore 排除本地开发配置与密钥，prod profile 不引入开发诊断面板。
RUN mvn -B -Pprod -pl infra-start -am package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true \
    && find infra-start/target -maxdepth 1 -name 'infra-start-*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -exec cp {} /tmp/infra.jar \; \
    && test -f /tmp/infra.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
# 命名数据卷首次挂载时会继承此目录的所有权。
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 infra \
    && useradd --system --uid 10001 --gid infra --home-dir /app infra \
    && mkdir -p /app/log \
    && chown infra:infra /app/log
COPY --from=build --chown=infra:infra /tmp/infra.jar /app/infra.jar
USER infra
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "/app/infra.jar"]
