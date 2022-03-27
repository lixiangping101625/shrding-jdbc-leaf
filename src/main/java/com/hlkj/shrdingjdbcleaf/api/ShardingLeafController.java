package com.hlkj.shrdingjdbcleaf.api;

import com.hlkj.shrdingjdbcleaf.pojo.TOrder;
import com.hlkj.shrdingjdbcleaf.repository.TOrder0Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lixiangping
 * @createTime 2022年03月27日 15:40
 * @decription: 测试shardingjdbc使用雪花算法
 */
@RestController
public class ShardingLeafController {

    @Autowired
    private TOrder0Repository order0Repository;

    @RequestMapping("/leaf/{userId}")
    public TOrder testLeaf(@PathVariable("userId") Long userId){

        TOrder order = new TOrder();
//        order.setId(10L);
        order.setTitle("测试订单");
        order.setUserId(userId);

        TOrder orderSave = order0Repository.save(order);
        return orderSave;
    }

}
