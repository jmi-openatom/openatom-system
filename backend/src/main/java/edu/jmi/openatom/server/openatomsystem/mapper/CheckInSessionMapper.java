package edu.jmi.openatom.server.openatomsystem.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jmi.openatom.server.openatomsystem.entity.CheckInSession;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInSessionMapper extends BaseMapper<CheckInSession> {
  default CheckInSession selectByToken(String token) {
    return selectOne(
        new LambdaQueryWrapper<CheckInSession>()
            .eq(CheckInSession::getToken, token)
            .last("LIMIT 1"));
  }

  default List<CheckInSession> selectByClubAndStatus(Integer clubId, String status) {
    return selectList(
        new LambdaQueryWrapper<CheckInSession>()
            .eq(CheckInSession::getClubId, clubId)
            .eq(status != null && !status.isBlank(), CheckInSession::getStatus, status)
            .orderByDesc(CheckInSession::getId));
  }
}
