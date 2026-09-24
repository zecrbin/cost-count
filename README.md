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

尚未完成：流水导入（`/api/import-records`）、财务总览统计；前端仍是旧版页面，尚未对接新接口。

接口文档：启动后端后访问 `http://localhost:8081/swagger-ui.html`。

## 测试

```bash
cd backend && mvn test
```

集成测试使用 H2（MySQL 模式），表结构见 `backend/src/test/resources/schema.sql`。

## 历史资料

删除前旧版本的业务模型、流程、接口和规则已归档至 [业务逻辑说明](docs/BUSINESS_LOGIC.md)。
