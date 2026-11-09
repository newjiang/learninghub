# JUC面试：从结论到机制与场景推演

> **使用方式**：先用一句话回答，再展开实现、前提和代价。遇到版本问题先说明目标 JDK（Java Development Kit，Java 开发工具包）。配套 [JUC原理](JUC原理.md)。

## 题目导航

- [Q1：JUC 线程安全是否等于业务线程安全？](#q-01)
- [Q2：AtomicLong 与 LongAdder 怎样选？](#q-02)
- [Q3：Latch、Barrier、Phaser 区别？](#q-03)
- [Q4：Semaphore 是限流器吗？](#q-04)
- [Q5：SynchronousQueue 容量为何是零？](#q-05)
- [Q6：Future 超时与取消有什么区别？](#q-06)
- [Q7：thenApply 与 thenCompose 有何区别？](#q-07)
- [Q8：allOf 与 anyOf 能实现什么？](#q-08)
- [Q9：handle、exceptionally、whenComplete 怎么选？](#q-09)
- [Q10：orTimeout 会停止远端请求吗？](#q-10)
- [Q11：ThreadLocal 弱键为什么仍泄漏？](#q-11)
- [Q12：park 与中断怎样组合？](#q-12)
- [Q13：阻塞队列单锁与双锁有什么权衡？](#q-13)
- [Q14：FutureTask 为什么先进入 COMPLETING 再发布 NORMAL？](#q-14)
- [Q15：cancel(false) 能取消已经开始的任务吗？](#q-15)
- [Q16：complete 会不会被 thenApply 的慢操作拖住？](#q-16)
- [Q17：complete 返回 false 说明什么？](#q-17)

<a id="q-01"></a>
### Q1：JUC 线程安全是否等于业务线程安全？

> **一句话结论**：不是，工具只保障各自契约，组合不变量需另外定义。

**机制展开**：安全发布不保护发布后的随意修改，多个原子变量也不是自动事务。

**⚖️ 方案对比与代价**：不可变消息、所有权转移与统一锁可减少组合风险。

**🔎 追问演练**：队列 put 后继续修改对象安全吗？

**推演要点**：交接保证放入前动作的发布，之后修改需另行同步。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-02)。

<a id="q-02"></a>
### Q2：AtomicLong 与 LongAdder 怎样选？

> **一句话结论**：严格单值状态转换选原子变量，高争用指标统计可考虑分片累加。

**机制展开**：LongAdder 汇总多个单元，sum 不是并发瞬时强快照。

**⚖️ 方案对比与代价**：扩展性换来较弱快照，不应拿近似统计做库存或余额约束。

**🔎 追问演练**：sum 小于阈值再 increment 能限流吗？

**推演要点**：两步组合不原子且汇总可能变化，应设计原子准入或 Semaphore。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-03)。

<a id="q-03"></a>
### Q3：Latch、Barrier、Phaser 区别？

> **一句话结论**：分别偏向一次完成计数、固定参与者会合、动态多阶段协调。

**机制展开**：Latch 到零不重置，Barrier 可多轮但会破损，Phaser 需要登记/注销维护。

**⚖️ 方案对比与代价**：越灵活越需管理失败和参与者生命周期。

**🔎 追问演练**：某任务异常后 await 永远等为什么？

**推演要点**：检查 finally 是否 countDown 或注销，并另行记录失败结果。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-04)。

<a id="q-04"></a>
### Q4：Semaphore 是限流器吗？

> **一句话结论**：它直接限制并发许可，不直接定义每秒速率。

**机制展开**：acquire 成功后才能保证对应 release，中断未获许可时不能无条件归还。

**⚖️ 方案对比与代价**：还要协调连接池与期限，否则许可前后仍会形成多重排队。

**🔎 追问演练**：为什么许可越用越多？

**推演要点**：Semaphore 不校验线程所有权，检查重复 release 或未成功 acquire 的释放。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-05)。

<a id="q-05"></a>
### Q5：SynchronousQueue 容量为何是零？

> **一句话结论**：它是生产者与消费者直接交接，不保存普通待消费元素。

**机制展开**：put 等待接收方，offer 在无匹配者时可失败，在线程池里直接改变扩线程和拒绝行为。

**⚖️ 方案对比与代价**：适合交接不适合缓冲，必须限制线程或上游准入。

**🔎 追问演练**：size 为零代表没有压力吗？

**推演要点**：不代表，可能有大量生产者或执行线程在别处等待。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-06)。

<a id="q-06"></a>
### Q6：Future 超时与取消有什么区别？

> **一句话结论**：超时只结束一次等待，取消改变结果状态并按实现发出协作请求。

**机制展开**：FutureTask 可尝试中断，CompletableFuture 不按同一方式控制底层任务。

**⚖️ 方案对比与代价**：需要统一期限和底层终止协议，结果不用不代表工作停止。

**🔎 追问演练**：任务拒绝却 get 永远等怎么修？

**推演要点**：让拒绝路径抛错或把对应结果明确取消/失败，别静默丢弃。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-07)。

<a id="q-07"></a>
### Q7：thenApply 与 thenCompose 有何区别？

> **一句话结论**：前者转换值，后者串联异步阶段并展平嵌套。

**机制展开**：返回 Future 的 thenApply 仍是嵌套结构，thenCompose 连接下一阶段完成状态。

**⚖️ 方案对比与代价**：都不强制切线程，Async 和显式执行器另行决定调度。

**🔎 追问演练**：非 Async 一定在注册线程执行吗？

**推演要点**：不一定，可由前驱完成线程执行，前驱已完成时也可能由注册线程执行。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-08)。

<a id="q-08"></a>
### Q8：allOf 与 anyOf 能实现什么？

> **一句话结论**：allOf 等全部终结但不收集结果，anyOf 取首个完成且可能是失败。

**机制展开**：失败不自动取消其余任务，Void 结果需再读取各阶段。

**⚖️ 方案对比与代价**：首个成功、快速失败并取消等策略需额外组合与生命周期管理。

**🔎 追问演练**：anyOf 返回异常为何另一个成功没采用？

**推演要点**：语义是首个完成，不是首个成功，应设计成功筛选策略。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-08)。

<a id="q-09"></a>
### Q9：handle、exceptionally、whenComplete 怎么选？

> **一句话结论**：分别是双路径转换、失败恢复、完成观察。

**机制展开**：观察回调抛异常也可能影响后续结果，join 与 get 的包装和中断语义不同。

**⚖️ 方案对比与代价**：记录日志不应无意把业务失败吞成成功，恢复必须有可信降级值。

**🔎 追问演练**：whenComplete 打日志失败会怎样？

**推演要点**：检查返回阶段的异常规则，不能把观察器当绝对无副作用。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-09)。

<a id="q-10"></a>
### Q10：orTimeout 会停止远端请求吗？

> **一句话结论**：不会自动停止底层操作，只改变阶段的完成状态。

**机制展开**：上游和兄弟任务的取消也不普遍自动传播，需要显式接入期限、取消与资源关闭。

**⚖️ 方案对比与代价**：避免超时后继续放大负载与重复副作用，重试要有幂等策略。

**🔎 追问演练**：cancel(true) CompletableFuture 为什么没中断？

**推演要点**：该参数不控制它的底层执行，不能照搬 FutureTask。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-09)。

<a id="q-11"></a>
### Q11：ThreadLocal 弱键为什么仍泄漏？

> **一句话结论**：键变弱不代表值立即清理，长寿命线程仍可保留值。

**机制展开**：池线程映射、值引用链与清理时机共同决定保留，任务还可能污染下一请求。

**⚖️ 方案对比与代价**：finally 清理或恢复作用域，必要时显式传参或采用适用的 ScopedValue。

**🔎 追问演练**：InheritableThreadLocal 为何没带来本次请求值？

**推演要点**：继承发生在线程创建，不是在每次向已存活 worker 提交时。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-10)。

<a id="q-12"></a>
### Q12：park 与中断怎样组合？

> **一句话结论**：park 返回后要查业务条件与中断，不把返回等同成功通知。

**机制展开**：许可最多一个，不累计；中断和虚假返回均可让 park 结束。

**⚖️ 方案对比与代价**：直接使用需完整同步协议，常规等待优先用成熟同步器。

**🔎 追问演练**：Thread.interrupted 连调两次为何结果不同？

**推演要点**：它查询并清除当前标志，后次没有新的中断可能是 false。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-11)。

<a id="q-13"></a>
### Q13：阻塞队列单锁与双锁有什么权衡？

> **一句话结论**：单锁结构简单，分离两端锁能提高某些生产消费并行度，却增加计数与通知协调。

**机制展开**：数组队列常用一个锁与两个条件，链式阻塞队列通过两端锁、原子计数和跨端通知维护边界转换。

**⚖️ 方案对比与代价**：双锁不是通用性能赢家，节点分配、计数热点、批量和缓存局部性也影响结果。

**🔎 追问演练**：从空到非空、从满到非满为什么是关键？

**推演要点**：这些转换决定需唤醒哪一端，漏通知会让可推进的线程永久等待。

**原理展开**：[JUC原理对应章节](JUC原理.md#p-12)。

<a id="q-14"></a>
### Q14：FutureTask 为什么先进入 COMPLETING 再发布 NORMAL？

> **一句话结论**：先独占完成权，再写 outcome，最后让等待者可靠看到终态和结果。

**机制展开**：中间态阻止正常、异常与取消互相覆盖，并避免结果未写好就被当完成返回。

**⚖️ 方案对比与代价**：内部状态服务发布与竞争，不能混同公开 Future.State，也不能推导线程已退出。

**🔎 追问演练**：NEW 是不是必然尚未开始？

**推演要点**：不是，它也可能表示 Callable 正在执行但尚未完成。

**原理展开**：[JUC深入机制](JUC原理.md#p-13)。

<a id="q-15"></a>
### Q15：cancel(false) 能取消已经开始的任务吗？

> **一句话结论**：可能取消其 Future 结果，却不要求中断执行线程。

**机制展开**：取消先赢得状态后，Callable 仍可继续，后来的正常返回不能覆盖取消终态。

**⚖️ 方案对比与代价**：结果不再使用和工作停止分开处理，外部副作用需独立协议。

**🔎 追问演练**：get 抛 CancellationException 是否证明数据库没写？

**推演要点**：不证明，任务或远端操作仍可能已完成写入。

**原理展开**：[JUC深入机制](JUC原理.md#p-13)。

<a id="q-16"></a>
### Q16：complete 会不会被 thenApply 的慢操作拖住？

> **一句话结论**：可能，非 Async 依赖可在完成路径线程执行。

**机制展开**：未完成前驱登记节点，完成时触发；已完成前驱还可能在注册路径直接执行。

**⚖️ 方案对比与代价**：Async 改调度但不自动隔离所有资源，执行器容量和拒绝也须设计。

**🔎 追问演练**：回调是否总按注册先后执行？

**推演要点**：不能把依赖结构与结果语义推广成全部回调的固定顺序保证。

**原理展开**：[JUC深入机制](JUC原理.md#p-14)。

<a id="q-17"></a>
### Q17：complete 返回 false 说明什么？

> **一句话结论**：当前结果已由其他完成路径决定，不说明本次对应的业务副作用没有发生。

**机制展开**：超时、取消、成功竞争同一阶段终态，迟到远端成功仍可能是真实提交。

**⚖️ 方案对比与代价**：结果状态需要与外部事实对账，幂等和清理不能依赖一次完成返回值包办。

**🔎 追问演练**：阶段只完成一次是否说明回调外部写入也全局一次？

**推演要点**：不是，每条工作路径仍有自己的副作用和重试协议。

**原理展开**：[JUC深入机制](JUC原理.md#p-14)。

## 参考资料

- [JUC 包与内存一致性](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/package-summary.html)
- [原子包](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/atomic/package-summary.html)
- [Semaphore](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Semaphore.html)、[Phaser](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/Phaser.html)
- [CompletableFuture](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/CompletableFuture.html)
- [ScopedValue](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ScopedValue.html)、[StructuredTaskScope（预览）](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/StructuredTaskScope.html)
- [LockSupport](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/locks/LockSupport.html)

## 附录：术语与缩写

| 术语 | 英文全称 | 中文含义 |
|---|---|---|
| JDK | Java Development Kit | Java 开发工具包 |
| JVM | Java Virtual Machine | Java 虚拟机 |
| API | Application Programming Interface | 应用程序编程接口 |
| JUC | java.util.concurrent | Java 并发工具包简称 |
| JMM | Java Memory Model | Java 内存模型 |
| CAS | Compare-And-Set | 比较并设置 |
| ABA | A→B→A | 同值往返问题 |
| PECS | Producer Extends, Consumer Super | 泛型上下界口诀，见泛型篇 |
| Scoped Value | Scoped Value | 作用域值 |
| Phaser | Phaser | 多阶段同步器 |
