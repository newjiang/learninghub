# Jenkins 持续交付导读与速查

> 阅读定位：本篇是导读与速查，不代替深入正文。已展开专题和当前覆盖边界见[知识手册导航](../../知识手册导航.md)。

适用范围：Jenkins LTS、Declarative Pipeline。核心目标是让制品、检查、权限和发布可追溯，插件版本由组织按支持策略锁定。

## 1. 流水线与执行隔离

controller 负责调度与状态，agent 执行构建；不要在 controller 常态跑不受信任构建。Jenkinsfile 纳入代码审查，编译、测试、扫描、制品发布、部署和验证分为明确阶段。

```groovy
pipeline {
  agent { label 'java17' }
  options { timeout(time: 20, unit: 'MINUTES'); disableConcurrentBuilds() }
  stages {
    stage('Verify') {
      steps { sh './mvnw -B verify' } // Linux agent，仓库提供 wrapper
    }
    stage('Archive') {
      steps { archiveArtifacts artifacts: 'target/*.jar', fingerprint: true }
    }
  }
  post { always { junit allowEmptyResults: false, testResults: 'target/surefire-reports/*.xml' } }
}
```

示例针对单模块、具有单元测试报告的 Java 工程，不含部署。disableConcurrentBuilds 只限制该 job 的并发，不协调其他 job 对同一环境的发布；环境锁需要单独设计。

## 2. 凭据、分支和供应链

凭据使用专用绑定和最小权限，按环境隔离；日志掩码不是完整泄漏防护，命令行参数、制品及恶意构建都可能暴露秘密。非可信 PR 不应获得生产凭据，也不应在可窃取后续秘密的共享环境执行。

依赖缓存提高速度但不是可信事实源；验证依赖来源、校验和、锁定版本及 SBOM。制品构建一次，以不可变摘要在环境间晋级；每个环境重新构建会让已测试制品与生产制品不一致。

## 3. Groovy CPS 与共享库

Pipeline 的 CPS 转换支持暂停和恢复。暂停点周围保存的状态可能需要序列化，连接、文件流和某些迭代器不适合长时间保留。`@NonCPS` 用于特定纯计算场景，不应在其中调用需要 CPS 的 Pipeline step。

共享库封装稳定能力：制品元信息、校验、发布和回滚接口。不要把全部业务隐藏到巨大脚本中，使 Jenkinsfile 无法审查实际权限与发布行为。

## 4. 发布、回滚和排障

流水线输出提交、制品 digest、配置版本、目标环境、检查结果和操作者。生产发布先做兼容性检查与小流量验证，依据错误率、延迟和业务指标判断是否继续；成功执行部署命令不等于业务成功。

失败先判断排队、agent 离线、依赖下载、测试、制品上传或部署阶段。重试只包围可重入步骤；整体 retry 可能重复建单、迁移或发布。controller 备份覆盖配置、凭据恢复方案及 job 元数据，恢复演练检验 RTO。

## 5. 面试与练习

- **CI 与 CD 的界线？** CI 保持集成后的可验证状态，交付保证随时可发布，部署自动化把制品送入环境；用团队实际门禁说明。
- **灰度异常怎样回滚？** 除镜像回退，还要检查 schema、配置、消息版本和外部副作用。
- **练习**：对一个本地测试环境实现不可变制品晋级，模拟 agent 中断和部署后验证失败。验收：不能绕过检查发布；重跑不重复执行非幂等动作；可定位到具体制品与配置。

## 6. 官方资料

- [Pipeline 语法](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [CPS 不匹配](https://www.jenkins.io/doc/book/pipeline/cps-method-mismatches/)
- [凭据](https://www.jenkins.io/doc/book/using/using-credentials/)
