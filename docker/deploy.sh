#!/usr/bin/env bash
# Docker 部署入口：不自动修改数据库，不覆盖现有配置和密钥。
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
MODE_FILE="$SCRIPT_DIR/.deployment-mode"
cd "$SCRIPT_DIR"

usage() {
    printf '%s\n' '用法：./docker/deploy.sh init|start|update|stop|status|dependencies'
    printf '%s\n' 'init 准备配置并选择部署方式；start 启动；update 拉取并更新；stop 停止；status 查看状态。'
    printf '%s\n' 'dependencies 仅用于启动可选的 MySQL 和 Redis，数据库初始化仍需人工执行。'
}

require_docker() {
    if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
        printf '%s\n' '错误：需要安装 Docker 和 Docker Compose 插件。' >&2
        exit 1
    fi
}

copy_if_missing() {
    local source_file="$1" target_file="$2"
    if [[ -e "$target_file" ]]; then
        printf '保留已有配置：%s\n' "$target_file"
    else
        cp -n "$source_file" "$target_file"
        printf '已创建：%s\n' "$target_file"
    fi
}

init() {
    require_docker
    copy_if_missing .env.example .env
    chmod 600 .env
    copy_if_missing config/application-prod.yml.example config/application-prod.yml
    copy_if_missing config/redisson.yml.example config/redisson.yml
    if [[ ! -f "$MODE_FILE" ]]; then
        printf '%s\n' '请选择部署方式：' \
            '1. 已有数据库和 Redis（默认）' \
            '2. 使用 Compose 启动 MySQL 和 Redis' \
            '3. 已有数据库和 Redis，使用 compose.redis.yaml 自定义网络'
        read -r -p '请输入 1、2 或 3：' choice
        case "${choice:-1}" in
            1) printf '%s\n' external > "$MODE_FILE" ;;
            2) printf '%s\n' dependencies > "$MODE_FILE" ;;
            3)
                if [[ ! -f compose.redis.yaml ]]; then
                    printf '%s\n' '错误：请先创建 docker/compose.redis.yaml，配置现有 Redis 网络。' >&2
                    exit 1
                fi
                printf '%s\n' redis-network > "$MODE_FILE" ;;
            *) printf '%s\n' '错误：无效选项，请重新执行 init。' >&2; exit 1 ;;
        esac
    else
        printf '%s\n' '保留已有部署方式。如需切换，请先阅读 docker/README.md。'
    fi
    printf '\n%s\n' '配置已准备。请编辑 docker/.env，设置数据库、Redis 和三个独立主密钥。' \
        '数据库需手动初始化或执行增量 SQL；本脚本不会执行 SQL。' \
        '完成配置和数据库准备后，运行 ./docker/deploy.sh start。'
}

load_compose() {
    require_docker
    if [[ ! -f "$MODE_FILE" ]]; then
        printf '%s\n' '错误：请先运行 ./docker/deploy.sh init，选择部署方式。' >&2
        exit 1
    fi
    local mode
    IFS= read -r mode < "$MODE_FILE"
    COMPOSE=(docker compose -f compose.yaml)
    case "$mode" in
        external) ;;
        dependencies) COMPOSE+=(-f compose.dependencies.yaml) ;;
        redis-network)
            if [[ ! -f compose.redis.yaml ]]; then
                printf '%s\n' '错误：缺少 docker/compose.redis.yaml。' >&2; exit 1
            fi
            COMPOSE+=(-f compose.redis.yaml) ;;
        *) printf '%s\n' '错误：部署方式无效，请检查 docker/.deployment-mode。' >&2; exit 1 ;;
    esac
    if [[ ! -f .env || ! -f config/application-prod.yml || ! -f config/redisson.yml ]]; then
        printf '%s\n' '错误：部署配置不完整，请先运行 ./docker/deploy.sh init。' >&2
        exit 1
    fi
}

confirm_database() {
    local answer
    printf '%s\n' '请确认已阅读版本说明，已完成数据库备份及本版本所需 SQL；脚本不会执行 SQL。'
    read -r -p '确认继续部署？[y/N] ' answer
    case "$answer" in y|Y|yes|YES) ;; *) printf '%s\n' '已取消，未启动或更新容器。'; exit 1 ;; esac
}

start() {
    load_compose
    confirm_database
    "${COMPOSE[@]}" config --quiet
    # 只更新应用，不重建或更改已运行的 MySQL、Redis 等依赖。
    "${COMPOSE[@]}" up -d --build --no-deps infra
    "${COMPOSE[@]}" ps
    printf '%s\n' '容器已提交启动，请检查健康状态及日志：./docker/deploy.sh status'
}

update() {
    load_compose
    if [[ -n "$(git -C "$REPO_DIR" status --porcelain --untracked-files=no)" ]]; then
        printf '%s\n' '错误：存在已跟踪文件的本地改动，请先检查并处理；脚本不会覆盖。' >&2
        exit 1
    fi
    if ! git -C "$REPO_DIR" symbolic-ref --quiet HEAD >/dev/null; then
        printf '%s\n' '错误：当前处于游离 HEAD，请先切换到要更新的分支。' >&2
        exit 1
    fi
    printf '%s\n' '拉取当前分支的上游更新（仅允许快进）。'
    # 拉取后重新执行最新脚本，避免 Git 更新正在运行的脚本内容。
    git -C "$REPO_DIR" pull --ff-only && git -C "$REPO_DIR" log -1 --oneline && exec bash "$SCRIPT_DIR/deploy.sh" start
}

case "${1:-}" in
    init) init ;;
    start) start ;;
    update) update ;;
    stop) load_compose; "${COMPOSE[@]}" stop infra ;;
    status) load_compose; "${COMPOSE[@]}" ps; "${COMPOSE[@]}" logs --tail=50 infra ;;
    dependencies)
        load_compose
        if [[ "$(< "$MODE_FILE")" != dependencies ]]; then
            printf '%s\n' '错误：当前不是 Compose 依赖容器模式。' >&2; exit 1
        fi
        "${COMPOSE[@]}" config --quiet
        "${COMPOSE[@]}" up -d mysql redis
        "${COMPOSE[@]}" ps mysql redis ;;
    *) usage; exit 1 ;;
esac
