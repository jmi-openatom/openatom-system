package edu.jmi.openatom.quest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("quest_technical_direction")
public class TechnicalDirection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String directionKey;
    private String name;
    private String description;
    private Integer sortOrder;
    private String status;
}
