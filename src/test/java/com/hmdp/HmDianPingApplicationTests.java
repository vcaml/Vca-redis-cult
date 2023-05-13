package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    RedisIdWorker redisIdWorker;

    @Resource
    IUserService userService;

    @Resource
    RedisTemplate redisTemplate;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testIdWorker(){
//         Runnable task = ()->{
//             for(int i =0 ; i < 100; i++){
//                 long id = redisIdWorker.nextId("order");
//                 System.out.println("id = " + id);
//             }
//         };
//        System.out.println("time start");
//         es.submit(task);
//        System.out.println("time end");

        System.out.println("long id = " + redisIdWorker.nextId("order"));

    }

    @Test
    void voucherJsonTest(){
        Voucher voucher = new Voucher();
        voucher.setShopId(1L);
        voucher.setTitle("100元代金券");
        voucher.setSubTitle("周一到周五均可使用");
        voucher.setRules("全场通用 无需预约 仅限堂食");
        voucher.setPayValue(8000L);
        voucher.setActualValue(10000l);
        voucher.setType(1);
        voucher.setStock(100);

        System.out.println(JSONUtil.toJsonStr(voucher));
    }

   @Test
    void userSignCount(){
       UserDTO user = new UserDTO();
       user.setId(1010l);
       UserHolder.saveUser(user);
       Result result = userService.signCount();
       System.out.println("result:"+result.getData());
       UserHolder.removeUser();
   }

   @Test
    void userSignSumCount(){
       String key ="sign:1010:202305";
       LocalDateTime now = LocalDateTime.now();
       System.out.println("sum:"+bitCount(key));

   }

    public long bitCount(String key) {
        return (long)redisTemplate.execute((RedisCallback<Long>) con -> con.bitCount(key.getBytes()));
    }
}
