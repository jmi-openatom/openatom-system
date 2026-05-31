package edu.jmi.openatom.server.openatomsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("oauth_authorization_code")
public class OauthAuthorizationCode {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  private String code;

  @TableField("client_id")
  private String clientId;

  @TableField("user_id")
  private Integer userId;

  @TableField("redirect_uri")
  private String redirectUri;

  private String scope;

  @TableField("code_challenge")
  private String codeChallenge;

  @TableField("code_challenge_method")
  private String codeChallengeMethod;

  private String nonce;
  private String state;

  @TableField("expires_at")
  private Timestamp expiresAt;

  @TableField("consumed_at")
  private Timestamp consumedAt;

  @TableField("created_at")
  private Timestamp createdAt;
}
