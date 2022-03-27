package com.hlkj.shrdingjdbcleaf.pojo;

import lombok.Data;

import javax.persistence.*;

/**
 * @author Lixiangping
 * @createTime 2022年03月27日 15:39
 * @decription:
 */
@Entity
@Table(name = "t_order")
@Data
public class TOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)//指示持久性提供程序必须使用数据库标识列为实体分配主键
    private Long id;
    private String title;
    private Long userId;

}
