package com.yhy.blackhorsereview.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yhy.blackhorsereview.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.yhy.blackhorsereview.utils.RedisConstants.*;

@Component
@Slf4j
/**
 * 封装Redis工具类
 */
public class CacheClient {


    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造注入
     * @param stringRedisTemplate
     */
    public CacheClient(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 将仍以java对象序列化为json并且存储在String类型的key中，可以设置TTL过期时间
     * @param key 键值
     * @param value 对象
     * @param time TTL时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将仍以java对象序列化为json并且存储在String类型的key中，可以设置逻辑过期时间，用于处理缓存击穿问题
     * @param key 键值
     * @param value 对象
     * @param time 逻辑过期时间
     * @param unit 时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 根据指定的key查询缓存，并且反序列化为指定的类型，利用缓存空值的方式解决缓存穿透的问题
     * @param keyPrefix 键值的前缀
     * @param id 查询的id
     * @param type 查询的类型
     * @param dbFallBack Lambda函数
     * @param time 时间
     * @param unit 时间单位
     * @param <R> 返回类型
     * @param <ID> id类型
     * @return
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallBack,
                                    Long time, TimeUnit unit){
        // 根据id查找redis缓存
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        // 判断是否有缓存
        if (StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }

        // 判断命中的是否是空值
        if (json != null) {
            return null;
        }

        // 没有缓存则从数据库查，然后放到redis里
        R r = dbFallBack.apply(id);
        if (r == null) {
            // 将空值放入redis，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        this.set(key, r, time, unit);
        return r;
    }


    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 根据指定的key查询缓存，并且反序列化为指定的类型，利用逻辑过期的方式解决缓存穿透的问题
     * @param keyPrefix 键值的前缀
     * @param lockKeyPrefix 锁的前缀
     * @param id 查询的id
     * @param type 查询的类型
     * @param dbFallback Lambda函数
     * @param time 时间
     * @param unit 时间类型
     * @param <R> 返回类型
     * @param <ID> id类型
     * @return
     */
    public <R, ID>  R queryWithLogicExpire(String keyPrefix, String lockKeyPrefix, ID id, Class<R> type,
                                           Function<ID, R> dbFallback, Long time, TimeUnit unit){
        // 根据id查找redis缓存
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        // 判断是否有缓存
        if (StrUtil.isBlank(json)){
            return null;
        }
        // 命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean(((JSONObject) redisData.getData()), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 没过期,直接返回商品信息
            return r;
        }
        // 已过期，需要缓存重建
        // 获取互斥锁
        String lockKey = lockKeyPrefix + id;
        boolean isLock = tryLock(lockKey);
        // 判断是否获取锁成功
        if (isLock) {
            // 如果成功则开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R r1 = dbFallback.apply(id);
                    // 写入redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unLock(lockKey);
                }
            });
        }
        // 返回过期的店铺信息
        return r;
    }

    /**
     * 获取锁
     * @param key
     * @return
     */
    private boolean tryLock(String key){
        boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * @param key
     */
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }
}
