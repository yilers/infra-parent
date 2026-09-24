package io.github.yilers.upm.handler;

import cn.hutool.v7.core.text.StrUtil;
import cn.hutool.v7.json.JSONObject;
import cn.hutool.v7.json.JSONUtil;

/**
 * 用户扩展字段处理工具。
 *
 * <p>修改单个配置时保留JSON中的其他字段，避免后续扩展用户配置时相互覆盖。</p>
 */
final class UserExpandHelper {

    private static final String INIT_PWD = "initPwd";

    private UserExpandHelper() {
    }

    /**
     * 设置用户当前是否仍在使用初始化密码。
     */
    static String setInitPwd(String expand, boolean initPwd) {
        JSONObject values = parse(expand);
        values.putValue(INIT_PWD, initPwd);
        return values.toString();
    }

    /**
     * 判断用户当前是否仍在使用初始化密码。
     */
    static boolean isInitPwd(String expand) {
        return Boolean.TRUE.equals(parse(expand).getBool(INIT_PWD));
    }

    private static JSONObject parse(String expand) {
        if (StrUtil.isBlank(expand)) {
            return new JSONObject();
        }
        try {
            return JSONUtil.parseObj(expand);
        } catch (Exception exception) {
            return new JSONObject();
        }
    }
}
