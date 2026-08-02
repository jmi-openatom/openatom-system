INSERT INTO `oauth_client` (`client_id`, `client_secret`, `client_name`, `redirect_uris`, `scopes`, `grant_types`, `enabled`)
SELECT 'openatom-seafile',
       NULL,
       'OpenAtom Seafile',
       'https://cloud.jmi-openatom.cn/oauth/callback/',
       'openid profile email',
       'authorization_code refresh_token',
       1
WHERE NOT EXISTS (SELECT 1 FROM `oauth_client` WHERE `client_id` = 'openatom-seafile');

UPDATE `oauth_client`
SET `client_name` = 'OpenAtom Seafile',
    `redirect_uris` = 'https://cloud.jmi-openatom.cn/oauth/callback/',
    `scopes` = 'openid profile email',
    `grant_types` = 'authorization_code refresh_token',
    `enabled` = 1
WHERE `client_id` = 'openatom-seafile';
