# Kafka 系列

[深入正文：日志、复制、事务与消费治理](01_日志复制事务语义与消费治理深入解析.md) 覆盖日志/保留、ISR/确认、producer 幂等、顺序、poll/重平衡、offset、Kafka 事务、outbox/inbox 和积压恢复。

[导读与速查](Kafka事件流与可靠消费手册.md) 用于回顾。与 [RocketMQ](../chapter_06_RocketMQ/README.md) 对比时，以消息语义、生态和团队运维为依据。

学习验收：能定位提交窗口、维护连续完成 offset，并计算净消化和追平时间；KRaft 运维、Streams/Connect 专项及运行实验集群尚未提供。
