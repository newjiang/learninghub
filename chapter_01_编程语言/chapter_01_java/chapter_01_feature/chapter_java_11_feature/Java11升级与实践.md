# Java 11 升级与实践

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../../../知识手册导航.md)。

Java 11 是本章固定基线，不代表建议所有项目停留在此版本。升级要同时检查 JDK、框架、构建插件、监控 agent 和运行镜像。

## 1. 常用能力及版本辨析

| 能力 | 工程价值 | 边界 |
| --- | --- | --- |
| 标准 HTTP Client | HTTP/2、同步与异步请求 | 配置连接及请求超时，响应体限制由应用负责 |
| String `isBlank/strip/lines/repeat` | 更清晰的文本处理 | strip 与 trim 的空白定义不同 |
| Files `readString/writeString` | 小文本文件操作 | 大文件仍应流式处理 |
| lambda 参数 `var` | 对隐式参数添加注解 | 参数声明方式需一致；不是动态类型 |
| 单文件源代码启动 | 小工具运行 | 不替代正式项目构建 |

局部变量 `var` 来自 Java 10，模块系统来自 Java 9，不能全部归为 Java 11 新特性。Java EE/CORBA 的部分模块在 11 被移除，原先“JDK 自带”的依赖可能要显式添加。

## 2. HTTP 调用示例

```java
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

class HttpDemo {
    public static void main(String[] args) throws Exception {
        var client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();
        var request = HttpRequest.newBuilder(URI.create("https://example.com"))
            .timeout(Duration.ofSeconds(3)).GET().build();
        var result = client.send(request, HttpResponse.BodyHandlers.discarding());
        System.out.println(result.statusCode());
    }
}
```

示例需要外网；状态码非 2xx 不会自动作为 IOException 抛出。真实业务复用 client、校验目标地址、限制重定向与响应体，并区分连接失败、请求超时和业务失败。

## 3. 升级流程

先以目标 JDK 运行现有制品，再用目标 JDK 重新编译，分开暴露运行与编译问题。使用 `jdeps` 识别内部 API 和模块依赖，关注反射访问、TLS、字符集假设及第三方字节码库。通过构建工具的 release 参数约束目标 API，不能只设 source/target 就声称兼容旧运行时。

性能比较固定堆、容器配额、GC、请求分布和预热时间，记录吞吐、P99、CPU 及 RSS。不要把 JDK 变化与同时升级框架、调整机器混在一次对照里。

## 4. 排查与面试

- **升级后类找不到？** 区分已移除模块、依赖遗漏和打包范围，核对运行时 classpath。
- **var 能接收任意类型吗？** 类型在编译期推断，后续赋值仍受静态类型限制。
- **异步 HTTP 为什么仍会耗尽资源？** 异步不等于无限容量，连接、回调执行器、响应体与下游都有上限。

## 5. 练习与验收

迁移一个 Java 8 小服务到 11，形成依赖清单、回归结果、性能对照和回滚说明。验收至少包含反射路径、日期时区、HTTPS 与容器内存测试。

## 6. 官方资料

- [JDK 11 发布说明及 JEP](https://openjdk.org/projects/jdk/11/)
- [Java 11 HTTP Client API](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)
