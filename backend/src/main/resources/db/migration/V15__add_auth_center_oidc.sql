CREATE TABLE IF NOT EXISTS `oauth_client`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `client_id` VARCHAR(80) NOT NULL COMMENT '客户端ID',
    `client_secret` VARCHAR(120) DEFAULT NULL COMMENT '客户端密钥BCrypt哈希，公开客户端可为空',
    `client_name` VARCHAR(120) NOT NULL COMMENT '客户端名称',
    `redirect_uris` TEXT NOT NULL COMMENT '允许回调地址，逗号分隔',
    `scopes` VARCHAR(500) NOT NULL DEFAULT 'openid profile email' COMMENT '允许scope，空格分隔',
    `grant_types` VARCHAR(300) NOT NULL DEFAULT 'authorization_code refresh_token' COMMENT '允许授权类型，空格分隔',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_client_id` (`client_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='OAuth/OIDC客户端';

CREATE TABLE IF NOT EXISTS `oauth_authorization_code`
(
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code` VARCHAR(160) NOT NULL COMMENT '授权码',
    `client_id` VARCHAR(80) NOT NULL COMMENT '客户端ID',
    `user_id` INT NOT NULL COMMENT '授权用户ID',
    `redirect_uri` VARCHAR(500) NOT NULL COMMENT '回调地址',
    `scope` VARCHAR(500) NOT NULL DEFAULT 'openid profile' COMMENT '授权scope',
    `code_challenge` VARCHAR(160) DEFAULT NULL COMMENT 'PKCE challenge',
    `code_challenge_method` VARCHAR(20) DEFAULT NULL COMMENT 'PKCE challenge方法',
    `nonce` VARCHAR(160) DEFAULT NULL COMMENT 'OIDC nonce',
    `state` VARCHAR(300) DEFAULT NULL COMMENT '客户端state',
    `expires_at` TIMESTAMP NOT NULL COMMENT '过期时间',
    `consumed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '消费时间',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_authorization_code` (`code`),
    KEY `idx_oauth_authorization_code_client` (`client_id`),
    KEY `idx_oauth_authorization_code_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='OAuth/OIDC授权码';

INSERT INTO `oauth_client` (`client_id`, `client_secret`, `client_name`, `redirect_uris`, `scopes`, `grant_types`)
SELECT 'openatom-web',
       NULL,
       'OpenAtom Web',
       'https://jmi-openatom.cn/auth/callback,https://www.jmi-openatom.cn/auth/callback,http://localhost:18080/auth/callback,http://127.0.0.1:18080/auth/callback,http://localhost:5173/auth/callback,http://localhost:5175/auth/callback,http://127.0.0.1:5173/auth/callback,http://127.0.0.1:5175/auth/callback',
       'openid profile email roles permissions',
       'authorization_code refresh_token'
WHERE NOT EXISTS (SELECT 1 FROM `oauth_client` WHERE `client_id` = 'openatom-web');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '查询认证应用', 'oauth-client:list', 'api', '/oauth/admin/clients', 'GET'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'oauth-client:list');

INSERT INTO `sys_permission` (`name`, `code`, `type`, `path`, `method`)
SELECT '管理认证应用', 'oauth-client:manage', 'api', '/oauth/admin/clients/**', 'POST'
WHERE NOT EXISTS (SELECT 1 FROM `sys_permission` WHERE `code` = 'oauth-client:manage');

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `sys_role` r
         JOIN `sys_permission` p ON p.`code` IN ('oauth-client:list', 'oauth-client:manage')
WHERE r.`code` = 'super_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_role_permission` rp
    WHERE rp.`role_id` = r.`id`
      AND rp.`permission_id` = p.`id`
);
