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
}
