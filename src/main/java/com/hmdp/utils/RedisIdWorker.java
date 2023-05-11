package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * redis 全局唯一ID 生成器
 * 全局id结构为 0 + （31位）时间戳 + id值
 * @autor larzhang
 */

@Component
public class RedisIdWorker {

    /**
     * 开始时间戳
     * */

    //这是2022年开始的时间
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate =stringRedisTemplate;
    }


    //keyPrefix为业务前缀
    public long nextId(String keyPrefix){

        //生成时间戳

        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        //生成序列号

       String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
       long count = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+date);

       //这里使用位运算 来拼接 lang//拼接并返回

        return timestamp << COUNT_BITS | count;
    }
}
