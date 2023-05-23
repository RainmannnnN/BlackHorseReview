package com.yhy.blackhorsereview.controller;


import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.service.IBlogService;
import com.yhy.blackhorsereview.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author yhy
 * @since 2023-5-23
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;


    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long id, @PathVariable("isFollow") boolean isFollow){
        return followService.follow(id, isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long id){
        return followService.isFollow(id);
    }


}
