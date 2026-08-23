-- =============================================================================
-- openatom-system 数据库性能检查脚本
--
-- 用法（服务器上执行，需 MySQL 管理员权限）：
--   mysql -u root -p < performance-check.sql
--
-- 启用慢查询（一次性，重启 MySQL 后失效；持久化需写入 my.cnf）：
--   SET GLOBAL slow_query_log = ON;
--   SET GLOBAL long_query_time = 1;
--   SET GLOBAL slow_query_log_file = '/www/server/data/slow.log';
--
-- my.cnf 持久化建议（[mysqld] 段）：
--   slow_query_log = ON
--   long_query_time = 1
--   innodb_buffer_pool_size = 1G          # 物理内存的 50-70%
--   innodb_flush_log_at_trx_commit = 2    # 高并发场景降级为 1 会显著提升写入吞吐
--   max_connections = 300
-- =============================================================================

-- 1. 慢查询统计（按执行次数排序，看哪些 SQL 值得优化）
SELECT DB_NAME, query, ROUND(AVG(query_time), 3) AS avg_ms,
       COUNT_STAR, SUM_ROWS_EXAMINED, SUM_ROWS_SENT
FROM performance_schema.events_statements_summary_by_digest
WHERE SCHEMA_NAME = 'openatom-db' AND SUM_ROWS_EXAMINED > 0
ORDER BY SUM_ROWS_EXAMINED DESC
LIMIT 20;

-- 2. 当前正在运行的长事务/慢查询
SELECT id, user, db, command, time, state, LEFT(info, 120) AS sql_text
FROM information_schema.processlist
WHERE command <> 'Sleep' AND time > 5
ORDER BY time DESC;

-- 3. 大表扫描情况：全表扫描次数高的表（explain 里出现 ALL 的表）
SELECT table_name, rows_read, rows_sent, rows_examined_scan
FROM performance_schema.table_io_waits_summary_by_table
WHERE object_schema = 'openatom-db'
ORDER BY rows_examined_scan DESC
LIMIT 15;

-- 4. 缺失索引提示（MySQL 8 自带 advisor，给出建议建索引的语句）
SELECT table_schema, table_name, column_name, index_name, cardinality
FROM information_schema.statistics
WHERE table_schema = 'openatom-db'
  AND index_name = 'PRIMARY'
  AND cardinality > 0
LIMIT 0;

-- 5. 常用热点查询的索引核对（如缺失会显示全表扫描/无可用索引）
--    tb_login_log：登录日志按 user_id/时间查询
SHOW INDEX FROM tb_login_log;
SHOW INDEX FROM tb_user;
SHOW INDEX FROM tb_notification;
SHOW INDEX FROM tb_blog_article;
SHOW INDEX FROM tb_activity;

-- 若 tb_login_log 缺少 user_id 索引，执行：
--   ALTER TABLE tb_login_log ADD INDEX idx_login_user_time (user_id, create_time);
-- 若 tb_notification 缺少 receiver 索引，执行：
--   ALTER TABLE tb_notification ADD INDEX idx_notif_receiver (receiver_user_id, create_time);
-- 若 tb_blog_article 缺少状态索引，执行：
--   ALTER TABLE tb_blog_article ADD INDEX idx_blog_status_time (status, create_time);