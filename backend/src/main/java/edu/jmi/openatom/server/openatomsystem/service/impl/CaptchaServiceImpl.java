package edu.jmi.openatom.server.openatomsystem.service.impl;

import edu.jmi.openatom.server.openatomsystem.service.CaptchaService;
import edu.jmi.openatom.server.openatomsystem.vo.ResponseCaptchaVO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 滑块拼图验证码服务实现
 *
 * <p>生成带随机渐变背景的滑块拼图：背景图从缺口位置抠出拼图块并绘制缺口轮廓，
 * 缺口 X 坐标仅存 Redis（5 分钟过期），校验成功即删除（一次性使用）。
 * 图片以 PNG base64 返回，前端直接渲染
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {
  private static final String CAPTCHA_KEY_PREFIX = "openatom:captcha:slider:";
  private static final long CAPTCHA_TTL_SECONDS = 5 * 60L;
  private static final int WIDTH = 280;
  private static final int HEIGHT = 150;
  private static final int PIECE_SIZE = 44;
  private static final int TAB_RADIUS = 11;
  private static final int PIECE_WIDTH = PIECE_SIZE + TAB_RADIUS;
  private static final int TOLERANCE = 6;
  private static final int MARGIN = PIECE_SIZE + 6;

  private final StringRedisTemplate redisTemplate;
  private final java.security.SecureRandom secureRandom = new java.security.SecureRandom();

  @Override
  public ResponseCaptchaVO generate() {
    String captchaId = UUID.randomUUID().toString().replace("-", "");
    int targetX = MARGIN + secureRandom.nextInt(Math.max(1, WIDTH - PIECE_WIDTH - 2 * MARGIN));
    int pieceY = MARGIN + secureRandom.nextInt(Math.max(1, HEIGHT - PIECE_SIZE - 2 * MARGIN));
    BufferedImage background = renderBackground();
    BufferedImage piece = cutPiece(background, targetX, pieceY);
    drawGap(background, targetX, pieceY);
    redisTemplate
        .opsForValue()
        .set(CAPTCHA_KEY_PREFIX + captchaId, String.valueOf(targetX),
            Duration.ofSeconds(CAPTCHA_TTL_SECONDS));
    return ResponseCaptchaVO.builder()
        .captchaId(captchaId)
        .backgroundBase64(toBase64(background))
        .pieceBase64(toBase64(piece))
        .pieceY(pieceY)
        .build();
  }

  @Override
  public boolean verify(String captchaId, int x) {
    if (captchaId == null || captchaId.isBlank()) {
      return false;
    }
    String key = CAPTCHA_KEY_PREFIX + captchaId;
    String stored = redisTemplate.opsForValue().get(key);
    if (stored == null) {
      return false;
    }
    redisTemplate.delete(key);
    try {
      return Math.abs(x - Integer.parseInt(stored)) <= TOLERANCE;
    } catch (NumberFormatException exception) {
      return false;
    }
  }

  private BufferedImage renderBackground() {
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      Color base = randomColor(120, 230);
      Color accent = randomColor(80, 200);
      graphics.setPaint(
          new java.awt.GradientPaint(0, 0, base, WIDTH, HEIGHT, accent));
      graphics.fillRect(0, 0, WIDTH, HEIGHT);
      for (int i = 0; i < 8; i++) {
        graphics.setColor(randomColor(100, 240));
        graphics.fillOval(
            secureRandom.nextInt(WIDTH + 40) - 20,
            secureRandom.nextInt(HEIGHT + 40) - 20,
            24 + secureRandom.nextInt(46),
            24 + secureRandom.nextInt(46));
      }
      for (int i = 0; i < 10; i++) {
        graphics.setColor(randomColor(90, 220));
        graphics.setStroke(new BasicStroke(1.2f));
        graphics.drawLine(
            secureRandom.nextInt(WIDTH), secureRandom.nextInt(HEIGHT),
            secureRandom.nextInt(WIDTH), secureRandom.nextInt(HEIGHT));
      }
      for (int i = 0; i < 60; i++) {
        graphics.setColor(randomColor(60, 230));
        graphics.fillRect(secureRandom.nextInt(WIDTH), secureRandom.nextInt(HEIGHT), 2, 2);
      }
    } finally {
      graphics.dispose();
    }
    return image;
  }

  private BufferedImage cutPiece(BufferedImage background, int x, int y) {
    BufferedImage piece =
        new BufferedImage(PIECE_WIDTH, PIECE_SIZE, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = piece.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setClip(pieceShape(0, 0));
      graphics.drawImage(background, -x, -y, null);
      graphics.setClip(null);
      graphics.setColor(new Color(255, 255, 255, 70));
      graphics.setStroke(new BasicStroke(1f));
      graphics.draw(pieceShape(0, 0));
    } finally {
      graphics.dispose();
    }
    return piece;
  }

  private void drawGap(BufferedImage background, int x, int y) {
    Graphics2D graphics = background.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setColor(new Color(255, 255, 255, 120));
      graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      graphics.draw(pieceShape(x, y));
    } finally {
      graphics.dispose();
    }
  }

  /** 拼图块形状：圆角方块 + 右侧中部圆形凸起 */
  private Shape pieceShape(int x, int y) {
    Area area =
        new Area(
            new RoundRectangle2D.Float(x, y, PIECE_SIZE, PIECE_SIZE, 14, 14));
    area.add(
        new Area(
            new Ellipse2D.Float(
                x + PIECE_SIZE - TAB_RADIUS,
                y + (PIECE_SIZE - TAB_RADIUS * 2) / 2f,
                TAB_RADIUS * 2,
                TAB_RADIUS * 2)));
    return area;
  }

  private String toBase64(BufferedImage image) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ImageIO.write(image, "png", output);
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    } catch (Exception exception) {
      log.error("captcha image rendering failed", exception);
      throw new IllegalStateException("captcha_render_failed", exception);
    }
  }

  private Color randomColor(int min, int max) {
    int range = Math.max(1, max - min);
    return new Color(
        min + secureRandom.nextInt(range),
        min + secureRandom.nextInt(range),
        min + secureRandom.nextInt(range));
  }
}