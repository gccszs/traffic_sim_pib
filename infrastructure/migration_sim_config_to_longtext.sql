-- ============================================
-- 数据库迁移脚本：优化 sim_config 存储
-- 1. 将 TEXT 类型改为 LONGTEXT 以支持更大的配置数据
-- 2. 清理已有数据中的冗余字段（map_pic 和 map_json）
-- Date: 2026-01-22
-- ============================================

USE `traffic_sim`;

-- 步骤1：修改 simulation_task 表的 sim_config 列类型
ALTER TABLE `simulation_task` 
MODIFY COLUMN `sim_config` LONGTEXT COMMENT '仿真配置（JSON）';

-- 步骤2：清理已有数据中的 map_pic 和 map_json 字段
-- 注意：这个操作会修改现有数据，建议先备份
-- 如果不需要清理历史数据，可以注释掉下面的 UPDATE 语句

UPDATE `simulation_task`
SET `sim_config` = JSON_REMOVE(
    JSON_REMOVE(
        `sim_config`,
        '$.simInfo.map_pic'
    ),
    '$.simInfo.map_json'
)
WHERE `sim_config` IS NOT NULL
  AND JSON_VALID(`sim_config`)
  AND (
      JSON_CONTAINS_PATH(`sim_config`, 'one', '$.simInfo.map_pic')
      OR JSON_CONTAINS_PATH(`sim_config`, 'one', '$.simInfo.map_json')
  );

-- 验证修改
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    COLUMN_COMMENT
FROM 
    INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_SCHEMA = 'traffic_sim' 
    AND TABLE_NAME = 'simulation_task' 
    AND COLUMN_NAME = 'sim_config';

-- 显示清理后的数据大小统计
SELECT 
    COUNT(*) as total_tasks,
    AVG(LENGTH(sim_config)) as avg_config_size,
    MAX(LENGTH(sim_config)) as max_config_size,
    MIN(LENGTH(sim_config)) as min_config_size
FROM 
    `simulation_task`
WHERE 
    `sim_config` IS NOT NULL;

-- ============================================
-- 迁移完成
-- ============================================

