# repChain
响应式许可链

## 参考阅读
- [akka getting start](http://doc.akka.io/docs/akka/current/intro/getting-started.html) ——系统内部模块采用akka actor实现
- [akka remoting security](http://doc.akka.io/docs/akka/current/scala/remoting.html) ——节点之间安全通信采用akka Remote支持的TLS
- [akka serialization](http://doc.akka.io/docs/akka/current/scala/serialization.html)——节点之间消息交互采用protobuf序列化
- [scalaPB](https://scalapb.github.io/)——从proto定义生成scala类的工具
- [protobufjs](https://github.com/dcodeIO/ProtoBuf.js/)——在web端根据proto定义，反序列化protobuf字节流
- [swagger-scala](https://github.com/swagger-api/swagger-scala-module)——API支持swagger-UI
- [json4s](https://github.com/json4s/json4s)——在API层提供输入对象的json反序列化，返回结果的json序列化
- [d3.js-force layout](https://github.com/d3/d3-3.x-api-reference/blob/master/Force-Layout.md)——入／离网节点的自动布局
- [delight-nashorn-sandbox](https://github.com/javadelight/delight-nashorn-sandbox)——约束脚本执行时间的js引擎sandbox
- [leveldb for java](https://github.com/dain/leveldb)——存取Blocks、Transactions索引
- [java security](http://docs.oracle.com/javase/8/docs/technotes/guides/security/index.html)——hash、签名、密钥对及证书管理均采用jdk内置方法

## 安装
- install [jdk8+](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- install [Scala](https://www.scala-lang.org/download/)
- install [SBT](http://www.scala-sbt.org/release/docs/Setup.html)
- install [scala IDE](http://scala-ide.org/)
- install [keystore-explorer](http://keystore-explorer.org/) ——用于生成密钥对的工具,非必须
- install [protobuf editor](https://github.com/Enide/polyglot-maven-editors)——编辑protobuf定义工具，非必须

## 运行
- `git clone` download the project to local
- under the project root path,`sbt` to download dependencies.(maven默认仓库下载龟速,应使用[阿里镜像](http://www.jianshu.com/p/4d5bb95b56c5))
- `compile` to  generate protobuf scala class.
- `eclipse` to generate eclipse project settings.
- open scala IDE, `File->Import->Existing Projects into Workspace`
- right click `rep.app.Repchain.scala`,`Run As->Scala Application`(单机组网5个节点)
- Run configuration 配置VM参数 -Dlogback.configurationFile=conf/logback.xml (使logback配置生效)
- view realtime graph `http://localhost:8081/web/g1.html`
- view rest apis `http://localhost:8081/swagger/index.html`