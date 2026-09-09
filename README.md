<div align="center">

# 🐎 uma-container

**容器化运行研究（M0-M8 路线图）**

![仓库](https://img.shields.io/badge/仓库-xf8410-8B5CF6?style=flat-square) ![分支](https://img.shields.io/badge/分支-1-10B981?style=flat-square) ![版本](https://img.shields.io/badge/版本-0-F59E0B?style=flat-square) ![CI](https://img.shields.io/badge/CI-1-3B82F6?style=flat-square)

</div>

---
> 📌 **一句话定位**：容器化运行研究（M0-M8 路线图）

## 🧭 项目定位

把赛马娘 APK 装进容器（VA Space）、嵌进 umawork 同屏运行的研究仓。参考 BlackBox/VA 原理自行重写（上游停更无 license，不抄源码），路线 PoC→反作弊实测→A14/15 适配→hlpatch 容器内加载→umawork 集成。已按项目决定暂停推进（方向调整），仓库保留为研究档案。

## ✨ 核心功能
- VA 容器原理重写方案- M0-M8 里程碑路线图- 反作弊与适配风险前置声明

## 🌿 分支导览（共 1 个分支全览）

<details open>
<summary><b>点击收起/展开全部分支用途说明</b></summary>

| 分支 | 用途说明 |
|---|---|
| `main` | 主干：架构计划书与研究记录 |

</details>

## 🏷️ 版本历史

无 release。

完整版本列表 ➡️ [Releases 页](../../releases)

## ⚙️ CI 流水线（共 1 条）

| 流水线 | 用途说明 |
|---|---|
| build | 文档构建流水线 |


---

## 📜 历史介绍存档

> 以下为仓库原有介绍，**内容未删改**，仅移入存档区（新版介绍以本页上方为准）。

<details>
<summary><b>点击展开原 README</b></summary>

# uma-container

**把日服《赛马娘 pretty derby》容器化，嵌进宿主 App 内部运行的实验项目。**

> ⚠️ **这是技术研究项目，不是开箱即用的工具。** 请先读完下面的风险声明。

---

## 这个仓库是干什么的

目标形态：宿主 App（[uma-workbench](https://github.com/xf8410/uma-workbench)，即 umawork）的界面上直接运行赛马娘——游戏画面、操作、数据采集全部发生在 umawork 进程内部，游戏本体不需要单独出现在屏幕上。同屏保留 AI 对话 / 工作区 / 小黑板。

技术路线：**App 容器化（virtual engine）**。参考 VirtualApp / BlackBox 一系的"虚拟引擎"原理：

- 游戏 APK 不装进系统，装进容器的私有空间（VA Space）
- 游戏 Activity / Service 以代理 Stub 形式跑在宿主进程里
- Framework 层代理：对游戏伪装 PackageManager / ActivityManager 等系统服务
- Native 层：IO 重定向 + 必要的 native hook
- 游戏渲染 Surface 直接由宿主提供 → 画面原生嵌入宿主 UI

## 与项目组其他仓库的关系

| 仓库 | 角色 |
|---|---|
| **uma-container**（本仓库） | 容器引擎：游戏在容器内运行 |
| [hlpatch](https://github.com/xf8410/hlpatch) | 游戏 SO 插件（IL2CPP hook / HTTP 端点），后续需适配容器内加载方式 |
| [uma-workbench](https://github.com/xf8410/uma-workbench) | 宿主 / 决策端：界面、AI 对话、小黑板，未来作为容器宿主集成 |

## 路线图

详细架构决策见 **[docs/architecture-plan.md](docs/architecture-plan.md)**（软件主体 / 模块结构 / 语言分配 / 冲突分析 / 兼容策略 / 里程碑）。

1. **PoC**：游戏 APK 装进容器 → 启动到标题画面（当前阶段）
2. **反作弊实测**：登录 + 进入主界面，观察完整性检测反应（决定性节点）
3. Android 14/15 适配修坑（现成开源框架均停留在 Android 12/13 时代）
4. hlpatch SO 容器内加载（注入方式需要按容器模型重做）
5. umawork 宿主集成：画面嵌入 + 同屏 AI 对话 / 小黑板

## 技术底座选型说明

| 框架 | 状态 | 结论 |
|---|---|---|
| VirtualApp 开源版 | GitHub 代码 2017-12 停更，新版仅商业授权 | 不采用 |
| BlackBox（FBlackBox/BlackBox） | 2024-04 停更，无 LICENSE 文件，支持至 Android 12/13 | **作为原理参考与起点，本仓库自行重写/改造** |
| 其他小 fork | 活跃度/质量不足 | 不采用 |

本仓库不直接复制上游代码，核心模块按其公开的架构原理自行实现，遇到关键难点（如 Stub Activity 注册、IO 重定向）参考其实现思路。

## ⚠️ 风险声明（请务必阅读）

1. **账号风险**：赛马娘有完整性校验与反作弊检测。容器化运行会显著改变应用的运行环境（签名、路径、进程模型、系统服务响应），**可能被检测并导致账号封禁**。任何后果由使用者自行承担。
2. **用途限制**：本仓库仅用于个人学习研究 Android 容器化 / 虚拟化技术。禁止用于任何商业用途。
3. **版权声明**：《赛马娘 pretty derby》及相关内容版权归 Cygames, Inc. 所有。本仓库不含游戏本体资源，不含任何绕过付费或访问控制的内容。
4. **无 license 声明的上游**：BlackBox 等参考项目未附带开源许可证，本仓库仅参考其公开技术资料与架构原理，不复制其源码；如相关权利人提出异议，将移除相关内容。

## 环境要求

- Android 12+（PoC 阶段目标设备：Android 14/15，需要修坑）
- 64 位设备（赛马娘日服为 arm64）

## CI

GitHub Actions：push 即编译（`gradle assembleDebug`），所有分支保持绿。

---
**当前状态**：🔴 PoC 前期——工程骨架搭建中，游戏尚未在容器内运行。


</details>
