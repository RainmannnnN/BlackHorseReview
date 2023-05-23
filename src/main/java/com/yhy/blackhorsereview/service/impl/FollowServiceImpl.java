package com.yhy.blackhorsereview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.dto.UserDTO;
import com.yhy.blackhorsereview.entity.Follow;
import com.yhy.blackhorsereview.mapper.FollowMapper;
import com.yhy.blackhorsereview.service.IFollowService;
import com.yhy.blackhorsereview.service.IUserService;
import com.yhy.blackhorsereview.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;


    @Override
    public Result follow(Long id, boolean isFollow) {
        // 判断是否登录
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，不用做判断
            return Result.ok();
        }
        Long userId = user.getId();
        String key = "follows:" + userId;
        // 判断是关注还是取消关注
        if (isFollow) {
            // 关注则新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            boolean save = save(follow);
            if (save) {
                // 把关注用户的id存到redis的set集合

                stringRedisTemplate.opsForSet().add(key, id.toString());
            }
        } else {
            // 取消关注，删除数据
            QueryWrapper<Follow> followQueryWrapper = new QueryWrapper<>();
            followQueryWrapper.eq("user_id", userId).eq("follow_user_id", id);
            boolean remove = remove(followQueryWrapper);
            if (remove) {
                // 移除
                stringRedisTemplate.opsForSet().remove(key, id.toString());
            }

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

    @Override
    public Result followCommons(Long id) {
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        // 求交集
        String key2 = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key, key2);
        if(intersect == null || intersect.isEmpty()){
            // 无交集
            return Result.ok(Collections.emptyList());
        }
        // 解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        // 查询用户
        List<UserDTO> userDTOS = userService.listByIds(ids).stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());

        return Result.ok(userDTOS);
    }
}
