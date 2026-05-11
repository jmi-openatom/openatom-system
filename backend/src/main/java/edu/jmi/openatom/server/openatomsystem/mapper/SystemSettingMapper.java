package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统设置数据访问层
 *
 * <p>提供对系统设置(SystemSetting)的数据库操作
 */
@Mapper
public interface SystemSettingMapper extends BaseMapper<SystemSetting> {}
