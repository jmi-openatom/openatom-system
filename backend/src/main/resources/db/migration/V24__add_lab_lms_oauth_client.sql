INSERT INTO `oauth_client` (`client_id`, `client_secret`, `client_name`, `redirect_uris`, `scopes`, `grant_types`, `enabled`)
SELECT 'lab-lms',
       NULL,
       'OpenAtom Lab LMS',
       'https://lms.jmi-openatom.cn/auth/callback,https://lab.jmi-openatom.cn/auth/callback,http://127.0.0.1:5174/auth/callback',
       'openid profile email roles permissions',
       'authorization_code refresh_token',
       1
WHERE NOT EXISTS (SELECT 1 FROM `oauth_client` WHERE `client_id` = 'lab-lms');

UPDATE `oauth_client`
SET `client_secret` = NULL,
    `enabled` = 1
WHERE `client_id` = 'lab-lms';

UPDATE `oauth_client`
SET `redirect_uris` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM `redirect_uris`), ''), 'https://lms.jmi-openatom.cn/auth/callback')
WHERE `client_id` = 'lab-lms'
  AND FIND_IN_SET('https://lms.jmi-openatom.cn/auth/callback', `redirect_uris`) = 0;

UPDATE `oauth_client`
SET `redirect_uris` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM `redirect_uris`), ''), 'https://lab.jmi-openatom.cn/auth/callback')
WHERE `client_id` = 'lab-lms'
  AND FIND_IN_SET('https://lab.jmi-openatom.cn/auth/callback', `redirect_uris`) = 0;

UPDATE `oauth_client`
SET `redirect_uris` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM `redirect_uris`), ''), 'http://127.0.0.1:5174/auth/callback')
WHERE `client_id` = 'lab-lms'
  AND FIND_IN_SET('http://127.0.0.1:5174/auth/callback', `redirect_uris`) = 0;

UPDATE `oauth_client`
SET `grant_types` = TRIM(CONCAT(COALESCE(NULLIF(TRIM(`grant_types`), ''), ''), ' authorization_code'))
WHERE `client_id` = 'lab-lms'
  AND INSTR(CONCAT(' ', `grant_types`, ' '), ' authorization_code ') = 0;

UPDATE `oauth_client`
SET `grant_types` = TRIM(CONCAT(COALESCE(NULLIF(TRIM(`grant_types`), ''), ''), ' refresh_token'))
WHERE `client_id` = 'lab-lms'
  AND INSTR(CONCAT(' ', `grant_types`, ' '), ' refresh_token ') = 0;

UPDATE `oauth_client`
SET `scopes` = TRIM(CONCAT(COALESCE(NULLIF(TRIM(`scopes`), ''), ''), ' openid'))
WHERE `client_id` = 'lab-lms'
  AND INSTR(CONCAT(' ', `scopes`, ' '), ' openid ') = 0;

UPDATE `oauth_client`
SET `scopes` = TRIM(CONCAT(COALESCE(NULLIF(TRIM(`scopes`), ''), ''), ' profile'))
WHERE `client_id` = 'lab-lms'
  AND INSTR(CONCAT(' ', `scopes`, ' '), ' profile ') = 0;
