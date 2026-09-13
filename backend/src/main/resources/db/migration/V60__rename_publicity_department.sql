-- 将存量部门名称统一为“宣传部”，并同步部门在统一分组中心中的投影名称。
UPDATE `club_department`
SET `name` = '宣传部'
WHERE `name` = '宣传组';

UPDATE `unified_group` unified_group
JOIN `club_department` department
  ON unified_group.`source_type` = 'department'
 AND unified_group.`source_id` = CAST(department.`id` AS CHAR)
SET unified_group.`name` = department.`name`
WHERE department.`name` = '宣传部'
  AND unified_group.`name` = '宣传组';
