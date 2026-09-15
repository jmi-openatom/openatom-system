package edu.jmi.openatom.quest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("quest_oauth_identity")
public class OauthIdentity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memberId;
    private String provider;
    private String subject;
    private String displayName;
    private String avatarUrl;
    private String email;
    private LocalDateTime firstLoginAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
