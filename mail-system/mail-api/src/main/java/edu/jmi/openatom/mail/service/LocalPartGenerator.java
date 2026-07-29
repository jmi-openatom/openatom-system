package edu.jmi.openatom.mail.service;

import edu.jmi.openatom.mail.config.MailProperties;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.stereotype.Component;

@Component
public class LocalPartGenerator {
  private static final String BASE32 = "abcdefghijklmnopqrstuvwxyz234567";
  private static final Set<String> RESERVED =
      Set.of("admin", "administrator", "postmaster", "abuse", "security", "support", "root");
  private final MailProperties properties;
  private final HanyuPinyinOutputFormat format;

  public LocalPartGenerator(MailProperties properties) {
    this.properties = properties;
    format = new HanyuPinyinOutputFormat();
    format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
    format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    format.setVCharType(HanyuPinyinVCharType.WITH_V);
  }

  public String baseFromName(String displayName) {
    if (displayName == null || displayName.isBlank()) {
      return null;
    }
    StringBuilder result = new StringBuilder();
    String normalized = Normalizer.normalize(displayName.trim(), Normalizer.Form.NFKC);
    for (char value : normalized.toCharArray()) {
      if (value < 128 && Character.isLetterOrDigit(value)) {
        result.append(Character.toLowerCase(value));
      } else if (Character.UnicodeScript.of(value) == Character.UnicodeScript.HAN) {
        appendPinyin(result, value);
      }
    }
    String localPart = sanitize(result.toString());
    return localPart.isBlank() ? null : localPart;
  }

  public String candidate(String base, String oauthSub, int attempt) {
    String safeBase = RESERVED.contains(base) ? base + ".user" : base;
    if (attempt == 0) {
      return truncate(safeBase, 48);
    }
    int suffixLength = attempt == 1 ? 4 : Math.min(4 + (attempt - 1) * 2, 16);
    String suffix = stableSuffix(oauthSub, suffixLength);
    return truncate(safeBase, 48 - suffix.length() - 1) + "." + suffix;
  }

  public String validateManual(String localPart) {
    String normalized = sanitize(localPart == null ? "" : localPart.toLowerCase(Locale.ROOT));
    if (!normalized.equals(localPart) || normalized.length() < 2 || RESERVED.contains(normalized)) {
      throw new IllegalArgumentException("invalid_or_reserved_local_part");
    }
    return normalized;
  }

  private void appendPinyin(StringBuilder target, char value) {
    try {
      String[] values = PinyinHelper.toHanyuPinyinStringArray(value, format);
      if (values != null && values.length > 0) {
        target.append(values[0]);
      }
    } catch (BadHanyuPinyinOutputFormatCombination exception) {
      throw new IllegalStateException("Invalid pinyin output configuration", exception);
    }
  }

  private String stableSuffix(String oauthSub, int length) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(properties.getAddressSalt().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(oauthSub.getBytes(StandardCharsets.UTF_8));
      StringBuilder encoded = new StringBuilder();
      int buffer = 0;
      int bits = 0;
      for (byte item : digest) {
        buffer = (buffer << 8) | (item & 0xff);
        bits += 8;
        while (bits >= 5 && encoded.length() < length) {
          bits -= 5;
          encoded.append(BASE32.charAt((buffer >> bits) & 31));
        }
      }
      return encoded.substring(0, length);
    } catch (Exception exception) {
      throw new IllegalStateException("Cannot generate stable mailbox suffix", exception);
    }
  }

  private String sanitize(String value) {
    String sanitized = value.replaceAll("[^a-z0-9.]", "").replaceAll("\\.{2,}", ".");
    sanitized = sanitized.replaceAll("^\\.+|\\.+$", "");
    return truncate(sanitized, 48);
  }

  private String truncate(String value, int maximum) {
    return value.length() <= maximum ? value : value.substring(0, maximum);
  }
}
