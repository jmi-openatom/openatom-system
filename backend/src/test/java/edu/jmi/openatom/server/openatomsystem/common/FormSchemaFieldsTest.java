package edu.jmi.openatom.server.openatomsystem.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormSchemaFieldsTest {
  @Test
  void addsRequiredCollegeSelectAfterStudentId() {
    List<Map<String, Object>> fields =
        FormSchemaFields.ensureCollegeField(
            List.of(
                Map.of("key", "name", "label", "姓名", "type", "text"),
                Map.of("key", "studentId", "label", "学号", "type", "text")));

    assertEquals("college", fields.get(2).get("key"));
    assertEquals("select", fields.get(2).get("type"));
    assertEquals(true, fields.get(2).get("required"));
    assertEquals(9, ((List<?>) fields.get(2).get("options")).size());
  }

  @Test
  void replacesExistingCollegeFieldWithoutDuplicatingIt() {
    List<Map<String, Object>> fields =
        FormSchemaFields.ensureCollegeField(
            List.of(Map.of("key", "department", "label", "学院", "type", "text")));

    assertEquals(1, fields.size());
    assertEquals("college", fields.get(0).get("key"));
    assertEquals("select", fields.get(0).get("type"));
    assertTrue(FormSchemaFields.COLLEGES.contains("信息工程学院"));
  }
}
