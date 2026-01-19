-- ============================================
-- 交通仿真系统数据库初始化脚本
-- Database: traffic_sim
-- Version: 2.0
-- Date: 2024
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 创建数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS `traffic_sim` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `traffic_sim`;

-- ============================================
-- 用户管理相关表
-- ============================================

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone_number` VARCHAR(20) DEFAULT NULL COMMENT '电话',
  `institution` VARCHAR(200) DEFAULT NULL COMMENT '机构',
  `role_id` INT(11) DEFAULT NULL COMMENT '角色ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/BANNED/BLOCKED',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 角色表
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色代码',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ----------------------------
-- 权限表
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限代码',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- ----------------------------
-- 角色权限关联表
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `role_id` BIGINT(20) NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT(20) NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ============================================
-- 地图管理相关表
-- ============================================

-- ----------------------------
-- 地图表
-- ----------------------------
DROP TABLE IF EXISTS `map`;
CREATE TABLE `map` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `map_id` VARCHAR(255) DEFAULT NULL COMMENT 'MongoDB地图ID',
  `name` VARCHAR(255) NOT NULL COMMENT '地图名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '地图描述',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `xml_file_name` VARCHAR(255) DEFAULT NULL COMMENT 'XML文件名',
  `xml_file_path` VARCHAR(500) DEFAULT NULL COMMENT 'Python端XML文件路径（用于仿真引擎）',
  `map_image` LONGTEXT COMMENT '地图图片（Base64）',
  `owner_id` BIGINT(20) NOT NULL COMMENT '所有者用户ID',
  `status` TINYINT(4) NOT NULL DEFAULT 1 COMMENT '地图状态：0-公开，1-私有，2-禁用',
  `file_size` BIGINT(20) DEFAULT 0 COMMENT '文件大小（字节）',
  `storage_path` VARCHAR(500) DEFAULT NULL COMMENT '存储路径',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_map_id` (`map_id`),
  KEY `idx_owner_id` (`owner_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='地图表';

-- ----------------------------
-- 用户地图配额表
-- ----------------------------
DROP TABLE IF EXISTS `user_map_quota`;
CREATE TABLE `user_map_quota` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `max_maps` INT(11) NOT NULL DEFAULT 50 COMMENT '最大地图数量',
  `current_maps` INT(11) NOT NULL DEFAULT 0 COMMENT '当前地图数量',
  `total_size` BIGINT(20) DEFAULT 0 COMMENT '总文件大小（字节）',
  `max_size` BIGINT(20) DEFAULT 1073741824 COMMENT '最大存储空间（字节，默认1GB）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户地图配额表';

-- ============================================
-- 仿真任务相关表
-- ============================================

-- ----------------------------
-- 仿真任务表
-- ----------------------------
DROP TABLE IF EXISTS `simulation_task`;
CREATE TABLE `simulation_task` (
  `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
  `name` VARCHAR(255) NOT NULL COMMENT '仿真名称',
  `map_xml_name` VARCHAR(255) DEFAULT NULL COMMENT '地图XML文件名',
  `map_xml_path` VARCHAR(500) DEFAULT NULL COMMENT '地图XML文件路径',
  `sim_config` TEXT COMMENT '仿真配置（JSON）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/RUNNING/PAUSED/STOPPED/FINISHED',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仿真任务表';

-- ============================================
-- 回放任务相关表
-- ============================================

-- ----------------------------
-- 回放任务表
-- ----------------------------
DROP TABLE IF EXISTS `replay_task`;
CREATE TABLE `replay_task` (
  `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
  `simulation_task_id` VARCHAR(64) NOT NULL COMMENT '关联的仿真任务ID',
  `name` VARCHAR(255) NOT NULL COMMENT '回放任务名称',
  `status` VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED/PLAYING/PAUSED/STOPPED/FINISHED',
  `current_step` BIGINT(20) DEFAULT 0 COMMENT '当前步数',
  `total_steps` BIGINT(20) DEFAULT 0 COMMENT '总步数',
  `playback_speed` DOUBLE DEFAULT 1.0 COMMENT '播放速度（倍速）',
  `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_simulation_task_id` (`simulation_task_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回放任务表';

-- ============================================
-- 初始化基础数据
-- ============================================

-- 插入默认角色
INSERT INTO `role` (`role_name`, `role_code`, `description`) VALUES
('ADMIN', '管理员', '系统管理员，拥有所有权限'),
('USER', '普通用户', '普通用户，拥有基本权限'),
('RESEARCHER', '研究员', '研究员，拥有高级权限')
ON DUPLICATE KEY UPDATE `role_name`=VALUES(`role_name`);

-- 插入默认权限
INSERT INTO `permission` (`permission_name`, `permission_code`, `description`) VALUES
('用户管理', 'USER_MANAGE', '管理用户信息'),
('地图管理', 'MAP_MANAGE', '管理地图信息'),
('仿真管理', 'SIMULATION_MANAGE', '管理仿真任务'),
('回放管理', 'REPLAY_MANAGE', '管理回放任务'),
('系统配置', 'SYSTEM_CONFIG', '系统配置管理')
ON DUPLICATE KEY UPDATE `permission_name`=VALUES(`permission_name`);

-- 为管理员角色分配所有权限
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id
FROM `role` r, `permission` p
WHERE r.role_code = 'ADMIN'
ON DUPLICATE KEY UPDATE `role_id`=VALUES(`role_id`);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 授权远程访问（Docker 网络必需）
-- ============================================

-- 授权 root 用户从任意主机连接
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;

-- 授权 traffic_sim 用户从任意主机连接
CREATE USER IF NOT EXISTS 'traffic_sim'@'%' IDENTIFIED BY 'traffic_sim';
GRANT ALL PRIVILEGES ON traffic_sim.* TO 'traffic_sim'@'%';

FLUSH PRIVILEGES;

-- ============================================
-- 初始化完成
-- ============================================

