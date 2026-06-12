package edu.jmi.openatom.lab.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lab_problem_test_cases")
public class LabProblemTestCase {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long problemId;
  private String inputText;
  private String expectedOutput;
  private Boolean sampleCase;
  private Integer sortOrder;
}
