package com.hlkj.shrdingjdbcleaf.repository;

import com.hlkj.shrdingjdbcleaf.pojo.TOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * @author Lixiangping
 * @createTime 2022年03月27日 15:41
 * @decription:
 */
public interface TOrder0Repository extends JpaRepository<TOrder, Long> {

    @Query(nativeQuery = true, value = "insert into t_order(id, title,user_id) values(#{id}, #{title},#{userId})")
    TOrder insertTest(TOrder order);

}
