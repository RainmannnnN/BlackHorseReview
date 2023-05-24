package com.yhy.blackhorsereview;

import com.yhy.blackhorsereview.entity.Shop;
import com.yhy.blackhorsereview.service.impl.ShopServiceImpl;
import com.yhy.blackhorsereview.utils.RedisIdWorker;
import io.lettuce.core.api.sync.RedisGeoCommands;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.yhy.blackhorsereview.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
class BlackHorseReviewApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);


    @Test
    void contextLoads() {
    }

    /**
     * id生成器的测试
     * @throws InterruptedException
     */
    @Test
    public void testIdWorker() throws InterruptedException{
        CountDownLatch countDownLatch = new CountDownLatch(300);

        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            countDownLatch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }

    //@Test
    /*public void testSaveShop2Redis(){
        shopService.saveShop2Redis(1L, 10L);
    }*/

    /**
     * 加载店铺经纬度数据，存入redis
     */
    @Test
    void testLoadShopData(){
        // 查询店铺信息
        List<Shop> list = shopService.list();
        // 把店铺分组，按照typeid分组，id一致的在一个list里
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        // 分批完成redis的写入
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            // 获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            // 获取同类型的店铺的集合
            List<Shop> value = entry.getValue();
           // List<RedisGeoCommands<String>> locations = new ArrayList<RedisGeoCommands.GeoLocation<String>>(value.size());
            // 写入redis key，经纬度，member
            for (Shop shop : value) {
                stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
            }
        }
    }

}
