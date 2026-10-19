# Spring 与 Spring Boot 工程导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

基线：Spring Framework 6、Spring Boot 3、Java 17+。这是固定学习基线，不代表最新版本；升级到其他主版本时单独核对 JDK、Jakarta API、配置与依赖兼容性。

## 1. 容器、生命周期与扩展点

BeanDefinition 描述如何创建 Bean，ApplicationContext 负责资源、事件和容器协作。典型过程是读取定义、执行 BeanFactoryPostProcessor、实例化、依赖注入、初始化前处理、初始化回调、初始化后处理并可能生成代理。销毁回调由容器管理，prototype 的完整销毁不自动托管。

BeanFactoryPostProcessor 修改定义，BeanPostProcessor 处理实例；AOP 自动代理建立在实例处理机制上。构造器注入使依赖显式，并能在启动时暴露循环设计。三级缓存是部分单例循环依赖场景的内部机制，不等于任意循环都可解决；构造器循环、prototype 和代理组合有额外限制。

单例仅表示容器中的共享实例，不保证字段线程安全。Controller/Service 避免保存请求级可变状态。不要靠 `@Lazy` 掩盖领域之间相互调用的结构问题。

## 2. AOP 与事务边界

默认代理模式下，调用要经过代理才触发切面；同对象内部自调用通常绕过代理。JDK 动态代理和类代理对接口、final/private 方法的能力不同，不能仅以是否存在注解判断事务生效。

```java
// 片段：由 Controller 或其他 Bean 调用该 Spring Bean；省略构造器注入。
@Transactional(rollbackFor = Exception.class)
public long createOrder(CreateOrder command) throws Exception {
    var id = orders.insert(command);
    outbox.insert("OrderCreated", id); // 同一个数据源、同一事务
    return id;
}
```

默认情况下 RuntimeException/Error 导致回滚，checked exception 需按规则配置。捕获异常后正常返回可能提交；内层已标记 rollback-only 时，外层捕获也不能恢复为可提交状态。异步线程通常不继承基于线程绑定的本地事务。

| 传播级别 | 含义 | 常见风险 |
| --- | --- | --- |
| REQUIRED | 加入现有事务或新建 | 内层失败可能使整个事务回滚 |
| REQUIRES_NEW | 使用独立物理事务 | 外层占用连接时可能加剧连接池压力 |
| NESTED | 已有事务内使用保存点，依赖实现支持 | 不等于独立提交，也不保证跨资源支持 |

数据库提交后直接发消息，进程可能在两者之间崩溃。事务内写 outbox，由可靠任务投递并允许重复，消费者幂等。afterCommit 回调能避免未提交就发送，但没有自动消除提交后崩溃的丢失窗口。

## 3. Boot 自动配置与 Web 请求链

自动配置依赖类路径、属性和 Bean 条件。用户声明的 Bean 可以让部分配置退让，具体取决于条件。排查“为什么没有创建 Bean”时看条件评估报告，再检查扫描范围与配置绑定，避免盲目添加更多注解。

MVC 请求经过 Servlet Filter、DispatcherServlet、HandlerMapping、HandlerAdapter、拦截器及 Controller，再经消息转换器输出。Filter 适合通用请求处理，Interceptor 了解 MVC handler；鉴权应使用统一安全链并检查对象权限，不能只在某个 Controller 中做登录判断。

参数校验、异常映射和错误码保持契约一致；分页必须有上限，上传和 JSON body 必须有大小限制。WebFlux 的价值取决于整条调用链是否非阻塞，把 JDBC 放在事件循环里会破坏并发模型。

## 4. 线上排查与工程取舍

| 现象 | 先取证 | 后续动作 |
| --- | --- | --- |
| 事务未生效 | 代理类型、调用路径、数据源及异常类型 | 拆分 Bean 或显式事务模板，补集成验证 |
| 请求阻塞 | 线程栈、连接池等待、下游时延 | 缩短事务、设总截止时间与隔离限额 |
| 启动异常 | 最深 cause、配置来源、条件报告 | 验证 profile、依赖及环境变量覆盖 |
| 健康检查成功却不能服务 | readiness 语义、容量、依赖 | 分离存活与接流判断，防重启雪崩 |

Actuator 管理端点按最小权限暴露；指标标签不放用户 ID、订单 ID 等高基数值。测试区分纯领域单元、Web 切片和真实数据库集成，事务测试自动回滚不能替代提交后路径验证。

## 5. 面试追问与回答要点

- **Spring 如何解决循环依赖？** 先限定单例属性注入及版本配置，再解释早期引用与代理一致性；最终说明为何应消除循环。
- **事务能管住 HTTP 调用吗？** 本地事务只能约束参与的资源，外部副作用要用业务状态、幂等和补偿处理。
- **什么时候不用微服务？** 团队边界、独立发布收益不足时，模块化单体通常更易保证一致性和排障。

## 6. 练习与验收

实现创建订单和 outbox 同事务写入；分别在订单写入后、事务提交后、投递成功后确认前模拟崩溃。验收：无半条本地业务数据，未投递事件可恢复，重复事件不重复扣库存，有积压与失败指标。

## 7. 官方资料

- [事务注解及代理限制](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)
- [Bean 生命周期](https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html)
- [Spring Boot 参考文档](https://docs.spring.io/spring-boot/reference/)
