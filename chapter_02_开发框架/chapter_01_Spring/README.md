# Spring 系列

以 Spring Framework 6.x、Boot 3.x 为主线，先理解对象与调用路径，再理解事务资源和 Web 请求，最后连接生产诊断。

| 顺序 | 深入正文 | 核心问题 |
| --- | --- | --- |
| 1 | [IoC 与 Bean 生命周期](01_IoC容器与Bean生命周期深入解析.md) | 定义、refresh、创建链、三级缓存、代理和作用域 |
| 2 | [事务传播、隔离与一致性](02_事务传播隔离与一致性实战.md) | 物理事务、回滚、锁、outbox/inbox 和故障恢复 |
| 3 | [Boot、Web 与生产诊断](03_Boot自动配置Web链路与生产诊断.md) | 自动配置、配置来源、MVC、连接池、超时及发布 |

[导读与速查](Spring与SpringBoot工程手册.md) 用于快速定位，不能替代上述正文。案例以订单为线索，跨资源的一致性还需结合 [Redis](../../chapter_03_中间件/chapter_01_Redis/README.md)、[Kafka](../../chapter_03_中间件/chapter_06_KAFKA/README.md) 和 [MySQL](../../chapter_03_中间件/chapter_03_SQL/README.md)。

目前未展开的独立主题包括 Spring Security/OAuth2、WebFlux 及完整 starter/测试工程，详见 [覆盖清单](../../编写规范与覆盖清单.md)。
