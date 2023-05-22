package com.yhy.blackhorsereview.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yhy.blackhorsereview.config.RedissonConfig;
import com.yhy.blackhorsereview.dto.Result;
import com.yhy.blackhorsereview.entity.SeckillVoucher;
import com.yhy.blackhorsereview.entity.VoucherOrder;
import com.yhy.blackhorsereview.mapper.VoucherOrderMapper;
import com.yhy.blackhorsereview.service.ISeckillVoucherService;
import com.yhy.blackhorsereview.service.IVoucherOrderService;
import com.yhy.blackhorsereview.utils.RedisIdWorker;
import com.yhy.blackhorsereview.utils.SimpleRedisLock;
import com.yhy.blackhorsereview.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yhy
 * @since 2023-5-21
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始!");
        }
        // 判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束！");
        }
        // 判断库存是否充足
        Integer stock = voucher.getStock();
        if (stock < 1) {
            return Result.fail("优惠券库存不足！");
        }
        // 获取锁对象
        Long userId = UserHolder.getUser().getId();
        //SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock rLock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = rLock.tryLock();
        // 判断是否获取锁成功
        if (!isLock) {
            return Result.fail("一个人只能下一单！");
        }
        try{
            // 获取与事务有关的代理对象，要用到aspectj
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 释放锁
            rLock.unlock();
        }



    }

    @Transactional
    public Result createVoucherOrder(Long voucherId){
        // 实现一人一单
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            // 用户已经下过单了
            return Result.fail("每个用户限一次！");
        }

        // 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0) // CAS来实现乐观锁
                .update();
        if (!success) {
            return Result.fail("优惠券库存不足！");
        }

        //创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 生成订单号
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setVoucherId(voucherId);
        // 设置用户id

        voucherOrder.setUserId(userId);
        save(voucherOrder);
        return Result.ok(orderId);


    }
}
