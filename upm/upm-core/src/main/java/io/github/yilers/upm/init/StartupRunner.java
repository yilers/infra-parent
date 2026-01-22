package io.github.yilers.upm.init;

import cn.hutool.v7.extra.spring.SpringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * 服务启动后干的事
 *
 * @author zhanghui
 * @since 2024/8/16 上午9:26
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupRunner implements CommandLineRunner {

    @Value("${app.version}")
    private String version;

    private void printBanner() {
        String banner = "\n" +
                "  _    _ _____  __  __ \n" +
                " | |  | |  __ \\|  \\/  |\n" +
                " | |  | | |__) | \\  / |\n" +
                " | |  | |  ___/| |\\/| |\n" +
                " | |__| | |    | |  | |\n" +
                "  \\____/|_|    |_|  |_|\n"
                + "\r\n" + version+ "\r\n";;
        log.info(banner);
    }

    @Override
    @SneakyThrows
    public void run(String... args) {
        log.info("服务端启动完成");
        printBanner();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = SpringUtil.getApplicationContext().getEnvironment().getProperty("server.port");
        log.info("\n----------------------------------------------------------\n\t" +
                "Application is running! Access URLs:\n\t" +
                "Local: \t\thttp://localhost:" + port + "/\n\t" +
                "External: \thttp://" + ip + ":" + port + "/\n\t" +
                "Swagger文档: \thttp://" + ip + ":" + port + "/doc.html\n" +
                "----------------------------------------------------------");
    }
}
