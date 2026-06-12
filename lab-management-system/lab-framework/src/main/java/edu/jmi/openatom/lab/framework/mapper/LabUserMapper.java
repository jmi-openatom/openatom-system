package edu.jmi.openatom.lab.framework.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.lab.framework.entity.LabUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LabUserMapper extends BaseMapper<LabUser> {}
