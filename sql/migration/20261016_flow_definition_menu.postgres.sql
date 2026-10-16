-- 在流程定义前端上线后执行一次。此前已执行 permissions 增量的库只需执行本脚本。
-- 同步启用模板菜单与已复制到其他租户的菜单，不触碰租户自建菜单。
UPDATE upm_permission
SET usable = 1, version = version + 1
WHERE deleted = 0 AND ((tenant_id = 1 AND id = 197) OR source_id = 197);

UPDATE upm_permission
SET usable = 1, component = 'flow/definition/index', version = version + 1
WHERE deleted = 0 AND ((tenant_id = 1 AND id = 198) OR source_id = 198);
