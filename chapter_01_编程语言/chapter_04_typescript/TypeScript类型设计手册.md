# TypeScript 类型设计导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

适用范围：TypeScript 5.x，示例开启 `strict`。类型系统的目标是提前暴露错误，运行期信任边界仍要校验。

## 1. 类型建模从业务约束开始

结构类型按成员兼容性判断，不按类名判断。`any` 会绕过许多检查，`unknown` 要求先收窄才能使用，`never` 表示不可能到达的类型。类型断言 `as` 不会转换或验证运行时数据。

```typescript
type Payment =
  | { status: 'pending'; orderId: string }
  | { status: 'paid'; orderId: string; transactionId: string }
  | { status: 'failed'; orderId: string; reason: string };

function assertNever(value: never): never {
  throw new Error(`Unexpected payment: ${JSON.stringify(value)}`);
}
function describe(payment: Payment): string {
  switch (payment.status) {
    case 'pending': return '处理中';
    case 'paid': return payment.transactionId;
    case 'failed': return payment.reason;
    default: return assertNever(payment);
  }
}
```

相比 `paid?: boolean; transactionId?: string`，判别联合消除了“已支付却没有流水号”的部分非法组合。外部 JSON 必须先经过运行时 schema 校验，才能声称其为 Payment。

## 2. 泛型与高级类型的使用边界

泛型应表达输入输出之间的关系，例如 `get<T, K extends keyof T>(obj: T, key: K): T[K]`。如果泛型只出现一次，往往没有提供有用约束。`Partial` 适合更新结构，但业务更新可能禁止修改 ID，需用 `Pick/Omit` 明确允许字段。

条件类型可根据类型分支；裸类型参数上的条件类型会对联合分配，`[T] extends [U]` 可避免这种分配。映射类型适合系统性变换，递归类型要考虑编译速度和错误可读性，不要为减少一行重复创造无法调试的类型谜题。

`satisfies` 检查表达式是否满足目标类型，同时保留表达式的推断结果；`as const` 更偏向字面量和 readonly 推断。readonly 只提供静态约束，不等于运行时不可变，更不自动递归冻结对象。

## 3. 工程配置与跨端契约

建议在可控增量下启用 `strict`、`noUncheckedIndexedAccess` 和 `exactOptionalPropertyTypes`，分别处理未知索引、缺失字段与显式 undefined 的差异。`target` 控制语法输出；`lib` 声明可用 API 的类型，并不自动注入 polyfill。模块解析配置必须与 Node、浏览器或打包器匹配。

前后端共享类型只能共享编译期描述，不能替代 HTTP 契约和运行时校验。基于 OpenAPI/schema 生成客户端时，将生成结果纳入版本管理策略，CI 检查破坏性字段变化。错误结果也要建模，避免把网络异常、业务拒绝和登录过期都归为一个 boolean。

## 4. 排查路线

类型报错先找最小调用点，再看实际推断、联合类型中未收窄的分支和泛型约束。不要先加 `as any`。编译突然变慢时开启编译器诊断，区分文件数、依赖声明和复杂递归实例化；大型仓库考虑 project references，先验证增量构建收益。

运行正常而类型失败，检查第三方声明版本和模块解析；类型通过而运行失败，检查外部输入、断言、非空断言及编译目标缺少的运行时 API。

## 5. 面试追问与回答要点

- **interface 和 type 怎么选？** 接口适合对象契约与声明合并；别名可表达联合、元组等。重点在可扩展边界和团队一致性。
- **为什么泛型不能验证接口 JSON？** 编译后类型被擦除，运行期需要解析与校验逻辑。
- **可选字段和 `T | undefined` 一样吗？** 前者允许属性不存在；相关赋值行为受 exactOptionalPropertyTypes 影响。
- **协变逆变有什么工程价值？** 从函数参数替换安全性解释，联系 strictFunctionTypes；注意方法签名等检查规则的例外。

## 6. 练习与验收

为订单状态机设计类型：只有待支付订单可取消，只有已支付订单可退款。验收：编译阶段拒绝不合法调用，接口边界拒绝无效 JSON，新增一种状态后穷尽检查能提示遗漏处理。

## 7. 官方资料

- [类型收窄](https://www.typescriptlang.org/docs/handbook/2/narrowing.html)
- [泛型](https://www.typescriptlang.org/docs/handbook/2/generics.html)
- [TSConfig](https://www.typescriptlang.org/tsconfig/)
