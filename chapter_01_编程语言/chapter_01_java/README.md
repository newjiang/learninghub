# Java 系列

本系列保留原有长文，以语言机制、并发、运行时和框架原理串成后端主线。不是只按 API 分类记忆。

## 1. 已有专题

- 类型与元编程：[泛型](泛型Generics.md)、[注解](注解Annotation.md)、[反射](反射Reflection.md)、[动态代理](动态代理%20Dynamic%20Proxy.md)。
- 数据与 I/O：[集合](集合框架.md)、[IO](IO.md)、[JDBC](JDBC.md)。
- 并发：[JUC](JUC.md)、[锁机制](锁机制.md)、[线程池](线程池.md)。
- 类与运行时：[类加载](类加载机制.md)、[自动内存管理](chapter_02_jvm/自动内存管理.md)、[垃圾收集器](chapter_02_jvm/垃圾收集器与内存分配策略.md)。

## 2. 新增深入正文

- [Java 工程设计与并发边界](Java后端工程设计与并发边界.md)：类型、异常、JMM、组合原子性、线程池和异步协作。
- [JVM 生产诊断](chapter_02_jvm/JVM性能分析与生产故障诊断.md)：CPU、锁/连接等待、GC、堆外、容器与取证。
- [Java 21 虚拟线程](chapter_01_feature/chapter_java_21_feature/01_虚拟线程调度资源隔离与迁移深入解析.md)：调度、pinning、下游并发、取消及迁移实验。

## 3. 版本迁移导读

[Java 11](chapter_01_feature/chapter_java_11_feature/Java11升级与实践.md)、[Java 17](chapter_01_feature/chapter_java_17_feature/Java17语言建模与迁移.md)、[Java 21 速查](chapter_01_feature/chapter_java_21_feature/Java21虚拟线程与现代语法.md) 属于导读。特性引入版本、当前基线和预览状态分别说明，不将某一 LTS 间可用能力全部称作该版本首创。

从 Java 进入框架，继续阅读 [Spring](../../chapter_02_开发框架/chapter_01_Spring/README.md)、[MyBatis](../../chapter_02_开发框架/chapter_02_Mybatis/README.md) 和 [Netty](../../chapter_02_开发框架/chapter_03_Netty/README.md)。
