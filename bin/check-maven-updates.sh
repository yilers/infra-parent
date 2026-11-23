
#!/usr/bin/env bash

# 检查 Maven 依赖、父 POM 属性和插件版本更新
# 脚本位置：infra-parent/bin/check-maven-updates.sh
#
# 支持从任意目录执行：
#   ./bin/check-maven-updates.sh
#   /path/to/infra-parent/bin/check-maven-updates.sh
#
# 仅检查版本，不会修改 pom.xml

set -u

PLUGIN_VERSION="2.25.1"
PLUGIN="org.codehaus.mojo:versions-maven-plugin:${PLUGIN_VERSION}"

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

# 检查脚本是否位于项目 bin 目录
if [[ "$(basename "${SCRIPT_DIR}")" != "bin" ]]; then
    echo "ERROR: 脚本应放在项目根目录的 bin/ 目录下。" >&2
    echo "当前脚本目录：${SCRIPT_DIR}" >&2
    exit 1
fi

# 检查项目根目录是否存在 pom.xml
if [[ ! -f "${PROJECT_ROOT}/pom.xml" ]]; then
    echo "ERROR: 未找到项目根目录的 pom.xml。" >&2
    echo "推测的项目根目录：${PROJECT_ROOT}" >&2
    echo "请确认脚本位于 infra-parent/bin/ 目录下。" >&2
    exit 1
fi

# 优先使用 Maven Wrapper
if [[ -x "${PROJECT_ROOT}/mvnw" ]]; then
    MVN_CMD="${PROJECT_ROOT}/mvnw"
elif command -v mvn >/dev/null 2>&1; then
    MVN_CMD="mvn"
else
    echo "ERROR: 未找到 Maven 或 Maven Wrapper (mvnw)。" >&2
    exit 1
fi

# 进入项目根目录
cd "${PROJECT_ROOT}" || exit 1

FAILED=0

run_check() {
    local title="$1"
    local goal="$2"
    shift 2

    echo
    echo "============================================================"
    echo " ${title}"
    echo "============================================================"

    if "${MVN_CMD}" -B "${PLUGIN}:${goal}" "$@"; then
        echo "RESULT: ${title} 检查完成。"
    else
        echo "RESULT: ${title} 检查失败。" >&2
        FAILED=1
    fi
}

echo "项目目录：${PROJECT_ROOT}"
echo "Maven 命令：${MVN_CMD}"
echo "Versions Maven Plugin：${PLUGIN_VERSION}"
echo "说明：仅检查版本，不会修改 pom.xml。"

# 1. 检查依赖更新
#run_check \
#    "Maven 依赖版本检查" \
#    "display-dependency-updates"

# 2. 检查父 POM 和自定义属性版本更新
run_check \
    "Maven 属性版本检查" \
    "display-property-updates" \
    "-DincludeParent=true"

# 3. 检查 Maven 插件更新
#run_check \
#    "Maven 插件版本检查" \
#    "display-plugin-updates"

echo
echo "============================================================"

if [[ "${FAILED}" -eq 0 ]]; then
    echo "所有版本检查已完成。"
else
    echo "部分版本检查失败，请查看上方日志。"
fi

echo "============================================================"

exit "${FAILED}"
