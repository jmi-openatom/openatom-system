UPDATE `oauth_client`
SET `redirect_uris` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM `redirect_uris`), ''), 'https://jmi-openatom.cn/auth/callback')
WHERE `client_id` = 'openatom-web'
  AND FIND_IN_SET('https://jmi-openatom.cn/auth/callback', `redirect_uris`) = 0;

UPDATE `oauth_client`
SET `redirect_uris` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM `redirect_uris`), ''), 'https://www.jmi-openatom.cn/auth/callback')
WHERE `client_id` = 'openatom-web'
  AND FIND_IN_SET('https://www.jmi-openatom.cn/auth/callback', `redirect_uris`) = 0;

UPDATE `oauth_client`
SET `scopes` = TRIM(CONCAT(COALESCE(NULLIF(TRIM(`scopes`), ''), ''), ' roles'))
WHERE `client_id` = 'openatom-web'
  AND INSTR(CONCAT(' ', `scopes`, ' '), ' roles ') = 0;

UPDATE `oauth_client`
SET `scopes` = TRIM(CONCAT(COALESCE(NULLIF(TRIM(`scopes`), ''), ''), ' permissions'))
WHERE `client_id` = 'openatom-web'
  AND INSTR(CONCAT(' ', `scopes`, ' '), ' permissions ') = 0;
