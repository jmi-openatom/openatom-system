package edu.jmi.openatom.server.openatomsystem.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class SharedFileControllerCreateTest {

  private static byte[] initialContent(String type, String name) throws Exception {
    SharedFileController controller = new SharedFileController(null, null, null, null);
    Method method =
        SharedFileController.class.getDeclaredMethod("initialFileContent", String.class, String.class);
    method.setAccessible(true);
    return (byte[]) method.invoke(controller, type, name);
  }

  private static String resolveName(String name, String type) throws Exception {
    SharedFileController controller = new SharedFileController(null, null, null, null);
    Method method =
        SharedFileController.class.getDeclaredMethod("resolveNewFileName", String.class, String.class);
    method.setAccessible(true);
    return (String) method.invoke(controller, name, type);
  }

  @Test
  void generatesValidDocx() throws Exception {
    byte[] bytes = initialContent("docx", "未命名文档.docx");
    assertTrue(bytes.length > 0);
    try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
      assertNotNull(document);
    }
  }

  @Test
  void generatesValidXlsx() throws Exception {
    byte[] bytes = initialContent("xlsx", "未命名表格.xlsx");
    assertTrue(bytes.length > 0);
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      assertEquals(1, workbook.getNumberOfSheets());
    }
  }

  @Test
  void generatesValidPptx() throws Exception {
    byte[] bytes = initialContent("pptx", "未命名演示文稿.pptx");
    assertTrue(bytes.length > 0);
    try (XMLSlideShow slideshow = new XMLSlideShow(new ByteArrayInputStream(bytes))) {
      assertEquals(1, slideshow.getSlides().size());
    }
  }

  @Test
  void generatesMarkdownTemplate() throws Exception {
    byte[] bytes = initialContent("md", "我的笔记.md");
    String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(text.startsWith("# 我的笔记"));
  }

  @Test
  void resolvesNamesWithDefaultsAndStrippedExtensions() throws Exception {
    assertEquals("未命名文档.docx", resolveName(null, "docx"));
    assertEquals("未命名表格.xlsx", resolveName("", "xlsx"));
    assertEquals("我的笔记.md", resolveName("我的笔记", "md"));
    assertEquals("我的笔记.md", resolveName("我的笔记.txt", "md"));
    assertEquals("v1.0 文档.docx", resolveName("v1.0 文档", "docx"));
    assertEquals("报告.pptx", resolveName("报告.pptx", "pptx"));
  }
}