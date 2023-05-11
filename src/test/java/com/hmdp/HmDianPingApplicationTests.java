package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Voucher;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    RedisIdWorker redisIdWorker;

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


}
