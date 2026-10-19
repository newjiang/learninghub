# React 应用工程导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

基线：React 18+ 函数组件与 Hooks 的通用模型；服务端组件、特定框架路由和构建能力需按所选版本另行核对。

## 1. Render、Commit 与状态快照

组件渲染根据 props、state 和 context 计算 UI；提交阶段才应用相应 DOM 变化。渲染应保持纯净，不能在组件函数内执行支付、订阅或修改全局对象。一次渲染中的事件处理器看到该次渲染的状态快照，setState 不会就地改写闭包中的变量。

依赖前一状态时使用函数更新 `setCount(c => c + 1)`。key 决定列表身份及状态复用，稳定业务 ID 比数组下标更适合重排列表。切换 key 会重建对应子树，也会重置局部状态，不能把它当作无成本刷新技巧。

## 2. Effect 与请求竞态

Effect 用来与外部系统同步；从现有 props/state 能计算出的值，优先在 render 中派生。依赖项缺失会保留旧闭包，滥用空数组并不能修复生命周期问题。

```tsx
useEffect(() => {
  const controller = new AbortController();
  let active = true;
  setError(null);
  fetch(`/api/orders/${encodeURIComponent(id)}`, { signal: controller.signal })
    .then(r => { if (!r.ok) throw new Error(String(r.status)); return r.json(); })
    .then(data => { if (active) setOrder(data); })
    .catch(error => {
      if (active && error.name !== 'AbortError') setError(error);
    });
  return () => { active = false; controller.abort(); };
}, [id]);
```

片段省略 state 声明及 JSON 校验。active 防止已结束生命周期的结果更新，AbortController 尽量终止网络等待。生产项目的缓存、去重、重试、SSR 数据加载通常宜交给统一数据层，避免每个组件自行实现。

## 3. 状态与组件边界

把输入焦点、展开状态等局部 UI 状态留在局部；跨组件共享且稳定的数据可提升或使用 context；服务端数据具有加载、失效及重新获取语义，不等同于全局客户端状态。复杂交互用 reducer 明确事件与状态转换。

不要把所有数据塞进一个 context，任一 value 身份变化可能影响订阅者。useMemo/useCallback 用于有证据的计算及身份稳定需求，不保证业务正确性，也不是“每个函数都要包”。

## 4. 性能、可用性与排查

先用 Profiler 看提交频率和昂贵子树，再检查重复请求、context 扩散和大列表。大列表优先窗口化，长计算可移 Worker；transition 可以降低非紧急更新优先级，但不会减少计算总量或自动取消外部请求。

开发 Strict Mode 的额外检查可能暴露 Effect 清理不对称，不应为隐藏问题而关闭检查。错误边界处理子树渲染错误，但不能默认捕获事件处理器和普通异步回调中的所有错误。SSR hydration 不一致要检查时间、随机数、浏览器专有状态和服务端数据差异。

所有交互具备键盘访问、标签、加载与失败反馈。防重复点击只是体验措施，写接口仍需要服务端幂等。

## 5. 面试与练习

- **状态为什么“没立即更新”？** 从渲染快照、闭包和更新调度解释，不笼统说“异步”。
- **useEffect 与 useLayoutEffect 如何选？** 后者在浏览器重绘前同步执行相关工作，可能阻塞显示，只有布局测量等需求才使用。
- **练习**：做订单搜索列表，包含筛选、分页、请求取消、错误重试和键盘操作。验收：快速切换不显示旧结果，卸载能清理订阅，删除一行不导致其他行输入状态错位。

## 6. 官方资料

- [状态快照](https://react.dev/learn/state-as-a-snapshot)
- [Effect 生命周期](https://react.dev/learn/lifecycle-of-reactive-effects)
- [状态身份与重置](https://react.dev/learn/preserving-and-resetting-state)
