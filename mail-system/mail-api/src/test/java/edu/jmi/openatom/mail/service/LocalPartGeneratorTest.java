package edu.jmi.openatom.mail.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.jmi.openatom.mail.config.MailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalPartGeneratorTest {
  private LocalPartGenerator generator;

  @BeforeEach
  void setUp() {
    MailProperties properties = new MailProperties();
    properties.setAddressSalt("test-address-salt-with-32-characters");
    generator = new LocalPartGenerator(properties);
  }

  @Test
  void convertsChineseNamesToLowercaseFullPinyin() {
    assertThat(generator.baseFromName("张三")).isEqualTo("zhangsan");
    assertThat(generator.baseFromName("欧阳娜娜")).isEqualTo("ouyangnana");
    assertThat(generator.baseFromName("吕布")).isEqualTo("lvbu");
  }

  @Test
  void removesPunctuationAndRetainsAsciiLettersAndDigits() {
    assertThat(generator.baseFromName(" Alice·张 3 ")).isEqualTo("alicezhang3");
  }

  @Test
  void collisionSuffixIsStableAndDoesNotExposeSubject() {
    String first = generator.candidate("zhangsan", "student-number-123456", 1);
    String repeated = generator.candidate("zhangsan", "student-number-123456", 1);
    assertThat(first).isEqualTo(repeated).matches("zhangsan\\.[a-z2-7]{4}");
    assertThat(first).doesNotContain("123456");
  }

  @Test
  void reservedNamesAreNeverAssignedDirectly() {
    assertThat(generator.candidate("postmaster", "42", 0)).isEqualTo("postmaster.user");
  }
}
