-- Next 页面加入开发申请表
CREATE TABLE IF NOT EXISTS next_page_join (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL COMMENT '姓名',
  contact     VARCHAR(100) NOT NULL COMMENT '联系方式（QQ/微信/邮箱）',
  direction   VARCHAR(50)  NOT NULL COMMENT '感兴趣方向：frontend / backend / devops',
  skills      VARCHAR(500) DEFAULT '' COMMENT '技能栈',
  message     VARCHAR(1000) DEFAULT '' COMMENT '留言',
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT 'openatom-system-next 加入开发申请';
