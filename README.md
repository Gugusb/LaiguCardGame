# Laigu 来古牌

> Minecraft 1.20.1 · Forge 47.4.6 · JDK 17

一个以「**收集卡牌**」为后期目标的 Minecraft 模组。开卡包、鉴宝、集齐主题卡牌，
为已经毕业的后期玩家提供新的长期目标。

## 环境要求

| 依赖 | 版本 |
|------|------|
| JDK  | 17（已安装：Microsoft Build of OpenJDK 17.0.16，`JAVA_HOME=D:\Env\Java\ms-17.0.16`） |
| Gradle | 8.8（项目自带 wrapper，无需单独安装） |
| Minecraft | 1.20.1 |
| Forge | 47.4.6 |

## 常用命令

在项目根目录（`Laigu/`）执行：

```bash
./gradlew build          # 编译并打出模组 jar（输出在 build/libs/）
./gradlew runClient      # 启动开发版 Minecraft 客户端
./gradlew runServer      # 启动开发版服务端（--nogui）
./gradlew runData        # 运行数据生成器
./gradlew genSources     # 生成可阅读的反编译源码（IDE 里跳转用）
./gradlew --refresh-dependencies  # 强制刷新依赖
```

> Windows 下建议直接在终端（Git Bash / PowerShell）里运行 `./gradlew`。
> 首次运行会下载 Minecraft 依赖并反编译，耗时较长，之后会走本地缓存。

## IDE 导入

- **IntelliJ IDEA**：`File → Open` 选中本目录，等待 Gradle 同步；首次会自动生成 `runClient` 运行配置。
- **VS Code**：装 Extension Pack for Java + Gradle for Java 后打开本目录。

## 项目结构

```
Laigu/
├── build.gradle          # ForgeGradle 6 构建脚本
├── gradle.properties     # 版本号与模组元数据（改这里改 mod id/版本）
├── settings.gradle
├── tools/generate_cards.py   # 卡牌资源生成器（贴图接入 + 模型 + 卡目录 + 语言）
├── src/main/java/com/laigu/laigu/
│   ├── Laigu.java            # @Mod 主类
│   ├── card/CardCatalog.java # 卡目录（79 张卡 id，生成文件勿手改）
│   ├── card/CardInfo.java    # 卡牌元数据：朝代 / 文物类型（可调）
│   ├── item/CardItem.java    # 卡牌物品类（端详 + tooltip + NBT）
│   ├── item/CardPouchItem.java  # 卡袋物品
│   ├── item/CardPackItem.java   # 卡包物品（开包逻辑 + 概率）
│   ├── container/CardPouchContainer.java # 卡袋 6 格容器（NBT 持久化，仅收卡牌）
│   ├── container/CardPouchMenu.java      # 卡袋菜单
│   ├── buff/CardSynergy.java  # 卡牌羁绊（袋内组合 → buff）
│   ├── client/InspectAnimator.java # 端详动画（第一人称）
│   ├── client/CardPouchScreen.java  # 卡袋 GUI
│   ├── client/ClientSetup.java      # 客户端注册（菜单→GUI）
│   ├── event/ModEvents.java   # 羁绊 tick 事件
│   ├── util/CardNbt.java      # 卡牌 NBT（所有者/附魔光泽）
│   └── registry/
│       ├── ModItems.java         # 物品 DeferredRegister（158 卡 + 4 卡包 + 卡袋）
│       ├── ModMenuTypes.java     # 菜单类型 DeferredRegister
│       └── ModCreativeTabs.java  # 创造页签「来古牌」
└── src/main/resources/
    ├── META-INF/mods.toml       # 模组元数据（modId/依赖）
    ├── pack.mcmeta
    └── assets/laigu/
        ├── textures/item/       # 卡牌/卡包/卡袋贴图
        ├── textures/gui/card_pouch.png  # 卡袋 GUI 贴图
        ├── models/item/         # 每张贴图一个物品模型
        └── lang/zh_cn.json / en_us.json   # 本地化（168 条）
```

### 卡牌物品

- **命名空间**：`laigu`
- **卡牌**：79 张 × 2 稀有度 = 158 个物品，形如 `laigu:<拼音>_common` / `laigu:<拼音>_gold`（如 `laigu:qian_li_jiang_shan_common`）
- **卡包**：`laigu:card_pack_common` / `card_pack_ender` / `card_pack_rainbow` / `card_pack_gold`
- **卡袋**：`laigu:card_pouch`（6 格卡牌存储）
- 全部在创造页签「来古牌」里，进游戏即可看到
- 新增卡/改贴图：改 `tools/generate_cards.py` 后运行 `python tools/generate_cards.py`，再重新构建

