# MyBatis 原理与数据访问导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

适用范围：MyBatis 3.5.x、MyBatis-Spring；SQL 示例按 MySQL 8.x 理解。ORM 工具不会自动解决索引、锁和事务问题。

## 1. 一次 Mapper 调用的路径

Mapper 接口代理根据方法定位 MappedStatement，经 SqlSession、Executor、StatementHandler、ParameterHandler、ResultSetHandler 完成 SQL 执行及映射。插件可以拦截特定执行组件，但过多插件会让 SQL、分页和缓存行为难以预测。

SqlSession 本身通常不能跨线程共享；Spring 集成的 SqlSessionTemplate 负责线程及事务协作。直接使用 DefaultSqlSession 时，必须自己管理提交、回滚和关闭。

## 2. 参数、映射与安全

`#{value}` 通常绑定 PreparedStatement 参数；`${value}` 做文本替换，不能直接接收用户输入。列名、排序方向不能用值参数替代，必须通过服务端白名单选择静态 SQL 片段。

```xml
<!-- 片段：namespace、接口及字段映射由工程提供 -->
<select id="listAfter" resultType="com.example.OrderRow">
  SELECT id, created_at, status
  FROM orders
  WHERE tenant_id = #{tenantId}
    AND id &gt; #{afterId}
  <if test="status != null">
    AND status = #{status}
  </if>
  ORDER BY id
  LIMIT #{limit}
</select>
```

tenantId 从受信任的登录上下文派生，不能直接相信请求字段；limit 在入口限制。resultMap 明确主键和关联映射，避免同名列覆盖。金额使用合适的 DECIMAL/BigDecimal，时间区分时刻、当地日期和时区。

## 3. 缓存、批处理与事务

一级缓存与 SqlSession 及 localCacheScope 有关，不保证跨会话一致。二级缓存按 namespace 管理，跨 namespace 或其他应用修改数据时可能失效不及时；高更新业务不要把它当作可靠的一致性层。

BATCH executor 在 flush 时才可能真正发送部分操作，错误、更新计数和主键回填时机都需要验证。批量大小要兼顾包大小、锁持有时间及回滚成本，不能只测最快吞吐。

MyBatis 执行的 SQL 能否在同一事务内，取决于 Spring 事务管理器、数据源和连接绑定。多个数据源上的 `@Transactional` 不会自然成为分布式事务。

## 4. 性能与排查

N+1 来自主查询后逐条关联查询，先统计一次请求的 SQL 条数，再用 JOIN、批量查询或读模型减少往返。JOIN 也可能放大行数，分页时尤其要注意一对多导致的结果数量变化。

深分页用稳定排序和游标条件；跨字段排序使用相应复合游标并考虑并发变更。大结果流式读取的 fetchSize 只是驱动提示，是否真正流式及其连接占用需按驱动配置验证。

排查按“最终 SQL 与参数 → 执行计划 → 扫描行/锁等待 → 映射与序列化 → 连接池等待”推进。日志脱敏并限制量，避免为排查慢 SQL 导致敏感数据外泄或磁盘打满。

## 5. 面试与练习

- **分页插件为什么可能慢？** count 查询、深 offset、JOIN 行数及不合适索引仍然存在；插件只改写 SQL。
- **一级缓存为什么偶尔读到旧值？** 说明会话范围、失效点和数据库隔离级别，分别验证缓存与快照读。
- **练习**：对十万订单做分页与批量状态更新。验收：SQL 条数有上界，跨租户查询被拒绝，空集合不生成错误 IN，更新失败能按既定事务边界回滚。

## 6. 官方资料

- [Mapper XML](https://mybatis.org/mybatis-3/sqlmap-xml.html)
- [MyBatis-Spring 事务](https://mybatis.org/spring/transactions.html)
