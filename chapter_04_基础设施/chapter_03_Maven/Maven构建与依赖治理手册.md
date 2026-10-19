# Maven 构建与依赖治理导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

基线：Maven 3.9、Java 项目；插件及 JDK 版本显式固定，Maven 4 的行为差异单独验证。

## 1. 生命周期与项目模型

validate、compile、test、package、verify、install、deploy 属于不同阶段，执行后续阶段会包含前序阶段。goal 是插件提供的具体动作，phase 是生命周期位置；命令能执行不意味着项目正确绑定了集成测试与质量门禁。

POM 继承、聚合和依赖是三种关系。父 POM 管理共性，modules 聚合多个工程，依赖图决定编译/运行关系。dependencyManagement 管版本及默认属性，不会自动引入依赖；pluginManagement 同样不代表所有插件一定执行。

## 2. 依赖解析与冲突

Maven 常按最近定义等规则选择版本，这个结果不保证二进制兼容。定位问题先查看实际 dependency tree 和 effective POM，再看 BOM、直接声明、传递路径与 exclusions。

| scope | 常见用途 | 边界 |
| --- | --- | --- |
| compile | 编译和运行 | 通常会传递 |
| provided | 编译有、运行环境提供 | 打包和运行需验证容器确实提供 |
| runtime | 运行时实现 | 编译期不能直接依赖其 API |
| test | 测试工具 | 不进入正常运行时依赖 |

optional 提示下游不自动传递该依赖，不等于当前工程不用它。排除冲突时确认被排除库是否仍被调用，不能只让构建转绿。

## 3. 可重复构建

```xml
<!-- POM 片段；插件版本也应在完整工程固定。 -->
<properties>
  <maven.compiler.release>17</maven.compiler.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <project.build.outputTimestamp>2026-01-01T00:00:00Z</project.build.outputTimestamp>
</properties>
```

release 约束目标语法、字节码和 API，构建实际 JDK 与插件支持仍需核实。Wrapper 固定 Maven 分发版本；toolchains 可把构建与目标工具链解耦。SNAPSHOT 和动态外部输入会破坏可重复性，发布制品使用不可变版本及来源可追溯记录。

单元测试与集成测试通常分别由 Surefire/Failsafe 承担，集成测试要确保 verify 阶段失败能阻断发布。`-DskipTests` 和 `-Dmaven.test.skip=true` 对测试编译行为不同，不宜常态用来绕过质量门禁。

## 4. 排查与面试

NoSuchMethodError 常见于编译/运行版本不一致，先检查最终制品及运行 classpath，不是先删本地仓库。下载失败区分代理、镜像、证书、仓库权限和 artifact 不存在。CI 与本地不同，检查 effective settings、JDK、profile 和环境变量。

- **BOM 为什么有用？** 统一一组兼容依赖版本，仍需验证与业务引入的其他 BOM 及显式覆盖之间的关系。
- **多模块循环依赖怎么办？** 重审模块边界，抽取稳定接口或合并强耦合模块，不能依赖构建排序解决真实循环。
- **练习**：构造两个不同传递版本，解释最终解析并用依赖约束治理。验收：CI 能发现不允许的冲突；两次干净构建的产物差异可解释；制品可追溯到提交与工具链。

## 5. 官方资料

- [生命周期](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
- [依赖机制](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
- [可重复构建](https://maven.apache.org/guides/mini/guide-reproducible-builds.html)
