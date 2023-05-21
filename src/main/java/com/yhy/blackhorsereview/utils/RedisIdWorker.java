package com.yhy.blackhorsereview.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


/**
 * 在redis中生成唯一的id
 */
@Component
public class RedisIdWorker {

    /**
     * 开始的时间戳 2023.5.21
     */
    public static final Long BEGIN_TIMESTAMP = 1684627200L;

    /**
     * 序列号的位数
     */
    public static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker (StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成下一个业务id
     * @param keyPrefix 业务的前缀
     * @return 返回Long类型的id
     */
    public long nextId(String keyPrefix){
        // 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 生成序列号
        // 获取当天日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        // 拼接并返回
        // 时间戳左移32位之后拼接count
        return timestamp << COUNT_BITS | count;
    }

}
