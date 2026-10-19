# Java 21 虚拟线程与现代语法

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../../../知识手册导航.md)。

固定基线：JDK 21。虚拟线程、record patterns、模式匹配 switch 在 21 正式提供；结构化并发和 Scoped Values 在 21 仍为预览，不能默认当作稳定 API。

## 1. 虚拟线程解决什么

虚拟线程由 JVM 调度，适合大量等待 I/O 的任务。它降低“一任务一线程”的平台线程成本，但不增加 CPU 算力，也不增加数据库连接数。不要给虚拟线程再套一个固定大小线程池；用每任务一个虚拟线程，并在数据库、远程 API 等稀缺资源入口限制并发。

```java
import java.util.concurrent.*;

class VirtualDemo {
    public static void main(String[] args) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var result = executor.submit(() -> {
                Thread.sleep(100);
                return "done";
            });
            System.out.println(result.get(1, TimeUnit.SECONDS));
        }
    }
}
```

`Future.get` 超时不会自动取消任务；示例的 executor 关闭会等待任务终止，所以这个超时不是整个作用域的硬截止时间。真实请求需要传播 deadline、协作取消并限制下游等待。

## 2. JDK 21 特有边界

在 21 中，某些 synchronized 或 native 路径上的阻塞可能 pin 住载体线程，降低可扩展性；不要把这个结论无版本限制地推广到后续 JDK。使用 JFR 和相关诊断选项寻找实际阻塞，再缩小锁区间或在适用场景使用其他锁。

ThreadLocal 仍可使用，但百万级线程各持有大对象会放大内存。原先在线程池线程上缓存连接等资源的技巧不适合直接搬到虚拟线程。日志 MDC、链路上下文及代理客户端要做兼容性验证。

## 3. 现代模式匹配

```java
record Point(int x, int y) {}
class PatternDemo {
    static String label(Object value) {
        return switch (value) {
            case null -> "empty";
            case Point(int x, int y) -> x + "," + y;
            default -> "other";
        };
    }
}
```

优先将有限业务状态建模为 sealed 层次，再用穷尽 switch 暴露遗漏。模式匹配改善表达，不替代输入校验和合理的领域边界。

## 4. 容量与排查

并发近似等于吞吐乘平均在系统时间。例如 2,000 请求/秒、平均 0.2 秒，稳定态平均约 400 个在途请求；尾延迟和突发还要额外预算。线程便宜后，瓶颈常转移到连接池、堆、限流和下游队列。

对照试验分别记录平台线程、虚拟线程的吞吐、P99、数据库等待、CPU、RSS 及取消完成时间。若吞吐不升，先判断是否 CPU 饱和或数据库限额，而不是继续增加任务数。

## 5. 面试与练习

- **虚拟线程能替代所有异步模型吗？** 适合以阻塞式代码处理高并发 I/O；流式背压、事件处理和已有响应式库仍有自身价值。
- **为何不能拿线程数估算数据库并发？** 连接池和数据库容量才是稀缺资源，要独立限额。
- **练习**：用固定延时的本地假下游做平台/虚拟线程对照，增加连接池上限模拟。验收：过载可拒绝、超时可取消、队列有界，并解释性能拐点。

## 6. 官方资料

- [Virtual Threads：JEP 444](https://openjdk.org/jeps/444)
- [JDK 21 特性清单](https://openjdk.org/projects/jdk/21/)
