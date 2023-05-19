package com.yhy.blackhorsereview.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yhy.blackhorsereview.dto.LoginFormDTO;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.entity.User;
import com.yhy.blackhorsereview.mapper.UserMapper;
import com.yhy.blackhorsereview.service.IUserService;
import com.yhy.blackhorsereview.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

import static com.yhy.blackhorsereview.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合则返回失败信息
            return Result.fail("手机号格式错误！");
        }
        // 3.符合则生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 4.保存验证码到session
        session.setAttribute("code", code);
        // 5.发送验证码
        log.debug("生成验证码成功，验证码为:{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 如果不符合则返回失败信息
            return Result.fail("手机号格式错误！");
        }
        // 2.校验验证码
        String code = loginForm.getCode();
        Object codeInSession = session.getAttribute("code");
        if(codeInSession == null || !codeInSession.toString().equals(code)){
            // 不一致则报错
            return Result.fail("验证码错误！");
        }
        // 3.根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 4.用户不存在则创建新用户保存在数据库
        if(user == null){
            user = createUserWithPhone(phone);
        }
        // 5.保存用户信息到session
        session.setAttribute("user", user);

        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setCreateTime(LocalDateTime.now());
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(5));
        // 保存用户
        save(user);
        return user;
    }
}
