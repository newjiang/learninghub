# Nginx 网关与代理导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

基线：开源 Nginx HTTP 反向代理，动态 DNS、健康检查和商业功能必须核对具体版本/发行版。

## 1. 请求处理与配置匹配

master 管理配置与 worker，worker 基于事件驱动处理多个连接。worker 数量不是可用容量的唯一因素，文件描述符、上游连接、TLS、带宽和缓冲都可能先成为瓶颈。

server 根据监听端口及主机等规则匹配，location 包含精确、前缀和正则等匹配规则。proxy_pass 是否带 URI 会影响路径重写语义，末尾斜线的差别应通过表驱动请求验证，而不是凭记忆部署。

```nginx
# http 块内片段；仅用于本地代理练习，公网需补 TLS 和访问控制。
upstream orders_api {
    server 127.0.0.1:8080;
    keepalive 32;
}
server {
    listen 8081;
    server_name localhost;
    location /api/ {
        proxy_pass http://orders_api; # 保留 /api/ 路径
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Request-ID $request_id;
        proxy_connect_timeout 2s;
        proxy_read_timeout 10s;
    }
}
```

read_timeout 通常约束两次读操作间隔，不等于整个响应总耗时；总 deadline 需要上层配合。X-Forwarded-For 的信任来源要按代理链配置，不能直接相信客户端提交的头部。

## 2. 负载均衡、重试与缓存

轮询适合一般无状态服务，least_conn 对长短请求混合可能更有帮助，hash 需要理解节点变化对分布的影响。会话状态优先外置，不能把粘性会话当作所有状态问题的解法。

上游重试可能重复写入。对支付、下单等请求，明确何时能重试并由业务幂等保证结果；502/504 不能证明上游没有执行。缓存 key 要包含会影响内容的维度，个性化与授权内容默认不应被公共缓存共享。

## 3. 超时、长连接与流式响应

WebSocket 升级需要正确转发 Upgrade/Connection 语义，通常用 map 区分普通与升级请求。SSE 需检查代理缓冲、应用 flush 和空闲超时，否则应用已发送而用户仍看不到事件。关闭缓冲会改变慢客户端对上游的影响，需要并发及带宽预算。

限流区分平均速率、突发和并发连接数，多副本网关的本地限额不自动成为全局限额。上传限制同时考虑网关和应用，错误响应需保持可识别。

## 4. 排查与面试

日志同时记录 request_time、upstream_response_time、状态及 request ID；上游耗时短而总耗时长，可能是慢客户端或传输，不能直接归因后端。

- **502 与 504 区别？** 一般分别关联无效上游响应/连接问题与等待超时，但需结合 error log 的具体失败阶段判断。
- **reload 是否零影响？** 通常启动新 worker 并优雅退出旧 worker，长连接、资源上限和退出时限仍需验证。
- **练习**：用 `nginx -t` 检查配置，测试 /api、/api/、查询参数、超时和慢客户端。验收：路径转发符合表格预期；写请求不会因代理重试重复落账；日志可定位各阶段耗时。

## 5. 官方资料

- [HTTP Proxy 模块](https://nginx.org/en/docs/http/ngx_http_proxy_module.html)
- [请求处理](https://nginx.org/en/docs/http/request_processing.html)
