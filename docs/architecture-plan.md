# uma-container 架构计划书

> 版本 1.0 · 2026-09-05 · 决策状态：**已定稿**
> 本文档回答五个问题：软件主体是谁 / 模块怎么分 / 每部分用什么语言 / 代码冲突在哪 / 如何兼容。

---

## 1. 软件主体：umawork 是主体，本仓库是引擎库

**最终形态的软件主体是 [uma-workbench](https://github.com/xf8410/uma-workbench)（umawork）**，不是本仓库。

容器引擎（本仓库）是 umawork 引入的一个 **Android Library（AAR）**，运行时游戏跑在 umawork 的子进程里：

```
最终形态（M8 之后）：

umawork（主体 app，用户看到/打开的软件）
 ├── umawork 自身 UI：AI 对话 / 工作区 / 小黑板 / 游戏 Tab
 └── uma-container 引擎（AAR）
      └── VAPP Client 进程（:pN 子进程）
           └── 赛马娘 Activity / 渲染 / hlpatch SO
           → 画面输出到 umawork 的 游戏 Tab

PoC 形态（M1-M7）：本仓库独立 APK 自当壳宿主，用于快速验证，
不背 umawork 的复杂度。壳的代码在 M8 废弃，引擎代码全部保留。
```

**为什么这样定**（对比过另两条）：

| 方案 | 结论 |
|---|---|
| ❌ 本仓库长期独立成 app | 游戏嵌进 umawork 的目标无法达成，等于退回"两个 app 分屏" |
| ❌ 引擎代码直接写进 umawork 仓库 | 引擎需要大量试错（Android 版本适配坑多），污染主仓库 CI 和稳定性 |
| ✅ **独立引擎仓库 + 最终以 AAR 集成** | 引擎独立试错；集成的接口面（3 个）稳定后 umawork 一次性接入 |

**umawork 需要提供的集成面（仅 3 个）**：

1. `Application` 里初始化引擎（传配置）
2. Manifest 声明 Stub Activity/Service/进程（引擎提供合并用的 manifest 片段）
3. 游戏 Tab 的 SurfaceView + 引擎启动接口

---

## 2. 模块结构

```
uma-container/
├── app/                     # PoC 壳宿主（Kotlin，M8 后废弃）
├── engine-core/             # ★ Android Library：Framework 代理层
│   ├── proxy/               #   AM/PM/WMS 等系统服务代理（Binder 动态代理）
│   ├── stub/                #   Stub Activity/Service/Provider 注册与分发
│   ├── space/               #   VA Space：游戏 APK 安装/存储/版本管理
│   ├── process/             #   VAPP Client 进程 spawn/attach/命名伪装
│   └── compat/              #   Android 版本适配层（12→15，每版本一个 Adapter）
├── engine-native/           # ★ Rust crate → libuma_container.so
│   ├── io_redir/            #   IO 重定向：拦截 open/stat/access 改写路径
│   ├── hook/                #   ShadowHook FFI 桥（inline/PLT hook）
│   └── jni/                 #   JNI 桥（jni crate）
├── engine-hook/             # Kotlin 层 hook 工具（反射/元反射封装，HiddenApiBypass）
└── docs/                    # 本文档 + 决策记录
```

**依赖方向**：`app → engine-hook → engine-core → engine-native`（单向，无环）。

---

## 3. 语言分配及理由

| 层 | 语言 | 占比 | 理由 |
|---|---|---|---|
| Framework 代理、Stub、VA Space、进程管理 | **Kotlin** | ~85% | 工作对象是 ART 里的 Java 对象和系统服务，Kotlin 反射/动态代理是唯一正解。汇编/C++ 在这一层没有入口 |
| IO 重定向、native hook、JNI 桥 | **Rust** | ~15% | 与 hlpatch 完全同构的工具链（cargo + cargo-ndk → .so），组内有 Rust 人力与 CI 经验；内存安全，hook 层出 segfault 概率低于 C++ |
| inline hook 跳板 | **不自己写** | — | 用 ShadowHook（MIT）现成框架，arm64 汇编跳板它已包办 |
| **汇编** | **无** | 0% | 见下 |

**为什么不是汇编**：容器引擎的核心工作（替换系统服务响应）发生在 ART/Framework 层，汇编摸不到；native 层只有 IO 重定向一小块，ShadowHook 已处理掉所有架构差异；游戏月更需要快速跟进，汇编无法维护。

**Rust vs C++ 备选说明**：BlackBox/VA 的 native 参考代码是 C++，读资料时直接；但定 Rust，理由是 hlpatch 已验证"Rust SO 跑进游戏进程 hook IL2CPP"全链路，风险已被组内踩平。若 ShadowHook FFI 桥出现解决不了的问题，降级预案是 C++ 重写 engine-native（接口不变，层隔离保证替换成本可控）。

---

## 4. 冲突分析（已识别的 5 个冲突点及解法）

### 4.1 Kotlin ↔ Rust：无冲突
边界是唯一交汇点：JNI（C ABI）。约定：JNI 桥只出现在 `engine-native/jni/`，engine-core 不直接碰 native 符号（通过一个 `NativeBridge.kt` 接口隔离）。

### 4.2 进程模型 vs umawork：**最大的集成冲突**
容器要求 VAPP Client 进程运行在 `umawork:pN` 进程并把进程名伪装成游戏进程名（`jp.co.cygames.umamusume` 等读到 `/proc/self/cmdline` 的检测要骗过）。umawork 已有自己的进程结构，两者要在 Manifest 合并层共存。
**解法**：引擎提供 manifest 片段（`<process>:p0..p3` 的 Stub 声明），umawork 一次合并；进程伪装由 engine-core 在 attach 时改写，umawork 自身进程不受影响。

### 4.3 hlpatch SO 注入：**最大的技术适配**
hlpatch 是注入游戏进程的。容器模式下游戏进程 = umawork:pN，注入路径全变：游戏 APK 在 VA Space 内安装（保留 hlpatch 重打包版），SO 由容器加载进 :pN 进程，IL2CPP hook 链（基址获取、hook 时机）要适配容器加载顺序。
**影响范围**：路线图 M7。hlpatch 的 HTTP 端点（18765）逻辑不变——SO 只要活着端点就在。**本仓库 M1-M6 不碰 hlpatch，把适配隔离在 M7 一个里程碑里**。

### 4.4 加速器分流：行为变化，不是代码冲突
Per-App 加速器按 uid 分流。游戏在 :pN 进程 = uid 是 umawork 的 → **加速器要给 umawork 开加速**，不能再按游戏包名开。无代码工作，需在用户文档写明（走本机回环的方案设计不受影响）。

### 4.5 依赖/版本 vs umawork
- AGP/Kotlin/依赖版本：library 侧向 umawork 对齐（M8 时调整本仓库 gradle 到 umawork 同版本），不是 umawork 迁就引擎
- namespace：`io.github.xf8410.umcontainer.*` 独立，无资源/类名冲突
- minSdk：引擎 minSdk 31 ≤ umawork minSdk（待对齐确认）

---

## 5. 兼容策略

| 维度 | 策略 |
|---|---|
| **Android 版本** | minSdk 31（Android 12），target 35。hidden API 访问统一走 `engine-hook`（元反射 + [HiddenApiBypass](https://github.com/LSPosed/HiddenApiBypass)（Apache-2.0））。版本差异全部收口在 `engine-core/compat/`：每 API level 一个 Adapter，代理层只调 Adapter 接口 |
| **CPU 架构** | **arm64 only**。日服赛马娘 arm64-only → VA 的"32位主包+64位插件包"双包痛点对我们不存在，引擎只跑 64 位 |
| **游戏月更** | 游戏月更只影响 VA Space 里的 APK（重装即升），引擎代码不跟游戏版本走；CI 加冒烟测试（装最新 APK 启动）——M4 后建 |
| **三方库** | 全部 Apache-2.0 / MIT：HiddenApiBypass（Apache-2.0）、ShadowHook（MIT）、jni crate（Apache-2.0/MIT 双许可）。**无 license 上游（BlackBox/VA）仅参考原理，不复制源码** |
| **hlpatch 采集链** | umawork 对 18765 端点的轮询链路不变（SO 在 :pN 进程里活着就监听）。M7 只验证一次 |
| **资源隔离** | 游戏资源经 IO 重定向落到 VA Space，与宿主资源零共享；游戏 Resources 对象由引擎单独构造（AssetManager 重定向），不污染 umawork 主题 |

---

## 6. 里程碑

| # | 内容 | 验收标准 | 风险 |
|---|---|---|---|
| M0 ✅ | 仓库骨架 + CI | assembleDebug 绿 | — |
| M1 | engine-core 骨架 + VA Space | 游戏 APK 装进容器、manifest 解析出包信息 | 低 |
| M2 | Stub 注册 + 启动链 | **demo app** 在容器内启动到 Activity 显示 | 中（hidden API） |
| M3 | engine-native IO 重定向 | Rust .so 接入，demo app 文件访问被重定向到 VA Space | 低 |
| M4 | **赛马娘 PoC** | 游戏装进容器，启动到标题画面（可能黑屏/闪退，记录即可） | 中 |
| M5 | **反作弊实测（决定性）** | 登录 + 进主界面，观察检测反应。**失败即项目终止评估点** | **高** |
| M6 | Android 14/15 适配 | 用户手机（Android 14/15）上全流程跑通 | 高（最大工程量） |
| M7 | hlpatch SO 容器内加载 | 18765 端点在 :pN 进程内可达，/summary 数据正常 | 中 |
| M8 | umawork 集成 | umawork 游戏 Tab 内玩游戏 + 黑板同屏 | 中 |

**M5 是项目的生死节点**：反作弊若不可绕过，容器方案死，退回虚拟屏方案（Extendroid 实测结论可复用）。M1-M4 的投入是可复用的通用容器技术，不算白干。

---

## 7. 决策记录（ADR 摘要）

1. **主体 = umawork**，本仓库是引擎库 + PoC 壳（§1）
2. **语言 = Kotlin 主体 + Rust native + 零汇编**（§3）
3. **hook 框架 = ShadowHook**，hidden API = HiddenApiBypass，不重造轮子（§5）
4. **arm64 only**，不背双包架构包袱（§5）
5. **hlpatch 适配隔离在 M7**，M1-M6 完全不碰（§4.3）
6. 上游无 license 项目（BlackBox/VA）只参考架构原理，源码自行重写（README 风险声明）
