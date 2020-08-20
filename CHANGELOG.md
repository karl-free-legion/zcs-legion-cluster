## 2.6.22 (2020-8-20)

Features:
   - 优化gossip的同步量级

BugFixes:
   - 修复发送概率性失败的问题   

## 2.6.20 (2020-8-12)

Features:
   - 修改gossip里的moduleInfo为对象，便于后续module信息扩展
   - X.message里添加routeversion信息用于后续灰度
   - simpleMessage的发送逻辑里加入根据ruoteVersion进行分流（灰度发布）
   - legion message 可灰度测试
   
## 2.6.19 (2020-7-29)

Features:
   - 添加注册中心能力，维护所有节点的HTTP路由信息
   - 提供客户端查询其他节点HTTP路由信息的接口处理器RequireModuleHttpInfoHandler
   - 添加routeVersion 在全Gossip和本地节点体系中
   

BugFixes:
   - 修改部分无用日志的打印

Docs:
   - 新增版本提交日志和Read.me