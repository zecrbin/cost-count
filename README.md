# Cost Count

本地个人记账系统，前后端单仓库：`backend/`（Spring Boot 3.5 + MyBatis-Plus-Join + MySQL）、`frontend/`（React + Mantine）。

## 当前进度

后端已完成记账核心链路：

| 模块 | 接口前缀 | 说明 |
| --- | --- | --- |
| 账户提供方 | `/api/account-providers` | 银行、支付平台等，增删改查 |
| 账户类型 | `/api/account-types` | 编码只能是 `DEBIT`（存储账户）或 `CREDIT`（信用账户），决定余额方向 |
| 账户 | `/api/accounts` | 开户时自动写入初始资金流水；填写尾号时生成银行卡图标；支持初始资金修正、停用和删除无流水账户 |
| 收支分类 | `/api/categories` | 两级分类树，类型为 `INCOME` / `EXPENSE` / `TRANSFER` |
| 交易流水 | `/api/transactions` | 收入、支出、转账、余额调整的增删改查，支持 `requestId` 幂等 |
| 账户日余额 | `/api/account-daily-balances` | 按活动日查询账户期初、变化和期末余额 |

余额规则：流水是余额的唯一事实来源。任何流水写入后，从受影响日期起按时间顺序回放相关账户，
同步账户当前余额、流水的 `balanceAfter` 和日余额快照。存储账户余额表示可用资金，信用账户余额表示待还金额，
具体方向见 `backend/src/main/java/com/costcount/common/LedgerBalance.java`。

前端（React 19 + Mantine 8）已对接以上全部接口：

| 页面 | 内容 |
| --- | --- |
| 总览 | 净资产、按月收支与结余、每日支出、支出构成、账户和最近流水；无账户时显示三步引导 |
| 流水 | 按月、类型、账户、分类、关键词筛选，按天分组；点击流水可修改或删除 |
| 账户 | 资产 / 负债分组、信用额度使用情况；详情抽屉含余额走势、账户流水、修正初始资金 |
| 分类 | 收入 / 支出 / 转账两级分类管理，可一键导入常用分类 |
| 机构与类型 | 机构及其账户类型管理，可一键添加常用银行和支付平台 |

「记一笔」在任意页面可用，支持支出、收入、转账和余额调整，并携带 `requestId` 防止重复提交。支持浅色 / 深色模式和手机布局。

尚未完成：流水导入（`/api/import-records`）。

## 本地运行

```bash
# 1. 建库：执行 backend/database/schema.sql（MySQL 8，默认库名 info_cc）
# 2. 后端：默认连接 127.0.0.1:3306，可用 MYSQL_HOST / MYSQL_USERNAME / MYSQL_PASSWORD 等环境变量覆盖
cd backend && mvn spring-boot:run
# 3. 前端（二选一，都会把 /api 和 /icons 代理到 8081）
cd frontend && npm install
npm run start   # 日常使用：打包后预览，加载约 460KB，打开 http://127.0.0.1:4173
npm run dev     # 改代码调试：热更新，未压缩的依赖约 13MB，打开 http://127.0.0.1:5173
```

接口文档：`http://localhost:8081/swagger-ui.html`。

### 本地文件

| 目录（相对 backend/） | 内容 | 环境变量 |
| --- | --- | --- |
| `data/account-icons` | 按卡号尾号生成的银行卡图标 | `ACCOUNT_ICON_DIR` |
| `data/uploaded-icons` | 上传的机构、账户图标 | `UPLOADED_ICON_DIR` |

未被任何机构或账户使用、且上传超过 24 小时的图标，每天 03:30 自动清理（例如上传后点了取消）。
执行时间可用 `UPLOADED_ICON_CLEANUP_CRON` 修改，设为 `-` 关闭。

## 测试

```bash
cd backend && mvn test
```

集成测试使用 H2（MySQL 模式），表结构见 `backend/src/test/resources/schema.sql`。

## 历史资料

删除前旧版本的业务模型、流程、接口和规则已归档至 [业务逻辑说明](docs/BUSINESS_LOGIC.md)。
