package com.yhy.blackhorsereview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.entity.Blog;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yhy
 * @since 2023-5-23
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result queryHotBlog(Integer current);

    Result likeBlog(Long id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    /**
     * 查询关注列表发送的博客
     * @param max 上一次查询的最小时间戳
     * @param offset 偏移量
     * @return ScrollResult对象
     */
    Result getBlogsOfFollow(Long max, Integer offset);
}
