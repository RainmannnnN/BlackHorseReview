package com.yhy.blackhorsereview;

import com.yhy.blackhorsereview.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class BlackHorseReviewApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Test
    void contextLoads() {
    }

    //@Test
    /*public void testSaveShop2Redis(){
        shopService.saveShop2Redis(1L, 10L);
    }*/

}
