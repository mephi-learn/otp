package otp.api;

import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.dao.impl.OtpDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.service.OtpService;
import otp.service.notification.NotificationChannel;
import otp.service.notification.NotificationServiceFactory;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;

/**
 * Контроллер пользовательских операций для работы с OTP-кодами (роль USER).
 * <p>
 * Доступные маршруты:
 * <ul>
 *   <li>POST /otp/new   — создаёт и отправляет OTP-код</li>
 *   <li>POST /otp/check — проверяет корректность и статус OTP-кода</li>
 * </ul>
 * </p>
 */
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final OtpService otpService = new OtpService(
            new OtpDaoImpl(),
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new NotificationServiceFactory()
    );

    /**
     * Обрабатывает HTTP POST запрос генерации OTP-кода.
     * <p>
     * Ожидает JSON: {"userId": 123, "operationId": "op123", "channel": "EMAIL"}.
     * </p>
     * <ul>
     *   <li>202 Accepted — запрос принят и код отправлен</li>
     *   <li>400 Bad Request — неверные данные или канал</li>
     *   <li>405 Method Not Allowed — метод не POST</li>
     *   <li>415 Unsupported Media Type — Content-Type не application/json</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange текущий HTTP-контекст
     * @throws IOException при ошибках ввода-вывода
     */
    public void newOtp(HttpExchange exchange) throws IOException {
        // Проверяем структуру запроса
        if (!validateRequest(exchange)) {
            return;
        }

        try {
            Dto.GenerateRequest req = JsonUtil.fromJson(exchange.getRequestBody(), Dto.GenerateRequest.class);
            logger.info("Create new OTP for user ID: {}", req.userId);
            otpService.sendOtpToUser(req.userId, req.operationId, NotificationChannel.valueOf(req.channel));
            HttpUtils.sendResponseCode(exchange, 202);
        } catch (IllegalArgumentException e) {
            logger.error("Create new OTP failed", e);
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Create new OTP failed", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP POST запрос валидации OTP-кода.
     * <p>
     * Ожидает JSON: {"code": "123456"}.
     * </p>
     * <ul>
     *   <li>200 OK — код корректен</li>
     *   <li>400 Bad Request — неверный или просроченный код</li>
     *   <li>415 Unsupported Media Type — Content-Type не application/json</li>
     *   <li>405 Method Not Allowed — метод не POST</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange текущий HTTP-контекст
     * @throws IOException при ошибках ввода-вывода
     */
    public void checkOtp(HttpExchange exchange) throws IOException {
        // Проверяем структуру запроса
        if (!validateRequest(exchange)) {
            return;
        }

        try {
            Dto.ValidateRequest req = JsonUtil.fromJson(exchange.getRequestBody(), Dto.ValidateRequest.class);
            logger.info("Check OTP: {}", req.code);
            boolean valid = otpService.validateOtp(req.code);
            if (valid) {
                HttpUtils.sendResponseCode(exchange, 200);
            } else {
                logger.error("Invalid or expired OTP: {}", req.code);
                HttpUtils.sendError(exchange, 400, "Invalid or expired code");
            }
        } catch (IllegalArgumentException e) {
            logger.error("Check OTP failed", e);
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Check OTP failed", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Проверяет метод, Content-Type и формат JSON в HTTP запросе.
     * Возвращает:
     * <ul>
     *   <li>True — запрос сформирован корректно</li>
     *   <li>False — запрос сформирован ошибочно</li>
     * </ul>
     *
     * @param exchange объект HttpExchange для текущего запроса
     * @throws IOException при ошибках чтения/записи
     */
    private boolean validateRequest(@NotNull HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            logger.error("Invalid method [{}], POST needed", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return false;
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            logger.error("Invalid Content-Type [{}]", contentType);
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return false;
        }
        return true;
    }
}
