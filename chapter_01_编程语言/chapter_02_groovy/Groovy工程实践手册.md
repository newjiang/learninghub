# Groovy 工程实践导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

适用范围：Groovy 3/4 的通用语义，Jenkins Pipeline 中的 Groovy 子集另见 Jenkins 系列。目标是维护 DSL、构建脚本和动态扩展，而不是把生产业务全部改写为脚本。

## 1. 类型、闭包与 DSL

`def` 表示编译期不显式声明类型，不等于所有调用都能在运行时成功。动态方法解析经过元对象协议；`@CompileStatic` 能提前检查许多错误并减少动态分派，但依赖运行期元编程的代码可能不兼容。

闭包有三个容易混淆的对象：`this` 是词法外围类实例，`owner` 是外围类或闭包，`delegate` 是可替换的解析对象。DSL 通常设置 delegate 并选择 `DELEGATE_FIRST`，但闭包内局部变量的词法绑定不因此改变。不要通过不受信任的属性名开放任意方法调用。

```groovy
import groovy.transform.CompileStatic

@CompileStatic
class Price {
    static BigDecimal total(List<BigDecimal> prices) {
        prices.inject(0G) { BigDecimal sum, BigDecimal value -> sum + value }
    }
}
assert Price.total([10.20G, 3.30G]) == 13.50G
```

金额显式使用 BigDecimal 并确定舍入策略。`==` 一般比较逻辑相等，身份比较使用 `is`；不要照搬 Java 的 `==` 直觉。

## 2. 容易进入生产的语义陷阱

| 特性 | 能解决什么 | 风险与约束 |
| --- | --- | --- |
| Groovy Truth | 简洁判断集合和字符串 | 数值 0、空集合均可为 false，业务判断要明确 |
| `?.` | 允许接收者为空 | 不能替代必填字段校验，可能把异常推迟到下游 |
| `?:` | 提供默认值 | 会覆盖非 null 的假值，含义不等于仅 null 回退 |
| GString | 模板插值 | 作为 Map key 或跨 Java API 边界时转成 String |
| 动态元编程 | 插件扩展、DSL | 全局修改 MetaClass 会增加并发和测试隔离成本 |

集合操作 `collect/findAll/groupBy` 通常产生中间对象。大文件应逐行处理，避免 `readLines()` 一次读入全部内容。传给 Shell、SQL 的插值字符串仍需参数绑定，DSL 不会自动消除注入。

## 3. 工程边界与故障排查

将动态入口限制在脚本边界，核心计算放到可静态编译、可独立测试的类中。脚本的执行身份、可访问类、依赖来源和最大耗时都需要约束；GroovyShell 不是安全沙箱。

遇到 `MissingMethodException`，先记录实际参数类型及重载列表，再确认 Groovy/Java 版本、类加载器和依赖冲突。遇到 DSL 属性读错，输出 owner/delegate/resolveStrategy，缩小闭包嵌套范围。脚本越来越慢时，分别测启动、编译与执行时间，缓存编译结果时限制大小及类加载器生命周期。

## 4. 面试追问与回答要点

- **动态语言为什么还能静态编译？** 语言语法、类型检查与方法分派策略是不同层次；说明 `@CompileStatic` 的收益和对运行期扩展的限制。
- **DSL 为什么能省略很多限定符？** 解释闭包委托和方法解析，再说明名字冲突如何通过限定接收者消除。
- **Jenkins 中普通 Groovy 能跑，Pipeline 为什么失败？** Pipeline 会进行 CPS 转换和持久化；跨暂停点保留的对象需要满足其恢复要求，不能把所有 Groovy 库当作可直接使用。

## 5. 练习与验收

实现一个只允许设置超时和重试次数的部署 DSL，拒绝负数、未知字段及任意命令。验收：嵌套闭包不误读配置、异常包含字段路径、两次执行互不污染。将相同计算分别以动态模式和静态模式运行，说明测量是否包含编译时间。

## 6. 官方资料

- [Groovy 文档：语义、闭包和类型检查](https://groovy-lang.org/documentation.html)
- [闭包说明](https://groovy-lang.org/closures.html)
