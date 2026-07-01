-- Next 页面统计数据表
CREATE TABLE IF NOT EXISTS next_page_stats (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  view_count  INT NOT NULL DEFAULT 0 COMMENT '页面访问次数',
  like_count  INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT 'openatom-system-next 页面统计';

-- 初始化一条记录（如果不存在）
INSERT INTO next_page_stats (view_count, like_count)
SELECT 0, 0
WHERE NOT EXISTS (SELECT 1 FROM next_page_stats);
