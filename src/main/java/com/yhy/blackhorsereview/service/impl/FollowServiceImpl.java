package com.yhy.blackhorsereview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.dto.UserDTO;
import com.yhy.blackhorsereview.entity.Follow;
import com.yhy.blackhorsereview.mapper.FollowMapper;
import com.yhy.blackhorsereview.service.IFollowService;
import com.yhy.blackhorsereview.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yhy
 * @since 2023-5-23
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long id, boolean isFollow) {
        // 判断是否登录
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，不用做判断
            return Result.ok();
        }
        Long userId = user.getId();
        // 判断是关注还是取消关注
        if (isFollow) {
            // 关注则新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            save(follow);
        } else {
            // 取消关注，删除数据
            QueryWrapper<Follow> followQueryWrapper = new QueryWrapper<>();
            followQueryWrapper.eq("user_id", userId).eq("follow_user_id", id);
            remove(followQueryWrapper);
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long id) {
        // 判断是否登录
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，不用做判断
            return Result.ok();
        }
        Long userId = user.getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", id).count();
        return Result.ok(count > 0);
    }
}
