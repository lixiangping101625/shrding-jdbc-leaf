package com.hlkj.shrdingjdbcleaf;

import com.hlkj.shrdingjdbcleaf.pojo.TOrder;
import com.hlkj.shrdingjdbcleaf.repository.TOrder0Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ShrdingJdbcLeafApplicationTests {

    @Test
    void contextLoads() {
    }


    @Autowired
    private TOrder0Repository order0Repository;

    @Test
    @Transactional
    void testShardingTransaction(){
        TOrder order = new TOrder();
//        order.setId(1L);
        order.setTitle("测试订单");
        order.setUserId(11L);
        TOrder orderSave = order0Repository.save(order);

        TOrder order2 = new TOrder();
//        order2.setId(2L);
        order2.setTitle("测试订单");
        order2.setUserId(12L);

        TOrder orderSave2 = order0Repository.save(order2);

        throw new RuntimeException("测试shardingjdbc 事务");
    }

}
