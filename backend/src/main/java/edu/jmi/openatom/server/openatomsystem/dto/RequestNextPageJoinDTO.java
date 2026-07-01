package edu.jmi.openatom.server.openatomsystem.dto;

import lombok.Data;

/**
 * Next 页面加入开发申请请求
 */
@Data
public class RequestNextPageJoinDTO {
  private String name;
  private String contact;
  private String direction;
  private String skills;
  private String message;
}
