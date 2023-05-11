package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    ISeckillVoucherService seckillVoucherService;

    @Resource
    RedisIdWorker redisIdWorker;

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        //查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        //判断是否开始和结束

        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始");
        }

        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已经结束");
        }
        //判断是否库存充足

        if(seckillVoucher.getStock()<1){
            return Result.fail("秒杀券库存不足");
        }

        //扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id",voucherId)
                .gt("stock",0) //加一行 stock = 刚开始查到的stock 确定没有被其他线程修改过 才进行更新
                .update();

        if(!success){
            return Result.fail("扣减失败 库存不足");
        }

        //创建订单

        VoucherOrder voucherOrder = new VoucherOrder();

        Long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);

        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);

        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);

        //返回订单id
        return Result.ok(orderId );
     }
}
