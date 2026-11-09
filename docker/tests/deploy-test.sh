#!/usr/bin/env bash
# 使用模拟 Docker 验证部署入口，不连接或修改真实容器。
set -euo pipefail
source_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
test_dir="$(mktemp -d "${TMPDIR:-/tmp}/infra-deploy-test.XXXXXX")"
trap 'rm -rf -- "$test_dir"' EXIT
mkdir -p "$test_dir/docker/config"
cp "$source_dir/deploy.sh" "$source_dir/.env.example" "$source_dir/compose.yaml" \
    "$source_dir/compose.dependencies.yaml" "$test_dir/docker/"
cp "$source_dir/config/"*.example "$test_dir/docker/config/"
printf '%s\n' '原有密钥不应被覆盖' > "$test_dir/docker/.env"
printf '%s\n' 'networks: {}' > "$test_dir/docker/compose.redis.yaml"

docker() {
    printf '%s\n' "$*" >> "$DEPLOY_TEST_CALLS"
    return 0
}
export -f docker
export DEPLOY_TEST_CALLS="$test_dir/docker-calls"
printf '3\n' | bash "$test_dir/docker/deploy.sh" init >/dev/null
[[ "$(< "$test_dir/docker/.env")" == '原有密钥不应被覆盖' ]]
[[ "$(< "$test_dir/docker/.deployment-mode")" == redis-network ]]
printf 'y\n' | bash "$test_dir/docker/deploy.sh" start >/dev/null
grep -Fq -- '-f compose.redis.yaml up -d --build --no-deps infra' "$DEPLOY_TEST_CALLS"
bash "$test_dir/docker/deploy.sh" stop >/dev/null
grep -Fq -- '-f compose.redis.yaml stop infra' "$DEPLOY_TEST_CALLS"
printf '%s\n' 'Docker 部署脚本模拟测试通过。'
