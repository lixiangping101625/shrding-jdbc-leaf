### sharding-jdbc 使用雪花算法
#### 1、数据库准备
    CREATE TABLE `t_order_0` (
      `id` bigint(20) NOT NULL COMMENT '主键不自增也可以，leaf算法会设置',
      `title` varchar(255) NOT NULL,
      `user_id` bigint(20) NOT NULL,
      PRIMARY KEY (`id`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
#### 2、配置    
    # 生命ShardingSphere的数据源
    spring.shardingsphere.datasource.names=ds0,ds1
    
    # 配置第 1 个数据源
    spring.shardingsphere.datasource.ds0.type=com.zaxxer.hikari.HikariDataSource
    spring.shardingsphere.datasource.ds0.driver-class-name=com.mysql.cj.jdbc.Driver
    spring.shardingsphere.datasource.ds0.jdbc-url=jdbc:mysql://localhost:3307/shard_order_leaf
    spring.shardingsphere.datasource.ds0.username=root
    spring.shardingsphere.datasource.ds0.password=123456
    # 配置第 2 个数据源
    spring.shardingsphere.datasource.ds1.type=com.zaxxer.hikari.HikariDataSource
    spring.shardingsphere.datasource.ds1.driver-class-name=com.mysql.cj.jdbc.Driver
    spring.shardingsphere.datasource.ds1.jdbc-url=jdbc:mysql://120.27.203.113:3306/shard_order_leaf
    spring.shardingsphere.datasource.ds1.username=root
    spring.shardingsphere.datasource.ds1.password=yuanban_mysql
    
    
    #####################################################
    ######t_order表分片规则（其实就是实际节点）
    #####################################################
    spring.shardingsphere.rules.sharding.tables.t_order.actual-data-nodes=ds$->{0..1}.t_order_$->{0..1}
    
    ##### 分库策略
    spring.shardingsphere.rules.sharding.tables.t_order.database-strategy.standard.sharding-column=id
    spring.shardingsphere.rules.sharding.tables.t_order.database-strategy.standard.sharding-algorithm-name=database-inline
    spring.shardingsphere.rules.sharding.sharding-algorithms.database-inline.type=INLINE
    spring.shardingsphere.rules.sharding.sharding-algorithms.database-inline.props.algorithm-expression=ds$->{id % 2}
    
    #####分表策略
    spring.shardingsphere.rules.sharding.tables.t_order.table-strategy.standard.sharding-column=user_id
    spring.shardingsphere.rules.sharding.tables.t_order.table-strategy.standard.sharding-algorithm-name=table-inline
    spring.shardingsphere.rules.sharding.sharding-algorithms.table-inline.type=INLINE
    spring.shardingsphere.rules.sharding.sharding-algorithms.table-inline.props.algorithm-expression=t_order_$->{user_id % 2}
    
    # 分布式序列(主键)策略配置
    spring.shardingsphere.rules.sharding.tables.t_order.key-generate-strategy.column=id
    spring.shardingsphere.rules.sharding.tables.t_order.key-generate-strategy.key-generator-name=snowflake
    # 分布式序列（主键）算法配置
    spring.shardingsphere.rules.sharding.key-generators.snowflake.type=SNOWFLAKE
    spring.shardingsphere.rules.sharding.key-generators.snowflake.props.worker-id=123
    
    
    #Sharding-jdbc打印sql
    spring.shardingsphere.props.sql-show=true    
    