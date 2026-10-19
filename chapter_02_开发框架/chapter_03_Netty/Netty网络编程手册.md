# Netty 网络编程导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

基线：Netty 4.1。重点是协议边界、线程模型、内存所有权和慢客户端治理。

## 1. Reactor、EventLoop 与 Pipeline

EventLoop 通常服务多个 Channel，同一 Channel 的 I/O 事件通常由绑定的 EventLoop 按序处理。Pipeline 由 Handler 组合编解码及业务逻辑；入站从头向后，出站向头传播，`ctx.write` 与 `channel.write` 的起始位置不同。

不要在 EventLoop 执行 JDBC、文件阻塞或长计算。转交业务执行器时队列必须有界，还要考虑同一连接消息顺序、超时后结果丢弃、Channel 关闭和上下文传播。

## 2. TCP 字节流与协议设计

TCP 不保存业务消息边界，所谓粘包/半包来自字节流分段与接收方式。协议要定义版本、类型、长度、请求 ID 和最大消息大小；校验长度后再分配内存，避免攻击者声明超大帧。

```java
// Pipeline 片段：4 字节长度头不含自身；帧总长限制为 1 MiB。
pipeline.addLast(new LengthFieldBasedFrameDecoder(
    1024 * 1024, 0, 4, 0, 4));
pipeline.addLast(new LengthFieldPrepender(4));
// 后续业务 handler 接收去除长度头的 ByteBuf。
```

双方必须统一端序、长度定义和编码。连接空闲检测只是存活信号的一部分，不能用“收到心跳”证明业务依赖健康。重连需退避与抖动，请求需唯一 ID、超时映射和幂等规则。

## 3. ByteBuf 所有权

引用计数要求每一份保留引用最终被释放。消费消息而不向后转交时通常要 release；异步跨线程保留时常需 retain 并在最终消费者释放。SimpleChannelInboundHandler 默认可能自动释放，不能再重复释放同一所有权。

slice/duplicate 通常共享底层内存，不能只看 Java 对象数量判断生命周期；retainedSlice 等操作会增加保留计数。泄漏检测用于诊断，长期高等级开启有额外成本，需按环境控制。

## 4. 背压、容量与故障排查

write 返回不代表对端已处理。写缓冲超过水位时 Channel 可能不可写，业务应暂停产生、丢弃允许丢弃的数据或断开过慢连接；单纯扩大缓冲只会推迟 OOM。

| 现象 | 证据 | 处理方向 |
| --- | --- | --- |
| 所有连接间歇卡顿 | EventLoop 栈、任务耗时 | 移走阻塞业务，限制业务队列 |
| direct memory 持续增长 | 泄漏报告、连接数、待写字节 | 检查 release/retain 和慢连接 |
| 消息乱序 | 请求 ID、执行器分配 | 对业务关键流串行处理或按序合并 |
| 重连风暴 | 连接建立率、错误码 | 退避、限速及分批恢复 |

容量至少估算连接元数据、读写缓冲、在途请求和堆外内存；JVM heap 不是整个进程内存。

## 5. 面试与练习

- **零拷贝是否完全没有复制？** 必须说明具体路径：组合缓冲减少用户态拼接，文件传输的系统调用可减少某些复制，不同 TLS/平台路径不同。
- **为什么 ChannelHandler 不能随便共享？** 有状态 handler 可能被多个 Channel 并发访问，只有满足线程安全条件才应声明可共享。
- **练习**：用 EmbeddedChannel 验证长度协议拆分、合并、超长帧，再用慢读客户端验证写缓冲受限。验收：无泄漏、无无限积压、消息失败可追踪。

## 6. 官方资料

- [引用计数对象](https://netty.io/wiki/reference-counted-objects.html)
- [Netty 4.1 API](https://netty.io/4.1/api/index.html)
