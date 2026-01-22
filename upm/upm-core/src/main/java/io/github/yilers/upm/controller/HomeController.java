package io.github.yilers.upm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.v7.crypto.SecureUtil;
import cn.hutool.v7.crypto.digest.BCrypt;
import cn.hutool.v7.http.server.servlet.ServletUtil;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.github.yilers.web.util.Ip2RegionUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.TimeZone;


@Slf4j
@RestController
@RequestMapping("/")
@Tag(name = "测试")
@ApiSupport(order = 1, author = "张辉")
public class HomeController {

    @SaIgnore
    @GetMapping("/")
    public String home(HttpServletRequest request) {
        String format = StrUtil.format("协议:{} 请求ip:{} 服务器时间:{}", request.getProtocol(), ServletUtil.getClientIP(request), DateUtil.now());
        String region = Ip2RegionUtil.region(ServletUtil.getClientIP(request));
        log.info(format);
        log.info("ip所属区域:{}", region);
        log.info("Default ZoneId = {}", ZoneId.systemDefault());
        log.info("Default TimeZone = {}", TimeZone.getDefault().getID());
        return format;
    }

//    @SaIgnore
    @GetMapping("/pwd")
    @Operation(summary = "获取密码")
    @SaCheckRole("admin")
    public String pwd(String pwd) {
        String md5 = SecureUtil.md5(pwd);
        String hashpw = BCrypt.hashpw(md5);
        return md5 + " | " + hashpw;
    }

}
