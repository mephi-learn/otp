package otp.service;

import otp.dao.OtpDao;
import otp.dao.OtpConfigDao;
import otp.dao.UserDao;
import otp.model.Otp;
import otp.model.OtpConfig;
import otp.model.OtpStatus;
import otp.model.User;
import otp.service.notification.NotificationChannel;
import otp.service.notification.NotificationService;
import otp.service.notification.NotificationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom random = new SecureRandom();

    private final OtpDao otpDao;
    private final OtpConfigDao otpConfigDao;
    private final UserDao userDao;
    private final NotificationServiceFactory notificationFactory;

    public OtpService(OtpDao otpDao,
                      OtpConfigDao otpConfigDao,
                      UserDao userDao,
                      NotificationServiceFactory notificationFactory) {
        this.otpDao = otpDao;
        this.otpConfigDao = otpConfigDao;
        this.userDao = userDao;
        this.notificationFactory = notificationFactory;
    }

    /**
     * Генерирует новый OTP-код, сохраняет его в БД и возвращает строку.
     */
    public String generateOtp(Long userId, String operationId) {
        OtpConfig config = otpConfigDao.getConfig();
        int length = config.getLength();

        // Генерация случайного цифрового кода нужной длины
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        String code = sb.toString();

        // Сохраняем в БД
        Otp otp = new Otp(
                null,
                userId,
                operationId,
                code,
                OtpStatus.ACTIVE,
                LocalDateTime.now()
        );
        otpDao.save(otp);
        logger.info("Generated OTP {} for userId={}, operationId={}", code, userId, operationId);
        return code;
    }

    /**
     * Возвращает текущую конфигурацию длины и TTL для кодов.
     */
    public OtpConfig getConfig() {
        return otpConfigDao.getConfig();
    }

    /**
     * Сгенерировать и отправить код указанным каналом.
     */
    public void sendOtpToUser(Long userId, String operationId, NotificationChannel channel) {
        String code = generateOtp(userId, operationId);
        User user = userDao.getById(userId);
        if (user == null) {
            logger.error("sendOtpToUser: user not found, id={}", userId);
            throw new IllegalArgumentException("User not found");
        }

        // Для простоты используем username как адресат (email, sms, chatId)
        String recipient = user.getUsername();
        NotificationService svc = notificationFactory.getService(channel);
        svc.sendCode(recipient, code);
        logger.info("Sent OTP code for userId={} via {}", userId, channel);
    }

    /**
     * Проверяет введённый код: активность и срок жизни, и переключает статус на USED.
     */
    public boolean validateOtp(String inputCode) {
        Otp otp = otpDao.getByCode(inputCode);
        if (otp == null) {
            logger.warn("validateOtp: code not found {}", inputCode);
            return false;
        }
        // Проверка статуса
        if (otp.getStatus() != OtpStatus.ACTIVE) {
            logger.warn("validateOtp: code {} is not active (status={})", inputCode, otp.getStatus());
            return false;
        }
        // Проверка истечения по времени
        OtpConfig config = otpConfigDao.getConfig();
        LocalDateTime expiry = otp.getCreatedAt().plusSeconds(config.getTtlSeconds());
        if (LocalDateTime.now().isAfter(expiry)) {
            otpDao.markAsExpired(Duration.ofSeconds(config.getTtlSeconds()));
            logger.warn("validateOtp: code {} expired at {}", inputCode, expiry);
            return false;
        }

        // Всё ок — помечаем как USED
        otpDao.markAsUsed(otp.getId());
        logger.info("validateOtp: code {} validated and marked USED", inputCode);
        return true;
    }

    /**
     * Меняет статус всех просроченных кодов на EXPIRED.
     */
    public void markExpiredOtps() {
        OtpConfig config = otpConfigDao.getConfig();
        Duration ttl = Duration.ofSeconds(config.getTtlSeconds());
        otpDao.markAsExpired(ttl);
        logger.info("markExpiredOtps: expired codes older than {} seconds", config.getTtlSeconds());
    }
}

