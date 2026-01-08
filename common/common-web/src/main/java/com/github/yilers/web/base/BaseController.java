package com.github.yilers.web.base;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.yilers.core.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.Serializable;
import java.util.List;

/**
 * base
 * @author zhanghui
 * @date 2023/8/2 15:42
 */

public abstract class BaseController<T> {

    @Resource
    protected HttpServletRequest request;

    @Resource
    protected HttpServletResponse response;

    protected abstract IService<T> getService();

    @Operation(summary = "通过id查询")
    @GetMapping("/findById/{id}")
    public Result<T> getById(@PathVariable Serializable id) {
        return Result.ok(getService().getById(id));
    }

    @Operation(summary = "查询所有")
    @GetMapping("/findAll")
    public Result<List<T>> findAll() {
        return Result.ok(getService().list());
    }

    @Operation(summary = "保存")
    @PostMapping("/save")
    public Result<?> save(@RequestBody T entity) {
        getService().save(entity);
        return Result.ok();
    }

    @Operation(summary = "根据id更新")
    @PostMapping("/updateById")
    public Result<?> updateById(@RequestBody T entity) {
        getService().updateById(entity);
        return Result.ok();
    }

//    @Operation(summary = "通过id删除")
//    @PostMapping("/deleteById/{id}")
//    public Result<?> deleteById(@PathVariable Serializable id) {
//        getService().removeById(id);
//        return Result.ok();
//    }

}
