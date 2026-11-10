# 反射Reflection面试：从结论到机制与场景推演

> **使用方式**：先用一句话回答，再展开实现、前提和代价。遇到版本问题先说明目标 JDK（Java Development Kit，Java 开发工具包）。配套 [反射Reflection原理](反射Reflection原理.md)。

## 题目导航

- [Q1：getMethod 与 getDeclaredMethod 区别？](#q-01)
- [Q2：反射调用的异常如何处理？](#q-02)
- [Q3：setAccessible 能访问一切 private 吗？](#q-03)
- [Q4：反射能随意修改 final 吗？](#q-04)
- [Q5：反射一定先慢调用再膨胀吗？](#q-05)
- [Q6：MethodHandle 比反射好在哪里？](#q-06)
- [Q7：VarHandle 与 Field 有何区别？](#q-07)
- [Q8：反射缓存为何造成类加载器泄漏？](#q-08)
- [Q9：反射扫描需过滤哪些成员？](#q-09)
- [Q10：AOT 下反射为什么失败？](#q-10)
- [Q11：invokeExact 为什么连返回值强转都重要？](#q-11)
- [Q12：反射框架只缓存 Method 为什么还不够？](#q-12)
- [Q13：ClassValue 的 computeValue 保证只运行一次吗？](#q-13)
- [Q14：拿到 Method 后 unreflect 就能无视模块吗？](#q-14)
- [Q15：怎样公平比较反射与 MethodHandle 性能？](#q-15)
- [Q16：反射传递一个 String[] 为什么可能被报参数数量不匹配？](#q-16)

<a id="q-01"></a>
### Q1：getMethod 与 getDeclaredMethod 区别？

> **一句话结论**：前者查 public 且含继承，后者查当前声明且可包含非 public。

**机制展开**：Declared 不是开放权限，也不自动扫描父类私有方法；精确参数签名仍要匹配。

**⚖️ 方案对比与代价**：框架应明确成员层次规则，避免把遍历顺序当覆盖优先级。

**🔎 追问演练**：用 Integer.class 能找到 int 参数吗？

**推演要点**：查询不做源码重载选择，要用 int.class；调用适配是后续阶段。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-02)。

<a id="q-02"></a>
### Q2：反射调用的异常如何处理？

> **一句话结论**：区分适配/访问失败与目标业务异常，后者从 InvocationTargetException 取 cause。

**机制展开**：invoke 包装目标抛错，参数错误和访问错误走其他异常类型。

**⚖️ 方案对比与代价**：统一记录时保留上下文，但不能把业务异常错映射成基础设施故障。

**🔎 追问演练**：代理转发为什么出现 UndeclaredThrowableException？

**推演要点**：先检查包装异常有没有解包，以及它是否符合接口声明的受检异常。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-03)。

<a id="q-03"></a>
### Q3：setAccessible 能访问一切 private 吗？

> **一句话结论**：不能，模块封装和成员类别有额外限制。

**机制展开**：exports 与 opens 不同，普通公共访问与深反射要分别判断；trySetAccessible 可返回失败。

**⚖️ 方案对比与代价**：优先公开契约或最小开放，而非依赖内部平台成员。

**🔎 追问演练**：升级 JDK 后报 InaccessibleObjectException 是缺 jar 吗？

**推演要点**：通常先看模块与包开放关系，而非类查找。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-05)。

<a id="q-04"></a>
### Q4：反射能随意修改 final 吗？

> **一句话结论**：不能作为通用保证，更不能用于稳定配置设计。

**机制展开**：不同字段类别有不可修改限制，final 语义还涉及构造、安全发布和优化。

**⚖️ 方案对比与代价**：显式构造器与不可变对象替换比事后改 final 更可控。

**🔎 追问演练**：反射赋完字段再发布对象需要考虑什么？

**推演要点**：构造不变量、异常中间状态与同步发布，不能只看赋值是否没抛错。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-04)。

<a id="q-05"></a>
### Q5：反射一定先慢调用再膨胀吗？

> **一句话结论**：不是，JDK 18 起核心反射实现已发生重要变化。

**机制展开**：历史访问器机制与现代基于方法句柄的实现不同，接口语义仍需稳定。

**⚖️ 方案对比与代价**：性能测量要指定 JDK、预热、缓存与调用形态，不能照抄旧倍率。

**🔎 追问演练**：缓存 Method 后还慢的原因？

**推演要点**：可能是装箱、参数适配、目标不稳定或业务本身，分阶段观察。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-07)。

<a id="q-06"></a>
### Q6：MethodHandle 比反射好在哪里？

> **一句话结论**：它适合类型化、组合与适配调用链，但没有无条件性能优势。

**机制展开**：Lookup 创建句柄时检查访问能力，invokeExact 依调用点精确描述，invoke 允许规定适配。

**⚖️ 方案对比与代价**：类型稳定有利优化，复杂动态适配也有成本；不能泄漏私有 Lookup 能力。

**🔎 追问演练**：返回值只改成 Object 为何 invokeExact 失败？

**推演要点**：调用点返回类型也是精确签名的一部分，需要一致或显式适配。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-06)。

<a id="q-07"></a>
### Q7：VarHandle 与 Field 有何区别？

> **一句话结论**：前者提供变量访问与原子内存模式，后者偏向反射字段元数据与读写。

**机制展开**：acquire/release、volatile、CAS 等有不同顺序和原子性语义。

**⚖️ 方案对比与代价**：更弱模式可能更轻，但必须证明同步关系，不能随机挑一个更快名字。

**🔎 追问演练**：拿到 VarHandle 后普通 get 能替代 volatile get 吗？

**推演要点**：不能等同，具体访问模式决定所需可见性与顺序。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-06)。

<a id="q-08"></a>
### Q8：反射缓存为何造成类加载器泄漏？

> **一句话结论**：Method、Class 和句柄都可能强引用定义加载器。

**机制展开**：全局缓存长寿命根保留旧插件类型，按名字缓存还会混淆同名类型。

**⚖️ 方案对比与代价**：ClassValue 或作用域缓存可减轻风险，但值和外部注册也需检查。

**🔎 追问演练**：弱键 Map 的值引用键就一定释放吗？

**推演要点**：不一定，值的强引用可能重新保留类型或加载器，要画完整可达链。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-07)。

<a id="q-09"></a>
### Q9：反射扫描需过滤哪些成员？

> **一句话结论**：按业务语义处理桥接、合成、继承覆盖与记录成员，而非简单全收集。

**机制展开**：编译器会补方法与字段，反射返回顺序不构成业务优先级。

**⚖️ 方案对比与代价**：过度过滤也可能漏必要入口，需解析实际方法而不只按名字去重。

**🔎 追问演练**：参数名为什么是 arg0？

**推演要点**：编译产物未保留预期参数名称，检查编译选项和框架的名称来源。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-08)。

<a id="q-10"></a>
### Q10：AOT 下反射为什么失败？

> **一句话结论**：闭世界工具需要知道可达元数据，动态名称可能超出静态分析。

**机制展开**：运行期找类、查成员的路径要按工具要求注册或生成。

**⚖️ 方案对比与代价**：生成适配能减少动态依赖，但构建成本与扩展能力需要权衡。

**🔎 追问演练**：JVM 测试全过能直接保证本地镜像吗？

**推演要点**：不能，必须在目标产物中验证动态入口和配置；这是工具约束，不是反射语言契约改变。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-08)。

<a id="q-11"></a>
### Q11：invokeExact 为什么连返回值强转都重要？

> **一句话结论**：返回类型属于调用点精确描述，Object 和 String 的描述并不相同。

**机制展开**：虚方法句柄首参数还包含接收者，完整类型如 (String,int)String。

**⚖️ 方案对比与代价**：invoke 或 asType 可提供规定适配，但会改变精确调用的匹配规则。

**🔎 追问演练**：findVirtual 只写了 int 参数，调用为什么还传 String？

**推演要点**：查找签名不写接收者，得到的虚调用句柄却需要首个接收者参数。

**原理展开**：[反射Reflection原理对应章节](反射Reflection原理.md#p-09)。

<a id="q-12"></a>
### Q12：反射框架只缓存 Method 为什么还不够？

> **一句话结论**：方法定位之外还有上下文泛型、参数转换、访问和异常策略需要保持一致。

**机制展开**：构建不可变调用计划并以真实类型身份缓存，避免热路径重复扫描和重载猜测。

**⚖️ 方案对比与代价**：提前校验降低运行期歧义，但按需插件也应保留清楚的延迟失败诊断。

**🔎 追问演练**：只按方法名作缓存键有什么问题？

**推演要点**：重载、不同目标实现和不同加载器都可能同名却需要不同计划。

**原理展开**：[反射Reflection深入机制](反射Reflection原理.md#p-10)。

<a id="q-13"></a>
### Q13：ClassValue 的 computeValue 保证只运行一次吗？

> **一句话结论**：不能把安装一个结果误解成竞争下求值函数永远一次。

**机制展开**：竞争可能产生多次计算，计算应无不可重复副作用；缓存值还可能保留其他加载域。

**⚖️ 方案对比与代价**：比字符串全局表更贴近类型生命周期，但外部强引用仍需管理。

**🔎 追问演练**：用了 ClassValue 为何插件仍泄漏？

**推演要点**：检查计划是否被另一个永久注册表保存，或值引用了长寿命外部对象图。

**原理展开**：[反射Reflection深入机制](反射Reflection原理.md#p-10)。

<a id="q-14"></a>
### Q14：拿到 Method 后 unreflect 就能无视模块吗？

> **一句话结论**：不能，它仍受 Lookup 与目标成员的访问规则约束。

**机制展开**：句柄创建和能力获取有边界，绑定接收者或 asType 不负责替用户获得业务权限。

**⚖️ 方案对比与代价**：显式句柄有利组合与稳定调用，但能力对象不能随意泄漏。

**🔎 追问演练**：给插件一个私有句柄会怎样？

**推演要点**：插件获得相应调用能力，不能假定每次调用会重新按它最初权限阻止。

**原理展开**：[反射Reflection深入机制](反射Reflection原理.md#p-11)。

<a id="q-15"></a>
### Q15：怎样公平比较反射与 MethodHandle 性能？

> **一句话结论**：让查询缓存、目标稳定性、适配与预热条件一致，并分开目标业务开销。

**机制展开**：重复查找与一次缓存的比较测到的不只是调用机制，签名稳定性也影响优化。

**⚖️ 方案对比与代价**：性能是场景结论，语义和访问能力先满足，再选择实现。

**🔎 追问演练**：换成 MethodHandle 就不用 AOT 动态配置了吗？

**推演要点**：不能泛化，工具是否能分析并保留入口仍需实际验证。

**原理展开**：[反射Reflection深入机制](反射Reflection原理.md#p-11)。

<a id="q-16"></a>
### Q16：反射传递一个 String[] 为什么可能被报参数数量不匹配？

> **一句话结论**：目标数组参数与 Method.invoke 的 Object... 参数列表是两层结构，必须明确哪一层代表什么。

**机制展开**：目标 varargs 在运行期通常仍是数组参数。将 String[] 明确作为一个 Object 传入，可避免被误作为反射参数列表展开。查找则仍需用 String[].class，不是 String.class。

**⚖️ 方案对比与代价**：通用适配器方便动态调用，却必须独立定义数组、null、基本类型与异常规则；源码调用的便利不能全部假定反射自动完成。

**🔎 追问演练**：getDeclaredMethod 得到父类方法，在子类对象调用一定执行父实现吗？

**推演要点**：普通虚调用仍尊重覆写；声明定位和执行分派分开，特殊父调用需相应机制和权限。

**原理展开**：[反射 varargs、转换与虚分派：把查找和执行拆开验证](反射Reflection原理.md#p-12)。

## 参考资料

- [Method](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/Method.html)、[AccessibleObject](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/reflect/AccessibleObject.html)
- [MethodHandle](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/invoke/MethodHandle.html)、[Lookup](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/invoke/MethodHandles.Lookup.html)、[VarHandle](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/invoke/VarHandle.html)
- [JDK 18 重要变化：JEP 416](https://docs.oracle.com/en/java/javase/18/migrate/significant-changes-jdk-release.html)
- [ClassValue](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ClassValue.html)

## 附录：术语与缩写

| 术语 | 英文全称 | 中文含义 |
|---|---|---|
| JDK | Java Development Kit | Java 开发工具包 |
| JVM | Java Virtual Machine | Java 虚拟机 |
| API | Application Programming Interface | 应用程序编程接口 |
| JPMS | Java Platform Module System | Java 平台模块系统 |
| JEP | JDK Enhancement Proposal | JDK 增强提案 |
| JIT | Just-In-Time Compilation | 即时编译 |
| AOT | Ahead-of-Time Compilation | 提前编译 |
| Lookup | MethodHandles.Lookup | 带访问能力的句柄查找对象 |
| Inflation | Reflection Inflation | 历史反射访问器膨胀机制 |
