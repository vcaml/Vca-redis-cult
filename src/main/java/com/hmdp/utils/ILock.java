package com.hmdp.utils;

public interface ILock {

    /**
     * 获取redis的锁 参数为锁持有的超时时间 过期就自动释放
     * */
    boolean tryLock(long timeoutSec);


    /**
     * 释放锁
     * */
    void unlock();
}
