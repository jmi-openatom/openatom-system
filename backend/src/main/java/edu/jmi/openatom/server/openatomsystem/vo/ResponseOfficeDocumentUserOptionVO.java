package edu.jmi.openatom.server.openatomsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 办公文档用户选项VO
 *
 * <p>包含用户姓名, 学号, 学院, 专业, 年级, 班级和联系电话等基本信息, 用于办公文档中的用户选择
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseOfficeDocumentUserOptionVO {
  private Integer id;
  private String realName;
  private String studentId;
  private String college;
  private String major;
  private String grade;
  private String className;
  private String phone;
}
