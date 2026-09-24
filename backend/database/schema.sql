-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: info_cc
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cc_account`
--

DROP TABLE IF EXISTS `cc_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cc_account` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `type_id` bigint DEFAULT NULL COMMENT '账户类型ID',
  `acc_name` varchar(256) DEFAULT NULL COMMENT '账户名称',
  `acc_tail_num` char(4) DEFAULT NULL COMMENT '账户尾号，如银行卡后4位',
  `balance` decimal(18,2) DEFAULT '0.00' COMMENT '当前余额，资产账户表示资产余额，负债账户表示当前欠款',
  `credit_limit` decimal(18,2) DEFAULT NULL COMMENT '当前信用额度，仅信用类账户使用',
  `ideal_credit_limit` decimal(18,2) DEFAULT NULL COMMENT '理想信用额度，仅信用类账户使用',
  `icon` varchar(256) DEFAULT NULL COMMENT '账户展示图标URL',
  `sort` int DEFAULT '0' COMMENT '排序值，越小越靠前',
  `status` tinyint DEFAULT '1' COMMENT '账户状态：1正常，0停用',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` varchar(256) DEFAULT 'system' COMMENT '创建人',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(256) DEFAULT 'system' COMMENT '更新人',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_type_id` (`type_id`),
  KEY `idx_status_sort` (`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cc_account_daily_balance`
--

DROP TABLE IF EXISTS `cc_account_daily_balance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cc_account_daily_balance` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `account_id` bigint NOT NULL COMMENT '账户ID',
  `stat_date` date NOT NULL COMMENT '统计日期',
  `opening_balance` decimal(18,2) DEFAULT NULL COMMENT '当日期初余额',
  `transaction_change` decimal(18,2) DEFAULT NULL COMMENT '当日正常交易导致的余额变动',
  `correction_change` decimal(18,2) DEFAULT NULL COMMENT '当日余额调整导致的余额变动',
  `closing_balance` decimal(18,2) DEFAULT NULL COMMENT '当日期末余额',
  `rebuilt_time` timestamp NULL DEFAULT NULL COMMENT '最近一次重建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_stat_date` (`account_id`,`stat_date`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户每日余额表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cc_account_provider`
--

DROP TABLE IF EXISTS `cc_account_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cc_account_provider` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `provider_name` varchar(128) DEFAULT NULL COMMENT '账户提供方名称，如招商银行、支付宝、微信',
  `icon` varchar(256) DEFAULT NULL COMMENT '提供方图标URL',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` varchar(256) DEFAULT 'system' COMMENT '创建人',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(256) DEFAULT 'system' COMMENT '更新人',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_provider_name` (`provider_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户提供方表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cc_account_type`
--

DROP TABLE IF EXISTS `cc_account_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cc_account_type` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `provider_id` bigint DEFAULT NULL COMMENT '账户提供方ID',
  `type_code` varchar(64) DEFAULT NULL COMMENT '账户类型编码',
  `type_name` varchar(128) DEFAULT NULL COMMENT '账户类型名称，如借记卡、信用卡、余额宝、花呗',
  `sort` int DEFAULT '0' COMMENT '排序值，越小越靠前',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` varchar(256) DEFAULT 'system' COMMENT '创建人',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(256) DEFAULT 'system' COMMENT '更新人',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_provider_id` (`provider_id`),
  KEY `idx_provider_sort` (`provider_id`,`sort`),
  KEY `idx_provider_type_code` (`provider_id`,`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cc_category`
--

DROP TABLE IF EXISTS `cc_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cc_category` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `pid` bigint DEFAULT NULL COMMENT '父分类ID，顶级分类为0',
  `category_type` varchar(64) DEFAULT NULL COMMENT '分类类型：INCOME收入、EXPENSE支出、TRANSFER转账',
  `category_name` varchar(256) DEFAULT NULL COMMENT '分类名称',
  `icon` varchar(256) DEFAULT NULL COMMENT '分类图标URL',
  `sort` int DEFAULT '0' COMMENT '排序值，越小越靠前',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` varchar(256) DEFAULT 'system' COMMENT '创建人',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(256) DEFAULT 'system' COMMENT '更新人',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_pid` (`pid`),
  KEY `idx_type_pid_sort` (`category_type`,`pid`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收支分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cc_import_record`
--

DROP TABLE IF EXISTS `cc_import_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cc_import_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `import_type` varchar(32) NOT NULL DEFAULT 'TEMPLATE' COMMENT '导入类型：TEMPLATE模板、WECHAT微信、BANK银行、AGENT智能体',
  `upload_file_name` varchar(256) DEFAULT NULL COMMENT '上传文件原始名称',
  `file_url` varchar(512) DEFAULT NULL COMMENT '原始导入文件存储地址',
  `file_hash` varchar(128) DEFAULT NULL COMMENT '文件SHA-256，用于文件完整性校验及重复上传识别',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小，单位字节',
  `status` varchar(32) NOT NULL DEFAULT 'PROCESSING' COMMENT '导入状态：PROCESSING处理中、SUCCESS成功、FAILED失败',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '文件流水总数',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功导入数量',
  `duplicate_count` int NOT NULL DEFAULT '0' COMMENT '重复流水数量',
  `error_message` varchar(1024) DEFAULT NULL COMMENT '导入失败原因',
  `import_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  `created_by` varchar(256) DEFAULT 'system' COMMENT '创建人',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(256) DEFAULT 'system' COMMENT '更新人',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_import_type_time` (`import_type`,`import_time`),
  KEY `idx_status_time` (`status`,`import_time`),
  KEY `idx_file_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='流水导入记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cc_transaction`
--

DROP TABLE IF EXISTS `cc_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cc_transaction` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `transaction_type` varchar(64) DEFAULT NULL COMMENT '流水类型：INITIAL初始化、INCOME收入、EXPENSE支出、TRANSFER转账、ADJUSTMENT余额调整',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID，初始化和余额调整可为空',
  `account_id` bigint DEFAULT NULL COMMENT '主账户ID',
  `target_account_id` bigint DEFAULT NULL COMMENT '目标账户ID，仅转账流水使用',
  `amount` decimal(18,2) DEFAULT NULL COMMENT '交易金额，统一存正数',
  `balance_change` decimal(18,2) DEFAULT NULL COMMENT '本流水对主账户余额产生的实际变动值，可正可负',
  `balance_after` decimal(18,2) DEFAULT NULL COMMENT '本流水发生后主账户余额',
  `transaction_time` datetime DEFAULT NULL COMMENT '交易发生时间，精确到秒',
  `counterparty` varchar(256) DEFAULT NULL COMMENT '交易对象，如商户、公司或个人',
  `source` varchar(64) DEFAULT NULL COMMENT '流水来源：MANUAL手工、IMPORT导入、AI智能记账、SYSTEM系统',
  `request_id` varchar(128) DEFAULT NULL COMMENT '请求唯一标识，用于接口幂等',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
  `created_by` varchar(256) DEFAULT 'system' COMMENT '创建人',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` varchar(256) DEFAULT 'system' COMMENT '更新人',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_request_id` (`request_id`),
  KEY `idx_account_time` (`account_id`,`transaction_time`),
  KEY `idx_target_account_time` (`target_account_id`,`transaction_time`),
  KEY `idx_category_time` (`category_id`,`transaction_time`),
  KEY `idx_type_time` (`transaction_type`,`transaction_time`),
  KEY `idx_transaction_time` (`transaction_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户交易流水表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-23 23:52:06
