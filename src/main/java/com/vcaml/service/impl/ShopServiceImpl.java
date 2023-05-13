package com.vcaml.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.vcaml.dto.Result;
import com.vcaml.entity.Shop;
import com.vcaml.mapper.ShopMapper;
import com.vcaml.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.vcaml.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author larzhzang
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        //简单情况下的通过id查询shop
       // Shop shop = queryByShopId(id);

        //查询shop的情况下增加互斥锁 解决缓存击穿
        Shop shop = queryWithMutex(id);

        if (shop==null){
           return Result.fail("shop不存在");
        }
        return Result.ok(shop);
    }

    //查询shop的基础上增加互斥锁 解决缓存击穿
    private Shop queryWithMutex(Long id) {
        //先查redis
        String shopJson =  stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        //存在就返回
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson,Shop.class);
            return shop;
        }
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;

        try {
            //缓存不存在
            //先需要获取互斥锁
            boolean isLock = tryLock(lockKey);
            //获取失败 休眠并重试
            if (!isLock){
                Thread.sleep(50);
                queryWithMutex(id);
            }
            //获取锁成功 可以去查询数据库
            shop = getById(id);
            Thread.sleep(200);

            if (shop == null){
               return null;
            }

            //写入缓存
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }

        return shop;
    }

    //简单情况下的通过id查询shop
    private Shop queryByShopId(Long id){
       String shopJson =  stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
       //存在就返回
       if(StrUtil.isNotBlank(shopJson)){
           Shop shop = JSONUtil.toBean(shopJson,Shop.class);
           return shop;
       }

       //不存在，先查数据
       Shop shop =getById(id);

       //数据库不存在 返回错误
       if(shop == null){
           return null;
       }

       //数据库存在， 写入redis 再返回
       stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);

       return shop;
   }

   //利用setnx 实现互斥锁
    private boolean tryLock(String key){
       Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.MINUTES);
       return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {

        //这里使用事务保证 更新和删除缓存的一致性。但是只在单机情况下可靠，@Transactional无法保证分布式中不同机器进程之前的原子性

        Long id = shop.getId();

        if (id == null){
            return Result.fail("SHOP id 不能为空");
        }

        //更新数据库
        updateById(shop);

        //再删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);

        return Result.ok();
    }
}
