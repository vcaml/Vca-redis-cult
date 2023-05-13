package com.vcaml.service.impl;

import com.vcaml.dto.Result;
import com.vcaml.entity.SeckillVoucher;
import com.vcaml.entity.VoucherOrder;
import com.vcaml.mapper.VoucherOrderMapper;
import com.vcaml.service.ISeckillVoucherService;
import com.vcaml.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vcaml.utils.RedisIdWorker;
import com.vcaml.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
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
 * @author larszhang
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    ISeckillVoucherService seckillVoucherService;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    RedissonClient redissonClient;

    @Resource
    RedisIdWorker redisIdWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        //判断是否开始和结束

        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            log.debug("秒杀尚未开始");
            return Result.fail("秒杀尚未开始");
        }

        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            log.debug("秒杀已经结束");
            return Result.fail("秒杀已经结束");
        }
        //判断是否库存充足
        if(seckillVoucher.getStock()<1){
            log.debug("秒杀券库存不足");
            return Result.fail("秒杀券库存不足");
        }

        //创建锁对象
        Long userId = UserHolder.getUser().getId();

        //自定义的setnx实现的分布式锁
        //SimpleRedisLock lock = new SimpleRedisLock("order:"+userId,stringRedisTemplate);

        //redisson获取分布式锁
         RLock lock = redissonClient.getLock("lock:order:"+userId);

         boolean isLock = lock.tryLock();

        //判断是否获得锁
        if(!isLock){
            log.debug("获取锁失败 这个分布锁 已经被其他线程获得");
            //根据业务判断 重试或者失败
            return Result.fail("秒杀券一人只能买一次 重复用户不能重复下单");
        }
        try{
            //获取代理对象
            IVoucherOrderService proxy =(IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }finally {
            lock.unlock();
        }

     }

     @Transactional
     public Result createVoucherOrder(Long voucherId){
         log.debug("0072 进入创建订单流程");

         Long userId = UserHolder.getUser().getId();

         //查一下这个用户是否已经抢过优惠券 一人只能获取一单
         int count = query().eq("user_id",userId).eq("voucher_id",voucherId).count();

         if(count>0){
             log.debug("count:{} 已经购买过 秒杀券一人只能买一次",count);
             return Result.fail("已经购买过");
         }

         boolean success = seckillVoucherService.update()
                 .setSql("stock = stock - 1")
                 .eq("voucher_id",voucherId)
                 .gt("stock",0) //加一行 stock = 刚开始查到的stock 确定没有被其他线程修改过 才进行更新
                 .update();

         if(!success){
             return Result.fail("扣减失败 库存不足");
         }


         VoucherOrder voucherOrder = new VoucherOrder();

         Long orderId = redisIdWorker.nextId("order");
         voucherOrder.setId(orderId);

         voucherOrder.setUserId(userId);

         voucherOrder.setVoucherId(voucherId);

         save(voucherOrder);

         return Result.ok(orderId);
     }
}
