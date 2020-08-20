Legion-Cluster 2.6.22(2020-8-20)
---
## 介绍

Leigion总线系统的服务端（核心）

主要特性

1. 通过gossip协议保持集群状态同步
2. 处理客户端的各类请求（心跳、业务消息、恢复消息）
3. 通过一直HashTable保证节点的分布均匀
4. 实现客户端的httpRoute信息同步
5. 实现灰度发布能力

## 修改日志

版本更新情况请查看[修改日志](/CHANGELOG.md)

## 运行环境要求
  
* JDK环境要求  1.8