### 卡牌玩法（2026-08-12 新增）

- **端详**：手持卡牌右键，第一人称会做出「端详」动作（约 3 秒，抬起、转向玩家、轻微摆动）。实现于 `client/InspectAnimator.java`，调参常量在类顶部 `INSPECT_*`。
- **卡牌信息（tooltip）**：卡牌下方显示该文物所属**朝代**与**文物类型**（数据在 `card/CardInfo.java`，可调）；开包产出的卡牌额外显示**所有者**标签。
- **卡袋**：右键卡袋打开 6 格 GUI，**只能放入来古牌**。内容持久化在物品 NBT（`container/CardPouchContainer.java`）。
- **羁绊（副手持卡袋触发）**：服务端每 2 秒按袋内卡牌组合施加 buff（规则见 `buff/CardSynergy.java`，初版可调）：
  - 同一朝代 ≥6 张不同卡 → **夜视**
  - 青铜器 ≥4 张 → **力量 I**
  - 书画 ≥4 张 → **急迫 I**
  - 织绣 ≥4 张 → **生命恢复 I**
  - 不同卡 ≥10 张 → **抗性提升 I**
  - 金质不同卡 ≥6 张 → **幸运 I**
- **开包**：右键卡包开包。
  - **普通包**：固定 5 张卡，普通 99.8% / 金质 0.2%，任意版本 1% 概率附魔光泽，并标记所有者。
  - 末影 / 炫彩 / 金质包：概率为占位值（见 `item/CardPackItem.java` 的 `PackType`），战利品表后续再细化。

## 模组元数据速查

- **mod_id**：`laigu`（`gradle.properties` 的 `mod_id`，与 `Laigu.MODID`、`mods.toml` 一致）
- **group / 包名**：`com.laigu.laigu`
- **版本**：`1.0.0`（`gradle.properties` 的 `mod_version`）

## 现有资产（仓库根目录）

`Wuhuamixian/像素画头像_64x64/` 下有已产出的贴图管线：

- 大量 **64×64 中式文物主题卡面**（如：千里江山图、亚长牛尊、曾侯乙编钟、敦煌飞天……）
- `make_cards.py` 系列脚本：合成 64×64 卡牌（卡面 38×57 + 中式回纹边框）
- 卡牌三稀有度：**普通 / 金质 / 钻石**（金/钻由卡面明暗程序化换色）
- 卡包贴图：普通、末影、炫彩、金质四种

这些资产是卡牌系统的美术来源，后续开发直接把对应 PNG 放回模组资源目录即可。

## 开发路线图（建议）

1. **卡牌数据系统**：用数据驱动（JSON/NBT）定义卡牌——id、名称、稀有度、贴图、简介。
2. **卡牌物品**：一个通用卡牌物品 + NBT 区分具体卡（避免注册表爆炸）；自定义渲染（图标层 + 卡面层）。
3. **卡包系统**：右键开包 → 按稀有度权重随机出卡。
4. **获取途径**：与后期玩法挂钩——Boss 掉落、考古、钓鱼、附魔、交易等。
5. **图鉴系统**：已收集/未收集图鉴（服务端存收集进度），收集进度可作后期目标。
6. **交易/展示**：卡牌可交易、可放入展示架/相框。

## 常见坑

1. **中文乱码**
   - `gradle.properties` 按 ISO-8859-1 读取，里面的中文必须写成 `\uXXXX` 字面转义（例：`来古牌` → `来古牌`）。
   - 资源文件（`mods.toml`、`pack.mcmeta`、`lang/*.json`）必须是 UTF-8。`build.gradle` 的 `processResources` 已加 `filteringCharset = 'UTF-8'`，不要删。
   - Java 源码用 UTF-8（`build.gradle` 已配置 `options.encoding = 'UTF-8'`）。
2. **首次构建慢**：首次 `./gradlew build` 要下载 Minecraft 并反编译（约 10 分钟）。之后走本地缓存，增量构建约半分钟。
3. **改 mod id / 版本**：改 `gradle.properties` 的 `mod_id` / `mod_version` 后，`mods.toml` 里会自动同步（processResources 注入）。

## 许可

All Rights Reserved（见 `gradle.properties` 的 `mod_license`）。
