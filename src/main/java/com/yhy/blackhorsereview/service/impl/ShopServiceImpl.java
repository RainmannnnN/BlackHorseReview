package com.yhy.blackhorsereview.service.impl;

import cn.hutool.core.lang.hash.Hash;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.entity.Shop;
import com.yhy.blackhorsereview.mapper.ShopMapper;
import com.yhy.blackhorsereview.service.IShopService;
import com.yhy.blackhorsereview.utils.CacheClient;
import com.yhy.blackhorsereview.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.yhy.blackhorsereview.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yhy
 * @since 2023-5-20
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;


    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        // Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 用互斥锁解决缓存击穿
        // Shop shop = queryWithMuteX(id);

        // 逻辑过期解决缓存击穿
        Shop shop = cacheClient
                .queryWithLogicExpire(CACHE_SHOP_KEY, LOCK_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }


    /**
     * 用互斥锁解决缓存击穿
     * @param id
     * @return
     */
    public Shop queryWithMuteX(Long id) {
        // 根据id查找redis缓存
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 判断是否有缓存
        if (StrUtil.isNotBlank(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 判断命中的是否是空值
        if (shopJson != null) {
            return null;
        }

        // 实现缓存重建
        // 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 判断是否获取成功
            if (!isLock) {
                // 休眠并且重试
                Thread.sleep(50);
                return queryWithMuteX(id);
            }

            // 没有缓存则从数据库查，然后放到redis里
            shop = getById(id);
            // 模拟重建的延迟
            Thread.sleep(200);
            if (shop == null) {
                // 将空值放入redis，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 释放互斥锁
            unLock(lockKey);
        }

        return shop;
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

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 先更新数据库，在删除缓存
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 由于x，y是可选的，所以先判断要不要根据坐标查询
        if(x == null || y == null){
            // 根据类型分页查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        // 计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        // 查询redis，按照距离排序，分页
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(x, y),
                new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );
        // 解析出id
        if (results == null) {
            return Result.ok();
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
        // 这里要判断content的大小是不是小于from，如果小于那么跳过以后就没有商店信息了，就会报错
        if(content.size() <= from){
            return Result.ok(Collections.emptyList());
        }

        // 截取从from到end的部分
        List<Long> ids = new ArrayList<>(content.size());
        HashMap<String, Distance> distanceMap = new HashMap<>(content.size());
        content.stream().skip(from).forEach(r -> {
            // 获取店铺id
            String shopIdStr = r.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 获取距离
            Distance distance = r.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 根据id查询shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("order by field(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }


        return Result.ok(shops);
    }

    /**
    * 存储店铺信息逻辑过期时间的函数
    * @param id
    * @param expireSeconds
    */
    /*public void saveShop2Redis(Long id, Long expireSeconds){
        Shop shop = getById(id);
        // 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }*/
}
