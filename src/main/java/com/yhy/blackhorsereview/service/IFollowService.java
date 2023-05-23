package com.yhy.blackhorsereview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.entity.Follow;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yhy
 * @since 2023-5-23
 */
public interface IFollowService extends IService<Follow> {
    /**
     * 关注或者取关
     * @param id 用户id
     * @param isFollow 是否有关注
     * @return
     */
    Result follow(Long id, boolean isFollow);

    /**
     * 判断是否有关注
     * @param id 用户id
     * @return
     */
    Result isFollow(Long id);
}
