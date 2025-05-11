package otp.api;

import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.dao.impl.UserDaoImpl;
import otp.model.UserRole;
import otp.service.UserService;
import otp.util.HttpUtils;
import otp.util.JsonUtil;

import java.io.IOException;
import java.util.Map;

/**
 * Контроллер аутентификации и регистрации пользователей.
 * Обрабатывает публичные запросы:
 * <ul>
 *   <li>POST /signup — регистрация нового пользователя (username, password, role)</li>
 *   <li>POST /signin — аутентификация и выдача токена (username, password)</li>
 * </ul>
 */
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService = new UserService(new UserDaoImpl());

    /**
     * Обрабатывает HTTP POST запрос на регистрацию пользователя.
     * Проверяет метод, Content-Type и формат JSON, затем вызывает UserService.register().
     * Возвращает:
     * <ul>
     *   <li>201 Created — при успешной регистрации</li>
     *   <li>405 Method Not Allowed — если метод не POST</li>
     *   <li>409 Conflict — если имя занято или администратор уже существует</li>
     *   <li>415 Unsupported Media Type — если Content-Type некорректен</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange объект HttpExchange для текущего запроса
     * @throws IOException при ошибках чтения/записи
     */
    public void handleSignUp(HttpExchange exchange) throws IOException {
        // Проверяем структуру запроса
        if (!validateRequest(exchange)) {
            return;
        }

        try {
            Dto.SignUpRequest req = JsonUtil.fromJson(exchange.getRequestBody(), Dto.SignUpRequest.class);
            logger.info("SignUp user [{}] with role {}", req.username, req.role);

            // Проверка, не существует ли уже администратор
            if (req.role.equalsIgnoreCase("ADMIN") && userService.adminExists()) {
                HttpUtils.sendError(exchange, 409, "Admin already exists");
                return;
            }

            userService.signUp(req.username, req.password, UserRole.valueOf(req.role));
            HttpUtils.sendResponseCode(exchange, 201);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.error("SignUp failed", e);
            HttpUtils.sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            logger.error("SignUp failed", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP POST запрос на аутентификацию пользователя.
     * Проверяет метод, Content-Type и формат JSON, затем вызывает UserService.login().
     * Возвращает:
     * <ul>
     *   <li>200 OK — возвращает JSON {"token":"..."}</li>
     *   <li>401 Unauthorized — если логин или пароль неверны</li>
     *   <li>405 Method Not Allowed — если метод не POST</li>
     *   <li>415 Unsupported Media Type — если Content-Type некорректен</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange объект HttpExchange для текущего запроса
     * @throws IOException при ошибках чтения/записи
     */
    public void handleSignIn(HttpExchange exchange) throws IOException {
        // Проверяем структуру запроса
        if (!validateRequest(exchange)) {
            return;
        }

        try {
            Dto.LoginRequest req = JsonUtil.fromJson(exchange.getRequestBody(), Dto.LoginRequest.class);
            logger.info("SignIn user [{}]", req.username);
            String token = userService.login(req.username, req.password);
            if (token == null) {
                logger.warn("SignIn user [{}]: no token", req.username);
                HttpUtils.sendError(exchange, 401, "Unauthorized");
                return;
            }
            String json = JsonUtil.toJson(Map.of("token", token)); // Используем сгенерированный токен
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (IllegalArgumentException e) {
            logger.error("SignIn failed", e);
            HttpUtils.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            logger.error("SignIn failed", e);
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
