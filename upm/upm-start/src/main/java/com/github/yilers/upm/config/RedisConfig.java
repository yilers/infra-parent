//package com.github.yilers.upm.config;
//
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.RedisPassword;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import org.springframework.data.redis.support.collections.RedisProperties;
//
//import java.time.Duration;
//
//@Configuration
//public class RedisConfig {
//    @Autowired
//    private RedisProperties redisProperties;
//
//    @Bean
//    public LettuceConnectionFactory redisConnectionFactory() {
//        // Redis 连接配置
//        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//        config.setHostName(redisProperties.getHost());
//        config.setPort(redisProperties.getPort());
//        config.setPassword(RedisPassword.of(redisProperties.getPassword()));
//        config.setDatabase(redisProperties.getDatabase());
//
//        // Lettuce 客户端连接池配置
//        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
//        poolConfig.setMaxTotal(200);
//        poolConfig.setMaxIdle(10);
//        poolConfig.setMinIdle(3);
//        poolConfig.setMaxWait(Duration.ofSeconds(5));
//        poolConfig.setTestOnBorrow(true);
//        poolConfig.setTestWhileIdle(true);
//
//        // Lettuce 客户端配置
//        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
//                .commandTimeout(Duration.ofSeconds(5))
//                .shutdownTimeout(Duration.ofMillis(100))
//                .poolConfig(poolConfig)
//                .clientOptions(ClientOptions.builder()
//                        .autoReconnect(true)
//                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
//                        .build())
//                .build();
//
//        return new LettuceConnectionFactory(config, clientConfig);
//    }
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//
//        // key 使用 String 序列化
//        StringRedisSerializer stringSerializer = new StringRedisSerializer();
//        // value 使用 JSON 序列化
//        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
//
//        template.setKeySerializer(stringSerializer);
//        template.setHashKeySerializer(stringSerializer);
//        template.setValueSerializer(jsonSerializer);
//        template.setHashValueSerializer(jsonSerializer);
//
//        template.afterPropertiesSet();
//        return template;
//    }
//}
