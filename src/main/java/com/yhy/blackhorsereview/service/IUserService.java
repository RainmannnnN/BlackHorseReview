package com.yhy.blackhorsereview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yhy.blackhorsereview.dto.LoginFormDTO;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author yhy
 * @since 2023-5-20
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone);

    /**
     * 用户登录
     * @param loginForm 登录表单
     * @return
     */
    Result login(LoginFormDTO loginForm);

    /**
     * 用户退出
     * @param token 保存的token
     * @return
     */
    Result logout(String token);

    /**
     * 实现签到功能,这个功能用postman测试
     * @return
     */
    Result sign();

    /**
     * 统计连续签到,这个功能用postman测试
     * @return
     */
    Result signCount();
}
