# JavaScript 语言与异步导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

适用范围：现代浏览器 ES Modules；Node.js 的任务调度和服务端资源管理见 Node.js 系列。资深工程师要把语言语义与宿主环境分开解释。

## 1. 值、作用域和对象模型

原始值按值使用，对象赋值复制的是引用。`const` 限制绑定重新赋值，不能冻结对象。展开语法只是浅复制，嵌套对象仍共享。跨边界的数据用显式 DTO，不要依赖 JSON 序列化实现通用深克隆：它会丢失 undefined、函数和部分类型信息，并拒绝循环引用。

`let/const` 存在词法作用域及暂时性死区；闭包保留对词法环境的访问，所以循环变量声明、事件监听器和定时器可能影响行为及内存生命周期。普通函数的 `this` 取决于调用方式，箭头函数捕获外部 `this`；`bind` 不会重写箭头函数的词法绑定。

原型链负责属性查找，class 提供类语法但不改变其基本机制。区分自有属性与继承属性；处理外部 key 时优先 Map，或使用无原型对象，避免不受控的对象合并引入原型污染。

## 2. 事件循环与 Promise

浏览器执行一个任务后，通常执行微任务检查点，再进入后续渲染机会及任务。Promise 回调和 `queueMicrotask` 属于微任务；定时器的延时是允许执行的下限，不是准时执行保证。持续添加微任务会推迟渲染和后续任务。

```javascript
console.log('A');
setTimeout(() => console.log('D'), 0);
Promise.resolve().then(() => console.log('C'));
console.log('B');
// 浏览器中：A B C D
```

`async` 函数总是返回 Promise。`await` 挂起该函数后续执行，不会把 CPU 密集计算变为后台任务。重计算放 Web Worker；大量异步 I/O 使用并发上限，避免 `Promise.all` 一次启动几万请求。`Promise.all` 提前拒绝不会取消其他已启动操作。

## 3. 工作场景：取消请求与竞态

```javascript
// 浏览器示例；调用方将用户切换、组件卸载等取消信号传入。
export async function loadOrder(id, signal) {
  const response = await fetch(`/api/orders/${encodeURIComponent(id)}`, { signal });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json(); // 外部 JSON 仍需要结构校验
}
```

搜索框中“旧请求后返回覆盖新结果”需要取消旧请求，并用请求序号校验当前结果；取消可能与完成竞争，不能只靠取消 API 判断时序。对写请求，客户端取消不等于服务端回滚，必须使用幂等键和可查询的业务状态。

## 4. 性能、模块与排查

ESM 的 import/export 建立模块依赖，导出绑定具有 live binding 语义。顶层副作用会影响 tree shaking，循环依赖容易在初始化时读取未就绪绑定。构建结果能否裁剪还取决于打包器和包声明，不能仅凭“用了 ESM”下结论。

页面卡顿先在 Performance 中定位长任务，再区分脚本、样式计算、布局和绘制；内存增长用堆快照比较保留路径，重点看脱离 DOM、全局缓存、监听器和闭包。优化前记录页面、设备、数据量和分位耗时，避免只报告本机平均值。

## 5. 面试追问与回答要点

- **闭包是否必然泄漏？** 不会；仍被可达对象引用且超出预期生命周期时才会形成不必要保留。举出未移除监听器的例子。
- **防抖与节流如何选？** 防抖适合等待输入稳定，节流适合限制连续事件频率；补充首尾触发、取消、卸载和参数保留。
- **Promise 与多线程是什么关系？** Promise 是结果及回调的抽象，并不意味着新线程；具体 I/O 与线程由宿主实现。
- **为什么金额不用浮点直接算？** 二进制浮点不能精确表示许多十进制小数；按币种最小单位或十进制定点计算，并定义舍入规则。

## 6. 练习与验收

实现并发数为 4 的请求队列，支持单项失败和整体取消。验收：任何时刻在途数不超过 4；取消后不再启动新任务；已启动写操作的结果可查询；不存在未处理的拒绝。解释为什么 `forEach(async () => ...)` 不能让外围等待所有任务。

## 7. 资料

- [ECMAScript 规范](https://tc39.es/ecma262/)
- [HTML 事件循环规范](https://html.spec.whatwg.org/multipage/webappapis.html#event-loops)
- [MDN JavaScript 指南](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide)
