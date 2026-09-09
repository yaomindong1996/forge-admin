package com.mdframe.forge.starter.auth.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.starter.auth.config.CaptchaProperties;
import com.mdframe.forge.starter.auth.domain.CaptchaResult;
import com.mdframe.forge.starter.auth.domain.SliderCaptchaResult;
import com.mdframe.forge.starter.auth.domain.EmailCaptchaResult;
import com.mdframe.forge.starter.auth.domain.SmsCaptchaResult;
import com.mdframe.forge.starter.auth.email.EmailCaptchaSender;
import com.mdframe.forge.starter.auth.service.ICaptchaService;
import com.mdframe.forge.starter.auth.sms.SmsCaptchaSender;
import com.mdframe.forge.starter.cache.service.ICacheService;
import com.mdframe.forge.starter.core.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 验证码服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements ICaptchaService {

    private final ICacheService cacheService;
    private final CaptchaProperties captchaProperties;
    private final Environment environment;
    private final Optional<SmsCaptchaSender> smsCaptchaSender;
    private final Optional<EmailCaptchaSender> emailCaptchaSender;

    /**
     * 验证码缓存key前缀
     */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /**
     * 滑块验证码一次性消费标记前缀。
     */
    private static final String SLIDER_CONSUMED_KEY_PREFIX = "captcha:slider:consumed:";

    /** 挑战缓存协议版本，便于未来平滑升级。 */
    private static final String SLIDER_CHALLENGE_VERSION = "v2";
    private static final Duration MAX_SLIDER_CHALLENGE_DURATION = Duration.ofMinutes(5);

    /** 开发测试环境的进程内临时密钥，生产环境不会使用。 */
    private static final String DEV_CHALLENGE_SECRET = createDevelopmentSecret();

    /**
     * 默认验证码长度
     */
    private static final int DEFAULT_LENGTH = 4;

    /**
     * 验证码图片宽度
     */
    private static final int IMAGE_WIDTH = 130;

    /**
     * 验证码图片高度
     */
    private static final int IMAGE_HEIGHT = 48;

    /**
     * 默认过期时间（5分钟）
     */
    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(5);

    /**
     * 字符集：数字+字母（不含易混淆字符）
     */
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * 短信验证码长度
     */
    private static final int SMS_CODE_LENGTH = 6;

    /**
     * 短信验证码发送间隔（秒）
     */
    private static final int SMS_SEND_INTERVAL = 60;

    /**
     * 滑块验证码容错范围（像素）
     */
    private static final int SLIDER_TOLERANCE = 8;

    /**
     * 滑块验证码图片宽度
     */
    private static final int SLIDER_BG_WIDTH = 320;

    /**
     * 滑块验证码图片高度
     */
    private static final int SLIDER_BG_HEIGHT = 160;

    /**
     * 滑块宽度
     */
    private static final int SLIDER_WIDTH = 50;

    /**
     * 滑块高度
     */
    private static final int SLIDER_HEIGHT = 50;

    /**
     * 滑块轨道宽度（与前端保持一致）
     */
    private static final int SLIDER_TRACK_WIDTH = 320;

    @Override
    public String generateCaptcha() {
        return generateCaptcha(DEFAULT_LENGTH, DEFAULT_DURATION);
    }

    @Override
    public String generateCaptcha(int length) {
        return generateCaptcha(length, DEFAULT_DURATION);
    }

    @Override
    public String generateCaptcha(int length, Duration duration) {
        // 生成验证码字符串
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(CAPTCHA_CHARS.charAt(RandomUtil.randomInt(CAPTCHA_CHARS.length())));
        }
        String codeStr = code.toString();
        
        // 生成唯一key
        String key = UUID.randomUUID().toString().replace("-", "");
        
        // 存入缓存
        String cacheKey = CAPTCHA_KEY_PREFIX + key;
        cacheService.set(cacheKey, codeStr, duration);
        
        log.debug("生成验证码: key={}", key);
        return key;
    }

    @Override
    public String getCaptcha(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        String cacheKey = CAPTCHA_KEY_PREFIX + key;
        return cacheService.get(cacheKey);
    }

    @Override
    public boolean validate(String key, String code) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(code)) {
            return false;
        }
        
        String cacheCode = getCaptcha(key);
        if (cacheCode == null) {
            log.warn("验证码不存在或已过期: key={}", key);
            return false;
        }
        
        // 忽略大小写比较
        boolean match = code.equalsIgnoreCase(cacheCode);
        log.debug("验证码校验: key={}, result={}", key, match);
        return match;
    }

    @Override
    public boolean validateAndDelete(String key, String code) {
        boolean valid = validate(key, code);
        if (valid) {
            deleteCaptcha(key);
        }
        return valid;
    }

    @Override
    public void deleteCaptcha(String key) {
        if (StrUtil.isBlank(key)) {
            return;
        }
        String cacheKey = CAPTCHA_KEY_PREFIX + key;
        cacheService.delete(cacheKey);
        log.debug("删除验证码: key={}", key);
    }

    /**
     * 生成图形验证码
     * @return 验证码结果，包含key、code（开发环境）和base64图片
     */
    public CaptchaResult generateGraphicCaptcha() {
        return generateGraphicCaptcha(DEFAULT_LENGTH, DEFAULT_DURATION);
    }

    /**
     * 生成图形验证码
     * @param length 验证码长度
     * @param duration 过期时间
     * @return 验证码结果，包含key、code（开发环境）和base64图片
     */
    public CaptchaResult generateGraphicCaptcha(int length, Duration duration) {
        // 先生成验证码字符串
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(CAPTCHA_CHARS.charAt(RandomUtil.randomInt(CAPTCHA_CHARS.length())));
        }
        String codeStr = code.toString();
        
        // 创建线干扰的验证码（Hutool内置）
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(IMAGE_WIDTH, IMAGE_HEIGHT, length, 50);
        
        // 使用反射设置自定义验证码文本
        try {
            java.lang.reflect.Field codeField = cn.hutool.captcha.AbstractCaptcha.class.getDeclaredField("code");
            codeField.setAccessible(true);
            codeField.set(captcha, codeStr);
        } catch (Exception e) {
            log.warn("设置验证码文本失败，使用默认生成的验证码", e);
        }
        
        // 生成验证码图片
        String imageBase64 = captcha.getImageBase64Data();
        
        // 生成唯一key
        String key = UUID.randomUUID().toString().replace("-", "");
        
        // 存入缓存（使用实际生成的验证码）
        String actualCode = captcha.getCode();
        String cacheKey = CAPTCHA_KEY_PREFIX + key;
        cacheService.set(cacheKey, actualCode, duration);
        
        log.debug("生成图形验证码: key={}", key);
        
        // 构建返回结果
        return CaptchaResult.builder()
                .codeKey(key)
                .code(isDevelopmentEchoEnabled() ? actualCode : null)
                .image(imageBase64)
                .expiresIn(duration.getSeconds())
                .build();
    }

    // ==================== 滑块验证码 ====================

    @Override
    public SliderCaptchaResult generateSliderCaptcha() {
        return generateSliderCaptcha(DEFAULT_DURATION);
    }

    @Override
    public SliderCaptchaResult generateSliderCaptcha(Duration duration) {
        try {
            Duration challengeDuration = normalizeSliderDuration(duration);
            // 1. 生成随机位置（滑块缺口位置）
            // 确保滑块不会出现在边缘，留有一定边距
            // x 的范围应该考虑滑块宽度和轨道宽度
            int minX = 60;  // 左边距
            int maxX = SLIDER_BG_WIDTH - SLIDER_WIDTH - 60;  // 右边距
            int minY = 40;
            int maxY = SLIDER_BG_HEIGHT - SLIDER_HEIGHT - 40;

            int x = ThreadLocalRandom.current().nextInt(minX, maxX);
            int y = ThreadLocalRandom.current().nextInt(minY, maxY);

            // 2. 生成验证码图片（使用更现代化的设计）
            BufferedImage bgImage = generateModernSliderBackground();
            BufferedImage sliderImage = generateModernSliderBlock(bgImage, x, y);

            // 3. 在背景图上添加缺口效果
            BufferedImage finalBgImage = addModernNotch(bgImage, x, y);

            // 4. 转换为Base64
            String bgBase64 = imageToBase64(finalBgImage);
            String sliderBase64 = imageToBase64(sliderImage);

            // 5. 生成唯一key
            String key = UUID.randomUUID().toString().replace("-", "");

            // 6. 存储正确答案到缓存（存储x坐标）
            // 注意：这里的x是相对于背景图的绝对位置
            String cacheKey = SLIDER_CAPTCHA_KEY_PREFIX + key;
            String binding = requestBinding();
            long expiresAt = Instant.now().plus(challengeDuration).toEpochMilli();
            String signature = signChallenge(key, x, binding, expiresAt);
            cacheService.set(cacheKey,
                    SLIDER_CHALLENGE_VERSION + "." + x + "." + binding + "." + expiresAt + "." + signature,
                    challengeDuration);

            log.debug("生成滑块验证码: key={}", key);

            // 7. 构建返回结果
            return SliderCaptchaResult.builder()
                    .codeKey(key)
                    .backgroundImage(bgBase64)
                    .sliderImage(sliderBase64)
                    .sliderWidth(SLIDER_WIDTH)
                    .sliderHeight(SLIDER_HEIGHT)
                    .backgroundWidth(SLIDER_BG_WIDTH)
                    .backgroundHeight(SLIDER_BG_HEIGHT)
                    .notchY(y)  // 返回Y坐标，前端需要知道滑块在哪个高度
                    .expiresIn(challengeDuration.getSeconds())
                    .captchaType("slider")
                    .build();

        } catch (Exception e) {
            log.error("生成滑块验证码失败", e);
            throw new RuntimeException("生成滑块验证码失败", e);
        }
    }

    @Override
    public boolean validateSliderCaptcha(String key, Integer moveX) {
        if (StrUtil.isBlank(key) || moveX == null || moveX < 0 || moveX > SLIDER_TRACK_WIDTH) {
            return false;
        }

        String cacheKey = SLIDER_CAPTCHA_KEY_PREFIX + key;
        String correctX = cacheService.get(cacheKey);

        if (correctX == null) {
            log.warn("滑块验证码不存在或已过期: key={}", key);
            return false;
        }

        try {
            String[] challenge = correctX.split("\\.", -1);
            if (challenge.length != 5 || !SLIDER_CHALLENGE_VERSION.equals(challenge[0])) {
                log.warn("滑块验证码缓存协议无效: key={}", key);
                return false;
            }
            int expectedX = Integer.parseInt(challenge[1]);
            String binding = challenge[2];
            long expiresAt = Long.parseLong(challenge[3]);
            String signature = challenge[4];
            if (expiresAt <= System.currentTimeMillis()) {
                log.warn("滑块验证码已过期: key={}", key);
                return false;
            }
            if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.US_ASCII),
                    signChallenge(key, expectedX, binding, expiresAt).getBytes(StandardCharsets.US_ASCII))) {
                log.warn("滑块验证码挑战签名无效: key={}", key);
                return false;
            }
            if (!MessageDigest.isEqual(binding.getBytes(StandardCharsets.US_ASCII),
                    requestBinding().getBytes(StandardCharsets.US_ASCII))) {
                log.warn("滑块验证码请求绑定不匹配: key={}", key);
                return false;
            }
            // 允许一定的误差范围
            boolean match = Math.abs(moveX - expectedX) <= SLIDER_TOLERANCE;
            log.debug("滑块验证码校验: key={}, result={}", key, match);
            return match;
        } catch (NumberFormatException e) {
            log.warn("滑块验证码缓存格式无效: key={}", key);
            return false;
        }
    }

    @Override
    public boolean validateAndDeleteSliderCaptcha(String key, Integer moveX) {
        if (StrUtil.isBlank(key) || moveX == null || moveX < 0 || moveX > SLIDER_TRACK_WIDTH) {
            return false;
        }
        String cacheKey = SLIDER_CAPTCHA_KEY_PREFIX + key;
        if (cacheService.get(cacheKey) == null) {
            return false;
        }
        String consumedKey = SLIDER_CONSUMED_KEY_PREFIX + key;
        boolean firstConsumer = cacheService.setIfAbsent(consumedKey, Boolean.TRUE, 5, TimeUnit.MINUTES);
        if (!firstConsumer) {
            return false;
        }
        try {
            // 无论答案正确与否都消费挑战，避免错误答案被无限重试。
            return validateSliderCaptcha(key, moveX);
        } finally {
            cacheService.delete(cacheKey);
        }
    }

    /**
     * 返回当前请求的稳定绑定摘要。只使用容器解析出的远端地址和 User-Agent，
     * 不信任客户端可任意伪造的 X-Forwarded-For。
     */
    private String requestBinding() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return sha256("no-request");
        }
        HttpServletRequest request = attributes.getRequest();
        String remoteAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String sessionId = request.getRequestedSessionId();
        return sha256(String.join("|",
                remoteAddress == null ? "" : remoteAddress,
                userAgent == null ? "" : userAgent,
                sessionId == null ? "" : sessionId));
    }

    private String signChallenge(String key, int expectedX, String binding, long expiresAt) {
        String secret = captchaProperties.getChallengeSecret();
        if (StrUtil.isBlank(secret)) {
            if (!isDevelopmentOnlyProfile()) {
                throw new IllegalStateException("CAPTCHA_CHALLENGE_SECRET_REQUIRED");
            }
            secret = DEV_CHALLENGE_SECRET;
        } else if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("CAPTCHA_CHALLENGE_SECRET_TOO_SHORT");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((key + "|" + expectedX + "|" + binding + "|" + expiresAt)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("CAPTCHA_CHALLENGE_SIGN_FAILED", exception);
        }
    }

    private boolean isDevelopmentOnlyProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean development = false;
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)
                    || "stage".equalsIgnoreCase(profile) || "staging".equalsIgnoreCase(profile)) {
                return false;
            }
            if ("dev".equalsIgnoreCase(profile) || "local".equalsIgnoreCase(profile)
                    || "test".equalsIgnoreCase(profile)) {
                development = true;
            }
        }
        return development;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("CAPTCHA_BINDING_HASH_FAILED", exception);
        }
    }

    private static String createDevelopmentSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    private Duration normalizeSliderDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("滑块验证码有效期必须大于0");
        }
        return duration.compareTo(MAX_SLIDER_CHALLENGE_DURATION) > 0
                ? MAX_SLIDER_CHALLENGE_DURATION : duration;
    }

    /**
     * 生成现代化滑块验证码背景图
     */
    private BufferedImage generateModernSliderBackground() {
        BufferedImage image = new BufferedImage(SLIDER_BG_WIDTH, SLIDER_BG_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制现代渐变背景
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(248, 250, 252),
                SLIDER_BG_WIDTH, SLIDER_BG_HEIGHT, new Color(241, 245, 249));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, SLIDER_BG_WIDTH, SLIDER_BG_HEIGHT);

        // 添加几何图案装饰
        g2d.setColor(new Color(226, 232, 240, 100));
        // 绘制一些圆点
        for (int i = 0; i < 15; i++) {
            int cx = RandomUtil.randomInt(20, SLIDER_BG_WIDTH - 20);
            int cy = RandomUtil.randomInt(20, SLIDER_BG_HEIGHT - 20);
            int r = RandomUtil.randomInt(3, 8);
            g2d.fillOval(cx - r, cy - r, r * 2, r * 2);
        }

        // 绘制一些线条
        g2d.setStroke(new BasicStroke(1.5f));
        for (int i = 0; i < 8; i++) {
            int x1 = RandomUtil.randomInt(SLIDER_BG_WIDTH);
            int y1 = RandomUtil.randomInt(SLIDER_BG_HEIGHT);
            int x2 = x1 + RandomUtil.randomInt(-60, 60);
            int y2 = y1 + RandomUtil.randomInt(-60, 60);
            g2d.drawLine(x1, y1, x2, y2);
        }

        g2d.dispose();
        return image;
    }

    /**
     * 生成现代化滑块（拼图块）
     */
    private BufferedImage generateModernSliderBlock(BufferedImage bgImage, int x, int y) {
        // 创建滑块图片，带透明通道
        BufferedImage sliderImage = new BufferedImage(SLIDER_WIDTH, SLIDER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = sliderImage.createGraphics();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 从背景图中裁剪出滑块区域
        BufferedImage subImage = bgImage.getSubimage(x, y, SLIDER_WIDTH, SLIDER_HEIGHT);

        // 创建圆角矩形裁剪区域
        g2d.setComposite(AlphaComposite.Src);
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, SLIDER_WIDTH, SLIDER_HEIGHT);

        // 绘制圆角矩形
        g2d.setComposite(AlphaComposite.SrcOver);
        int cornerRadius = 8;
        g2d.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, SLIDER_WIDTH, SLIDER_HEIGHT, cornerRadius, cornerRadius));
        g2d.drawImage(subImage, 0, 0, null);
        g2d.setClip(null);

        // 添加内发光效果
        g2d.setColor(new Color(255, 255, 255, 80));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(1, 1, SLIDER_WIDTH - 3, SLIDER_HEIGHT - 3, cornerRadius, cornerRadius);

        // 添加外阴影边框
        g2d.setColor(new Color(0, 0, 0, 60));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(0, 0, SLIDER_WIDTH - 1, SLIDER_HEIGHT - 1, cornerRadius, cornerRadius);

        // 添加中心图标（箭头）
        g2d.setColor(new Color(255, 255, 255, 220));
        int centerX = SLIDER_WIDTH / 2;
        int centerY = SLIDER_HEIGHT / 2;

        // 绘制左右箭头
        int arrowSize = 6;
        int arrowGap = 2;

        // 左箭头
        int[] leftX = {centerX - arrowGap, centerX - arrowGap - arrowSize, centerX - arrowGap};
        int[] leftY = {centerY - arrowSize/2, centerY, centerY + arrowSize/2};
        g2d.fillPolygon(leftX, leftY, 3);

        // 右箭头
        int[] rightX = {centerX + arrowGap, centerX + arrowGap + arrowSize, centerX + arrowGap};
        int[] rightY = {centerY - arrowSize/2, centerY, centerY + arrowSize/2};
        g2d.fillPolygon(rightX, rightY, 3);

        g2d.dispose();
        return sliderImage;
    }

    /**
     * 在背景图上添加现代化缺口效果
     */
    private BufferedImage addModernNotch(BufferedImage bgImage, int x, int y) {
        BufferedImage result = new BufferedImage(bgImage.getWidth(), bgImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();

        // 复制原图
        g2d.drawImage(bgImage, 0, 0, null);

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cornerRadius = 8;

        // 创建缺口区域（深色填充）
        g2d.setColor(new Color(200, 200, 200, 180));
        g2d.fillRoundRect(x, y, SLIDER_WIDTH, SLIDER_HEIGHT, cornerRadius, cornerRadius);

        // 添加缺口内阴影效果
        g2d.setColor(new Color(0, 0, 0, 40));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x + 1, y + 1, SLIDER_WIDTH - 3, SLIDER_HEIGHT - 3, cornerRadius, cornerRadius);

        // 添加缺口边框
        g2d.setColor(new Color(180, 180, 180, 200));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(x, y, SLIDER_WIDTH - 1, SLIDER_HEIGHT - 1, cornerRadius, cornerRadius);

        g2d.dispose();
        return result;
    }

    /**
     * 将图片转换为Base64字符串
     */
    private String imageToBase64(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    // ==================== 短信验证码 ====================

    @Override
    public SmsCaptchaResult sendSmsCaptcha(String phone) {
        return sendSmsCaptcha(phone, DEFAULT_DURATION);
    }

    @Override
    public SmsCaptchaResult sendSmsCaptcha(String phone, Duration duration) {
        if (StrUtil.isBlank(phone)) {
            return SmsCaptchaResult.builder()
                    .phone(phone)
                    .status("fail")
                    .message("手机号不能为空")
                    .build();
        }

        // 检查发送间隔
        String intervalKey = SMS_CAPTCHA_KEY_PREFIX + "interval:" + phone;
        String lastSendTime = cacheService.get(intervalKey);
        if (lastSendTime != null) {
            return SmsCaptchaResult.builder()
                    .phone(phone)
                    .status("fail")
                    .message("发送过于频繁，请稍后再试")
                    .interval(SMS_SEND_INTERVAL)
                    .build();
        }

        String code = generateSmsCode();
        String key = UUID.randomUUID().toString().replace("-", "");
        String cacheKey = SMS_CAPTCHA_KEY_PREFIX + phone;
        boolean developmentEcho = isDevelopmentEchoEnabled();

        try {
            cacheService.set(cacheKey, code, duration);
            boolean sendSuccess = developmentEcho || smsCaptchaSender
                    .map(sender -> sender.sendVerificationCode(phone, code, duration))
                    .orElse(false);
            if (!sendSuccess) {
                rollbackSmsCaptcha(cacheKey, phone);
                log.warn("短信验证码发送失败: phone={}", SensitiveDataUtil.maskPhone(phone));
                return smsFailure(phone);
            }

            cacheService.set(intervalKey, String.valueOf(System.currentTimeMillis()),
                    Duration.ofSeconds(SMS_SEND_INTERVAL));
            log.info("短信验证码发送成功: phone={}, mode={}", SensitiveDataUtil.maskPhone(phone),
                    developmentEcho ? "development" : "provider");
            return SmsCaptchaResult.builder()
                    .codeKey(key)
                    .phone(phone)
                    .status("success")
                    .message("验证码发送成功")
                    .code(developmentEcho ? code : null)
                    .expiresIn(duration.getSeconds())
                    .interval(SMS_SEND_INTERVAL)
                    .captchaType("sms")
                    .build();
        } catch (Exception e) {
            rollbackSmsCaptcha(cacheKey, phone);
            log.error("短信验证码发送异常: phone={}, errorType={}",
                    SensitiveDataUtil.maskPhone(phone), e.getClass().getSimpleName(), sanitizedException(e));
            return smsFailure(phone);
        }
    }

    @Override
    public boolean validateSmsCaptcha(String phone, String code) {
        if (StrUtil.isBlank(phone) || StrUtil.isBlank(code)) {
            return false;
        }

        String cacheKey = SMS_CAPTCHA_KEY_PREFIX + phone;
        String cacheCode = cacheService.get(cacheKey);

        if (cacheCode == null) {
            log.warn("短信验证码不存在或已过期: phone={}", SensitiveDataUtil.maskPhone(phone));
            return false;
        }

        boolean match = code.equals(cacheCode);
        log.debug("短信验证码校验: phone={}, result={}", SensitiveDataUtil.maskPhone(phone), match);
        return match;
    }

    @Override
    public boolean validateAndDeleteSmsCaptcha(String phone, String code) {
        boolean valid = validateSmsCaptcha(phone, code);
        if (valid) {
            String cacheKey = SMS_CAPTCHA_KEY_PREFIX + phone;
            cacheService.delete(cacheKey);
        }
        return valid;
    }

    // ==================== 邮箱验证码 ====================

    @Override
    public EmailCaptchaResult sendEmailCaptcha(String email) {
        return sendEmailCaptcha(email, DEFAULT_DURATION);
    }

    @Override
    public EmailCaptchaResult sendEmailCaptcha(String email, Duration duration) {
        if (StrUtil.isBlank(email)) {
            return EmailCaptchaResult.builder()
                    .email(email)
                    .status("fail")
                    .message("邮箱不能为空")
                    .build();
        }

        String intervalKey = EMAIL_CAPTCHA_KEY_PREFIX + "interval:" + email;
        String lastSendTime = cacheService.get(intervalKey);
        if (lastSendTime != null) {
            return EmailCaptchaResult.builder()
                    .email(email)
                    .status("fail")
                    .message("发送过于频繁，请稍后再试")
                    .interval(SMS_SEND_INTERVAL)
                    .build();
        }

        String code = generateSmsCode();
        String key = UUID.randomUUID().toString().replace("-", "");
        String cacheKey = EMAIL_CAPTCHA_KEY_PREFIX + email;
        boolean developmentEcho = isDevelopmentEchoEnabled();

        try {
            cacheService.set(cacheKey, code, duration);
            boolean sendSuccess = developmentEcho || emailCaptchaSender
                    .map(sender -> sender.sendVerificationCode(email, code, duration))
                    .orElse(false);
            if (!sendSuccess) {
                rollbackEmailCaptcha(cacheKey, email);
                log.warn("邮箱验证码发送失败: email={}", SensitiveDataUtil.maskEmail(email));
                return emailFailure(email);
            }

            cacheService.set(intervalKey, String.valueOf(System.currentTimeMillis()),
                    Duration.ofSeconds(SMS_SEND_INTERVAL));
            log.info("邮箱验证码发送成功: email={}, mode={}", SensitiveDataUtil.maskEmail(email),
                    developmentEcho ? "development" : "provider");
            return EmailCaptchaResult.builder()
                    .codeKey(key)
                    .email(email)
                    .status("success")
                    .message("验证码发送成功")
                    .code(developmentEcho ? code : null)
                    .expiresIn(duration.getSeconds())
                    .interval(SMS_SEND_INTERVAL)
                    .captchaType("email")
                    .build();
        } catch (Exception e) {
            rollbackEmailCaptcha(cacheKey, email);
            log.error("邮箱验证码发送异常: email={}, errorType={}",
                    SensitiveDataUtil.maskEmail(email), e.getClass().getSimpleName(), sanitizedException(e));
            return emailFailure(email);
        }
    }

    @Override
    public boolean validateEmailCaptcha(String email, String code) {
        if (StrUtil.isBlank(email) || StrUtil.isBlank(code)) {
            return false;
        }

        String cacheKey = EMAIL_CAPTCHA_KEY_PREFIX + email;
        String cacheCode = cacheService.get(cacheKey);

        if (cacheCode == null) {
            log.warn("邮箱验证码不存在或已过期: email={}", SensitiveDataUtil.maskEmail(email));
            return false;
        }

        boolean match = code.equals(cacheCode);
        log.debug("邮箱验证码校验: email={}, result={}", SensitiveDataUtil.maskEmail(email), match);
        return match;
    }

    @Override
    public boolean validateAndDeleteEmailCaptcha(String email, String code) {
        boolean valid = validateEmailCaptcha(email, code);
        if (valid) {
            String cacheKey = EMAIL_CAPTCHA_KEY_PREFIX + email;
            cacheService.delete(cacheKey);
        }
        return valid;
    }

    private boolean isDevelopmentEchoEnabled() {
        return captchaProperties.isDevEchoCode()
                && environment.acceptsProfiles(Profiles.of("dev", "local"));
    }

    private String generateSmsCode() {
        StringBuilder code = new StringBuilder(SMS_CODE_LENGTH);
        for (int i = 0; i < SMS_CODE_LENGTH; i++) {
            code.append(RandomUtil.randomInt(10));
        }
        return code.toString();
    }

    private void rollbackSmsCaptcha(String cacheKey, String phone) {
        try {
            cacheService.delete(cacheKey);
        } catch (RuntimeException rollbackException) {
            log.error("短信验证码缓存回滚失败: phone={}, errorType={}",
                    SensitiveDataUtil.maskPhone(phone),
                    rollbackException.getClass().getSimpleName(), sanitizedException(rollbackException));
        }
    }

    private void rollbackEmailCaptcha(String cacheKey, String email) {
        try {
            cacheService.delete(cacheKey);
        } catch (RuntimeException rollbackException) {
            log.error("邮箱验证码缓存回滚失败: email={}, errorType={}",
                    SensitiveDataUtil.maskEmail(email),
                    rollbackException.getClass().getSimpleName(), sanitizedException(rollbackException));
        }
    }

    private RuntimeException sanitizedException(Exception exception) {
        RuntimeException sanitized = new RuntimeException("CAPTCHA_OPERATION_FAILED");
        sanitized.setStackTrace(exception.getStackTrace());
        return sanitized;
    }

    private SmsCaptchaResult smsFailure(String phone) {
        return SmsCaptchaResult.builder()
                .phone(phone)
                .status("fail")
                .message("验证码发送失败，请稍后重试")
                .build();
    }

    private EmailCaptchaResult emailFailure(String email) {
        return EmailCaptchaResult.builder()
                .email(email)
                .status("fail")
                .message("验证码发送失败，请稍后重试")
                .build();
    }
}
