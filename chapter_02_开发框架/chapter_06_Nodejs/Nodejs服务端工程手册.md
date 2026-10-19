# Node.js 服务端工程导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

适用范围：Node.js 22 基线、ESM；不同主版本的模块互操作及事件循环细节需要单独验证。

## 1. 事件循环和工作线程池

JavaScript 通常在事件循环线程执行；部分文件、DNS、加密工作使用 libuv 工作线程池，网络 I/O 常由操作系统异步机制协作。不能把“Node 是单线程”理解为整个进程只有一个线程，也不能把所有异步 API 理解为都进线程池。

Promise 微任务、process.nextTick 及事件循环阶段有各自调度规则。顶层 setTimeout(0) 与 setImmediate 的先后不要作为跨环境业务保证；无限 nextTick 或微任务链会饿死其他工作。

CPU 密集型 JS 使用 worker_threads 或外部计算服务；多进程/多副本提高隔离和多核利用率，但各进程堆不共享，内存缓存、限流及定时任务不能默认全局一致。

## 2. 流和背压

```javascript
import { createReadStream, createWriteStream } from 'node:fs';
import { pipeline } from 'node:stream/promises';

// 独立示例：目标必须不存在，避免覆盖。
await pipeline(
  createReadStream('input.bin'),
  createWriteStream('output.bin', { flags: 'wx' })
);
```

pipeline 协作处理背压和错误销毁，比手动拼接 data 事件更可靠。失败时输出可能是部分文件；正式导出应先写临时文件，成功校验后发布，再定义失败清理与续传策略。highWaterMark 是缓冲阈值，不是整个进程的硬内存上限。

## 3. HTTP 服务边界

入口限制请求体、头部、处理时间和并发；区分客户端断开、上游超时和业务拒绝。异步 handler 的拒绝处理方式与框架主版本有关，不能假定所有框架自动捕获异常。

使用 AsyncLocalStorage 传播追踪上下文，验证第三方库及回调边界是否保留。数据库连接池每个进程各有一份，横向扩容时总连接数约为副本数乘池上限，需要为数据库保留余量。

服务停止先撤销接流，再停止接受新请求，等待在途请求及任务达到截止时间，最后关闭连接池。不能收到信号立即 process.exit，否则日志、响应及消息确认可能丢失。

## 4. 排查与取舍

事件循环延迟高先看长同步计算、JSON 大对象、复杂正则和同步文件 API。堆上限不是总 RSS，Buffer、原生扩展和线程也占内存。堆快照可能暂停进程并增加内存，先在受控副本采样。

遇到异步吞吐不升，分别检查 libuv 线程池、数据库池和远端限流；加大 UV_THREADPOOL_SIZE 只影响部分工作，还可能把压力转移到 CPU 和存储。

## 5. 面试与练习

- **为什么异步代码仍能阻塞？** 回调中的计算仍在主 JS 线程执行；异步只描述等待或接口行为。
- **怎样防止导出接口 OOM？** 数据库流式分页、响应流背压、并发限制、用户取消及文件大小上限一起设计。
- **练习**：提供大文件导出，模拟慢客户端与连接中断。验收：RSS 在预期范围内，不无限缓冲，取消能关闭数据源，关闭服务不无限等待。

## 6. 官方资料

- [不要阻塞事件循环或工作池](https://nodejs.org/en/learn/asynchronous-work/dont-block-the-event-loop)
- [Streams](https://nodejs.org/docs/latest-v22.x/api/stream.html)
- [AsyncLocalStorage](https://nodejs.org/docs/latest-v22.x/api/async_context.html)
