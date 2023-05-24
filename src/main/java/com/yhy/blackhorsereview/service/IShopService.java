package com.yhy.blackhorsereview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yhy
 * @since 2023-5-20
 */
public interface IShopService extends IService<Shop> {

    Result queryById(Long id);

    Result update(Shop shop);

    /**
     * 根据商铺类型分页查询商铺信息,并且根据经纬度排序
     * @param typeId 商店类型，0为吃的，1为KTV
     * @param current 页码，实现滚动查询
     * @param x x坐标
     * @param y y坐标
     * @return List<Shop>符合要求的商户信息
     */
    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
