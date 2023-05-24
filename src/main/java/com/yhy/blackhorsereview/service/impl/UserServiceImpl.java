package com.yhy.blackhorsereview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yhy.blackhorsereview.dto.LoginFormDTO;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.dto.UserDTO;
import com.yhy.blackhorsereview.entity.User;
import com.yhy.blackhorsereview.mapper.UserMapper;
import com.yhy.blackhorsereview.service.IUserService;
import com.yhy.blackhorsereview.utils.RegexUtils;
import com.yhy.blackhorsereview.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.yhy.blackhorsereview.utils.RedisConstants.*;
import static com.yhy.blackhorsereview.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author yhy
 * @since 2023-5-19
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合则返回失败信息
            return Result.fail("手机号格式错误！");
        }

        // 3.符合则生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码
        log.debug("生成验证码成功，验证码为:{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 如果不符合则返回失败信息
            return Result.fail("手机号格式错误！");
        }

        // 2.从redis获取，校验验证码
        String code = loginForm.getCode();
        String codeInRedis = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if(codeInRedis == null || !codeInRedis.equals(code)){
            // 不一致则报错
            return Result.fail("验证码错误！");
        }

        // 3.根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 4.用户不存在则创建新用户保存在数据库
        if(user == null){
            user = createUserWithPhone(phone);
        }

        // 5.保存用户信息到redis
        // 随机生成token作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 将User对象装换成HashMap
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().
                        setIgnoreNullValue(true). // 把map里面的数据变成String类型的
                        setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 存储,并且设置过期时间
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 返回token
        return Result.ok(token);
    }

    @Override
    public Result logout(String token) {
        stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        return Result.ok();
    }

    @Override
    public Result sign() {
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 写入redis
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 获取本月截至今天为止的所有签到记录，返回一个十进制的数字
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if(result == null || result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num == null || num == 0){
            return Result.ok(0);
        }
        int count = 0; // 计数器
        while(true){
            // 和1做与运算，统计签到次数
            if((num & 1) == 0){
                // 为0说明没签到，直接退出
                break;
            }
            count++;
            num >>>= 1;
        }
        return Result.ok(count);

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
