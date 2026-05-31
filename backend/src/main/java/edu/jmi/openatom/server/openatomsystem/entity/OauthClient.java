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
@TableName("oauth_client")
public class OauthClient {
  @TableId(value = "id", type = IdType.AUTO)
  private Integer id;

  @TableField("client_id")
  private String clientId;

  @TableField("client_secret")
  private String clientSecret;

  @TableField("client_name")
  private String clientName;

  @TableField("redirect_uris")
  private String redirectUris;

  private String scopes;

  @TableField("grant_types")
  private String grantTypes;

  private Boolean enabled;

  @TableField("created_at")
  private Timestamp createdAt;

  @TableField("updated_at")
  private Timestamp updatedAt;
}
