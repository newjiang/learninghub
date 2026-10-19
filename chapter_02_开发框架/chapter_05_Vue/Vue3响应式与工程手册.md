# Vue 3 响应式与工程导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

基线：Vue 3 Composition API、TypeScript、Vue Router 4、Pinia。小版本特有的编译宏行为需按项目版本核实。

## 1. 响应式原理与语义

reactive 借助 Proxy 追踪属性访问与更新；ref 通过 value 提供响应式值容器。依赖收集与触发更新需要经过相应访问路径，直接解构 reactive 的原始值可能失去跟踪，应按场景使用 toRefs 或 getter。

computed 缓存依赖计算结果，适合纯派生数据；watch 指定来源观察变化并执行副作用；watchEffect 自动追踪同步执行阶段读到的依赖，异步函数在首个 await 后读取的值不会因此自动全被追踪。

```vue
<script setup lang="ts">
import { ref, computed } from 'vue';
const quantity = ref(1);
const unitPrice = ref(1200); // 单位：分，真实业务需确定币种
const total = computed(() => quantity.value * unitPrice.value);
</script>

<template>
  <label>数量 <input v-model.number="quantity" type="number" min="1" /></label>
  <output>{{ total }} 分</output>
</template>
```

模板中的解包规则与普通 JS 访问不同，复杂嵌套集合不能一概认为都会解包。输入的 min 是体验约束，服务端仍需验证整数、上限和库存。

## 2. 组件与组合式函数

组件使用 props 下传、事件上传，避免直接修改 props 或隐式改变父对象。composable 封装可复用的状态及副作用，命名和返回结构保持清晰；订阅、定时器与请求必须能在生命周期结束时清理。

Pinia 放跨页面共享的客户端状态，服务端数据仍需定义缓存失效和刷新策略。SSR 中按请求创建应用及 store，避免多个用户共享进程级可变数据。

路由守卫可控制导航体验，但接口必须独立鉴权。懒加载路由减少初始脚本，拆分粒度还需结合网络延迟和用户路径，不能无限细分。

## 3. 更新调度与性能

状态变化后的 DOM 更新会批处理，需要读取更新后 DOM 时使用 nextTick；它不保证图片已加载或浏览器已完成所有绘制。深层 watch 成本与对象规模有关，优先观察具体字段。大规模只读结构可考虑 shallowRef，更新时显式替换引用并验证视图一致性。

列表 key 使用稳定 ID；v-if 切换会销毁重建，v-show 保留节点并切换显示，取决于频率与初始成本。KeepAlive 缓存实例仍占内存，注意 activated/deactivated 的资源管理和缓存大小。

## 4. 故障排查

| 现象 | 先检查 | 修复方向 |
| --- | --- | --- |
| 数据变了视图不变 | 是否仍经过代理、是否解构失联 | 使用响应式来源和正确替换方式 |
| 请求重复或循环 | watch 来源、回调是否反写依赖 | 分离派生数据与副作用，减少深监视 |
| 页面切换仍收事件 | 监听器、缓存组件生命周期 | 在对应清理点退订和取消 |
| SSR 数据串用户 | 模块级 store 和缓存 key | 每请求隔离，缓存包含租户与权限边界 |

## 5. 面试与练习

- **Vue 和 React 状态模型差异？** 说明依赖追踪与渲染快照等机制，但避免宣称某一方天然更快；真实性能由数据流和工作负载决定。
- **computed 为什么不放请求？** 派生计算需要可预测，异步副作用应独立管理加载、错误和取消。
- **练习**：实现可复用搜索 composable，支持取消和缓存失效。验收：卸载后不更新、旧请求不覆盖新请求、缓存不跨租户复用。

## 6. 官方资料

- [响应式基础](https://vuejs.org/guide/essentials/reactivity-fundamentals.html)
- [侦听器](https://vuejs.org/guide/essentials/watchers.html)
- [Pinia](https://pinia.vuejs.org/)
