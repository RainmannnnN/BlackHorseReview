package com.yhy.blackhorsereview.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.entity.Shop;
import com.yhy.blackhorsereview.mapper.ShopMapper;
import com.yhy.blackhorsereview.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.yhy.blackhorsereview.utils.RedisConstants.CACHE_SHOP_KEY;

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

    @Override
    public Result queryById(Long id) {
        // 根据id查找redis缓存
        String key = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 判断是否有缓存
        if (StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 没有缓存则从数据库查，然后放到redis里
        Shop shop = getById(id);
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop));
        return Result.ok(shop);
    }
}
