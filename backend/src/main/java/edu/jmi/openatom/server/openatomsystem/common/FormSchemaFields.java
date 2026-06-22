package edu.jmi.openatom.server.openatomsystem.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 系统表单的固定字段定义与兼容补全。 */
public final class FormSchemaFields {
  public static final String COLLEGE_KEY = "college";
  public static final String COLLEGE_LABEL = "学院";
  public static final List<String> COLLEGES =
      List.of(
          "航海技术学院",
          "轮机与电气工程学院",
          "船舶与智能制造学院",
          "经济管理学院",
          "信息工程学院",
          "邮轮与艺术设计学院",
          "国防教育与定向培养学院",
          "创新创业学院",
          "国际教育学院");

  private FormSchemaFields() {}

  public static List<Map<String, Object>> ensureCollegeField(List<Map<String, Object>> source) {
    List<Map<String, Object>> fields = copyFields(source);
    int existingIndex = findCollegeIndex(fields);
    Map<String, Object> collegeField = keyedCollegeField();
    if (existingIndex >= 0) {
      collegeField.putAll(fields.get(existingIndex));
      applyKeyedCollegeDefinition(collegeField);
      fields.set(existingIndex, collegeField);
      return fields;
    }

    int studentIdIndex = findKeyIndex(fields, "studentId");
    fields.add(studentIdIndex >= 0 ? studentIdIndex + 1 : Math.min(1, fields.size()), collegeField);
    return fields;
  }

  public static String ensureCollegeFieldJson(String value) {
    return Jsons.stringify(ensureCollegeField(Jsons.parseListOfObjects(value)));
  }

  public static Object ensureCollegeRegistrationFields(Object value) {
    List<Map<String, Object>> fields = parseFields(value);
    int existingIndex = findCollegeIndex(fields);
    Map<String, Object> collegeField = registrationCollegeField();
    if (existingIndex >= 0) {
      collegeField.putAll(fields.get(existingIndex));
      applyRegistrationCollegeDefinition(collegeField);
      fields.set(existingIndex, collegeField);
      return fields;
    }

    int nameIndex = findLabelIndex(fields, "姓名");
    fields.add(nameIndex >= 0 ? nameIndex + 1 : 0, collegeField);
    return fields;
  }

  public static String ensureCollegeRegistrationFieldsJson(String value) {
    return Jsons.stringify(ensureCollegeRegistrationFields(value));
  }

  private static List<Map<String, Object>> parseFields(Object value) {
    if (value == null) return new ArrayList<>();
    if (value instanceof String stringValue) {
      return copyFields(Jsons.parseListOfObjects(stringValue));
    }
    return copyFields(Jsons.parseListOfObjects(Jsons.stringify(value)));
  }

  private static List<Map<String, Object>> copyFields(List<Map<String, Object>> source) {
    List<Map<String, Object>> fields = new ArrayList<>();
    if (source == null) return fields;
    for (Map<String, Object> field : source) {
      if (field != null) fields.add(new LinkedHashMap<>(field));
    }
    return fields;
  }

  private static Map<String, Object> keyedCollegeField() {
    Map<String, Object> field = new LinkedHashMap<>();
    applyKeyedCollegeDefinition(field);
    return field;
  }

  private static Map<String, Object> registrationCollegeField() {
    Map<String, Object> field = new LinkedHashMap<>();
    applyRegistrationCollegeDefinition(field);
    return field;
  }

  private static void applyKeyedCollegeDefinition(Map<String, Object> field) {
    field.put("key", COLLEGE_KEY);
    field.put("label", COLLEGE_LABEL);
    field.put("type", "select");
    field.put("required", true);
    field.put("placeholder", "请选择学院");
    field.put(
        "options",
        COLLEGES.stream()
            .map(college -> Map.<String, Object>of("label", college, "value", college))
            .toList());
  }

  private static void applyRegistrationCollegeDefinition(Map<String, Object> field) {
    field.put("label", COLLEGE_LABEL);
    field.put("type", "select");
    field.put("required", true);
    field.put("options", COLLEGES);
  }

  private static int findCollegeIndex(List<Map<String, Object>> fields) {
    for (int i = 0; i < fields.size(); i++) {
      Map<String, Object> field = fields.get(i);
      String key = text(field.get("key"));
      String label = text(field.get("label"));
      if (COLLEGE_KEY.equalsIgnoreCase(key) || COLLEGE_LABEL.equals(label)) return i;
    }
    return -1;
  }

  private static int findKeyIndex(List<Map<String, Object>> fields, String key) {
    for (int i = 0; i < fields.size(); i++) {
      if (key.equals(text(fields.get(i).get("key")))) return i;
    }
    return -1;
  }

  private static int findLabelIndex(List<Map<String, Object>> fields, String label) {
    for (int i = 0; i < fields.size(); i++) {
      if (label.equals(text(fields.get(i).get("label")))) return i;
    }
    return -1;
  }

  private static String text(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }
}
