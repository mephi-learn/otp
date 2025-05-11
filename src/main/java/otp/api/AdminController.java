package otp.api;

import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.OtpDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.model.User;
import otp.service.AdminService;
import otp.util.HttpUtils;
import otp.util.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Контроллер для административных операций (роль ADMIN).
 * <p>
 * Доступные маршруты:
 * <ul>
 *   <li>PATCH  /admin/config     — изменить длину и время жизни OTP-кодов</li>
 *   <li>GET    /admin/users      — получить список всех пользователей без админов</li>
 *   <li>DELETE /admin/users/{id} — удалить пользователя и связанные OTP-коды</li>
 * </ul>
 * </p>
 */
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService = new AdminService(
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new OtpDaoImpl()
    );

    /**
     * Обрабатывает HTTP PATCH запрос на изменение конфигурации OTP.
     * <p>
     * Ожидает JSON: {"length": 6, "ttlSeconds": 300}
     * </p>
     * <ul>
     *   <li>204 No Content — успешно обновлено</li>
     *   <li>415 Unsupported Media Type — если Content-Type не application/json</li>
     *   <li>405 Method Not Allowed — если метод не PATCH</li>
     *   <li>500 Internal Server Error — другие ошибки</li>
     * </ul>
     *
     * @param exchange HTTP-контекст текущего запроса
     * @throws IOException при ошибках ввода-вывода
     */
    public void updateOtpConfig(HttpExchange exchange) throws IOException {

        // Тип запроса дожен быть PATCH
        if (!exchange.getRequestMethod().equalsIgnoreCase("PATCH")) {
            logger.error("Invalid method [{}], PATCH needed", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        // Ожидаем увидеть в запросе JSON
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            logger.error("Invalid Content-Type [{}]", contentType);
            HttpUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        try {
            // Парсим запрос и если всё прошло хорошо, обновляем конфигурацию OTP
            Dto.ConfigRequest req = JsonUtil.fromJson(exchange.getRequestBody(), Dto.ConfigRequest.class);
            logger.info("Update Configuration");
            adminService.updateOtpConfig(req.length, req.ttlSeconds);
            HttpUtils.sendResponseCode(exchange, 204);
        } catch (Exception e) {
            // В случае ошибки пятисотим
            logger.error("Update Configuration error", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP GET запрос для получения списка пользователей без прав администратора.
     * <ul>
     *   <li>200 OK — возвращает JSON-массив пользователей</li>
     *   <li>405 Method Not Allowed — если метод не GET</li>
     *   <li>500 Internal Server Error — другие ошибки</li>
     * </ul>
     *
     * @param exchange HTTP-контекст текущего запроса
     * @throws IOException при ошибках ввода-вывода
     */
    public void listUsers(@NotNull HttpExchange exchange) throws IOException {

        // Тип запроса должен быть GET
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            logger.error("Invalid method [{}], GET needed", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            // Получаем список обычных пользователей, формируем из них JSON и отдаём
            logger.info("List users");
            List<User> users = adminService.getAllUsersWithoutAdmins();
            String json = JsonUtil.toJson(users);
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (Exception e) {
            // В случае ошибки пятисотим
            logger.error("List user error", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Обрабатывает HTTP DELETE запрос на удаление пользователя по ID.
     * <ul>
     *   <li>204 No Content — успешно удалено</li>
     *   <li>400 Bad Request — если ID некорректен</li>
     *   <li>404 Not Found — если пользователь не найден</li>
     *   <li>405 Method Not Allowed — если метод не DELETE</li>
     *   <li>500 Internal Server Error — другие ошибки</li>
     * </ul>
     *
     * @param exchange HTTP-контекст текущего запроса
     * @throws IOException при ошибках ввода-вывода
     */
    public void deleteUser(@NotNull HttpExchange exchange) throws IOException {

        // Тип запроса должен быть DELETE
        if (!exchange.getRequestMethod().equalsIgnoreCase("DELETE")) {
            logger.error("Invalid method [{}], DELETE needed", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        try {

            // Получаем идентификатор пользователя (он должен идти последним параметром после /) и удаляем пользователя
            URI uri = exchange.getRequestURI();
            String[] segments = uri.getPath().split("/");
            Long id = Long.valueOf(segments[segments.length - 1]);

            logger.info("Delete user by ID: {}", id);
            adminService.deleteUserAndCodes(id);
            HttpUtils.sendResponseCode(exchange, 204);
        } catch (NumberFormatException e) {
            logger.error("Delete user error: invalid ID", e);
            HttpUtils.sendError(exchange, 400, "Invalid user ID");
        } catch (IllegalArgumentException e) {
            logger.error("Delete user error", e);
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            logger.error("Delete user error", e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }
}
