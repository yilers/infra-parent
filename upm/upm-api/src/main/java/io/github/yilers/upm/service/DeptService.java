package io.github.yilers.upm.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.extension.service.IService;
import io.github.yilers.upm.entity.Dept;

import java.util.List;

public interface DeptService extends IService<Dept> {

    Dept findByDeptCode(String deptCode);

    List<Dept> findByParentId(Long parentId,  Integer usable);

    List<Dept> findByIdList(List<Long> idList);

    List<Dept> findChildById(Long deptId);

    @Cached(name = "dept:", key = "#deptId", cacheType = CacheType.REMOTE, expire = 600)
    default Dept findById(Long deptId) {
        return getById(deptId);
    }

    List<Dept> findAllByByUsable(Integer usable);

}
