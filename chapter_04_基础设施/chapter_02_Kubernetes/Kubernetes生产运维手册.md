# Kubernetes 生产运维导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

基线：稳定的 apps/v1 Deployment、v1 Service 等 API；集群版本、Ingress/Gateway、CNI 和云负载均衡器按部署环境核实。原有课程笔记用于概念入门，本章用于上线与排障。

## 1. 声明式控制与工作负载

用户声明期望状态，控制器通过 reconcile 逐步逼近。API 写入成功不代表 Pod 已就绪。Deployment 管理无状态副本滚动发布；StatefulSet 提供稳定身份和存储关联，不自动提供数据库复制、一致性或备份。

Pod 是调度单元，共享网络命名空间；同 Pod 容器可经 localhost 通信。Service 提供稳定访问入口，实际数据路径由实现决定，不应把 kube-proxy iptables 当作所有集群唯一方案。

## 2. 探针与资源

```yaml
# Deployment 的容器配置片段，端点需由应用实现。
resources:
  requests:
    cpu: "250m"
    memory: "512Mi"
  limits:
    cpu: "1"
    memory: "1Gi"
startupProbe:
  httpGet: {path: /health/startup, port: 8080}
  periodSeconds: 5
  failureThreshold: 30
readinessProbe:
  httpGet: {path: /health/ready, port: 8080}
  periodSeconds: 5
livenessProbe:
  httpGet: {path: /health/live, port: 8080}
  periodSeconds: 10
```

startupProbe 成功前抑制其他两类探针；readiness 决定是否接流，liveness 失败可能导致重启。共享数据库暂时故障不宜让所有实例 liveness 同时失败重启，探针语义要避免放大事故。

request 影响调度和部分资源分配，limit 约束消耗；CPU 限制可能引发 throttling，内存超限可能触发 OOM。上例数字仅示意，必须由压测和运行基线确定。HPA 的 CPU 利用率与 request 相关，request 设置会影响扩容判断。

## 3. 发布、退出与配置

滚动发布需要 readiness、maxSurge/maxUnavailable、容量余量以及数据库兼容。Pod 收到终止流程后，入口摘流和应用停止存在传播时间；应用停止接新任务、等待在途处理并设置终止宽限。preStop 与退出等待共同消耗宽限时间。

PodDisruptionBudget 约束部分自愿驱逐，不保证硬件故障时可用，也不是 Deployment 滚动策略的替代。多副本若全在一个节点或可用区，仍会共享故障域，应配置合适的分布约束。

Secret 的 base64 不是加密，应结合存储加密、RBAC、外部密钥管理和审计。ConfigMap/Secret 的更新是否被应用感知，取决于挂载方式和应用刷新逻辑，不能假定环境变量自动热更新。

## 4. 排障顺序

```text
Pending：describe/events → 配额、资源、亲和性、PVC
CrashLoopBackOff：上次容器日志 → 退出原因 → 启动配置和依赖
Running 但无流量：readiness → Service selector → EndpointSlice → 端口
跨节点不通：DNS → NetworkPolicy → CNI/路由 → 节点网络
延迟抖动：CPU throttling、GC、连接池、下游和网络
```

先记录 namespace、Pod UID、镜像 digest、节点和时间线，再操作。不要以删 Pod 作为固定排障第一步，会丢掉证据且不解决资源/配置根因。

## 5. 面试与练习

- **三类探针为什么要分开？** 分别回答是否完成启动、能否接新流量、是否需重启，避免把依赖健康与进程存活混为一谈。
- **自动扩容能解决所有流量增长吗？** 受冷启动、最大副本、集群容量及数据库瓶颈限制，仍需过载保护。
- **练习**：发布三个副本，模拟慢启动、依赖不可用、节点驱逐和滚动升级。验收：不因共享依赖故障反复重启；接流与退出行为符合预期；能解释每次失败事件。

## 6. 官方资料

- [探针](https://kubernetes.io/docs/concepts/workloads/pods/probes/)
- [资源管理](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/)
- [Pod 生命周期](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/)
