package edu.jmi.openatom.server.openatomsystem.service;

import edu.jmi.openatom.server.openatomsystem.vo.ResponseCaptchaVO;

/**
 * 滑块拼图验证码服务
 *
 * <p>自建滑块拼图验证码，完全本地生成，不依赖任何第三方服务，中国大陆网络环境下可用。
 * 拼图缺口位置存 Redis（5 分钟过期），一次性使用，防止机器人批量调用发送邮件接口
 */
public interface CaptchaService {

  /** 生成一张滑块拼图验证码（背景图 + 拼图块），缺口 X 坐标仅存于 Redis */
  ResponseCaptchaVO generate();

  /** 校验滑块位置（校验后即删除，一次性使用） */
  boolean verify(String captchaId, int x);
}