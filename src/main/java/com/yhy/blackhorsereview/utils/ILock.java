package com.yhy.blackhorsereview.utils;

public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec 锁的超时时间，过期自动释放
     * @return true表示获取锁成功
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();


}
