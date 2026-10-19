# Java 17 语言建模与迁移

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../../../知识手册导航.md)。

本章以 JDK 17 正式特性为基线，包含从 11 升级时可用的增量能力，不把它们都称作“17 首次引入”。

## 1. 特性与适用场景

| 特性 | 正式引入版本 | 使用边界 |
| --- | --- | --- |
| switch 表达式 | 14 | 与旧 switch 语句的穿透行为区分 |
| 文本块 | 15 | 不会自动防 SQL 注入，缩进和末尾换行需要验证 |
| record | 16 | 浅不可变的数据载体，不适合直接替代所有 ORM 实体 |
| instanceof 模式匹配 | 16 | 简化类型检查后的转换 |
| sealed class/interface | 17 | 对可扩展子类型集合建模 |

Java 17 的模式匹配 switch 仍是预览，不能把 21 的正式写法直接复制到 17 工程。预览特性需要编译及运行同时启用，并锁定对应 JDK 版本。

## 2. 用类型约束业务

```java
import java.util.List;

sealed interface Result permits Success, Failure {}
record Success(List<String> ids) implements Result {
    Success { ids = List.copyOf(ids); }
}
record Failure(String code, String message) implements Result {}
```

record 生成访问器、equals/hashCode/toString 等。组件为可变 List 时，调用者仍可能修改其内容，所以用防御性拷贝约束容器；容器中的可变对象还需进一步处理。`List.copyOf` 不接受 null 元素，也不等于深拷贝。

sealed 控制继承集合，在业务边界稳定时有帮助；插件体系需要第三方自由扩展时，封闭层次可能增加耦合。实体是否使用 record 要考虑 ORM 代理、标识语义、生命周期和框架支持。

## 3. 强封装与迁移

JDK 内部 API 的强封装会让旧反射路径失败。定位到具体库后优先升级依赖，临时 `--add-opens` 应记录用途和移除期限；广泛开放所有包会掩盖真正兼容性问题。

检查字节码处理库、序列化库、测试框架、监控 agent、Maven/Gradle 插件及容器镜像。构建 JDK 和运行 JDK 一致只是起点，还需测试业务协议、数据库驱动和 TLS 行为。

## 4. 排查与面试

- **record 是否不可变？** 字段绑定不可重赋值，引用目标未必不可变；解释防御性复制和深层对象的处理。
- **sealed 比 enum 多什么？** 各子类型能有不同数据形状和行为；enum 适合有限常量集合，两者不是替代关系。
- **为何运行失败但编译成功？** 编译期和运行期可能使用不同 JDK、依赖、模块开放参数或预览选项。

## 5. 练习与验收

将混合 boolean、nullable 字段的接口结果改成封闭结果类型。验收：成功与失败数据不混用；外部序列化兼容性有用例；原可变列表变更不会改写已创建的结果。

## 6. 官方资料

- [JDK 17](https://openjdk.org/projects/jdk/17/)
- [Records：JEP 395](https://openjdk.org/jeps/395)
- [Sealed Classes：JEP 409](https://openjdk.org/jeps/409)
