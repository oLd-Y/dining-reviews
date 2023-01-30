package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    RedissonClient redissonClient() {
        // redis的配置
        Config config = new Config();
        // redis服务器地址
        config.useSingleServer().setAddress("redis://43.142.151.93:6379").setPassword("123456");
        // 创建RedissonClient对象并返回
        return Redisson.create(config);
    }
    @Bean
    RedissonClient redissonClient2() {
        // redis的配置
        Config config = new Config();
        // redis服务器地址
        config.useSingleServer().setAddress("redis://43.142.151.93:6380").setPassword("123456");
        // 创建RedissonClient对象并返回
        return Redisson.create(config);
    }
    @Bean
    RedissonClient redissonClient3() {
        // redis的配置
        Config config = new Config();
        // redis服务器地址
        config.useSingleServer().setAddress("redis://43.142.151.93:6381").setPassword("123456");
        // 创建RedissonClient对象并返回
        return Redisson.create(config);
    }

}
