package edu.jmi.openatom.lab.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("lab_checkin_group")
public class LabCheckInGroup {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String name;
  private Long createdBy;
  private LocalDateTime createdAt;
}
