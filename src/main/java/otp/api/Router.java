package otp.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.model.UserRole;

import java.io.IOException;

/**
 * Dispatcher отвечает за регистрацию HTTP-контекстов (маршрутов) и их привязку к методам контроллеров.
 * <p>
 * Список маршрутов:
 * <ul>
 *   <li>POST   /signup             → AuthController.handleRegister()  (публичный)</li>
 *   <li>POST   /signin             → AuthController.handleLogin()     (публичный)</li>
 *   <li>POST   /otp/generate       → UserController.generateOtp()     (роль USER)</li>
 *   <li>POST   /otp/validate       → UserController.validateOtp()     (роль USER)</li>
 *   <li>PATCH  /admin/config       → AdminController.updateOtpConfig() (роль ADMIN)</li>
 *   <li>GET    /admin/users        → AdminController.listUsers()       (роль ADMIN)</li>
 *   <li>DELETE /admin/users/{id}   → AdminController.deleteUser()      (роль ADMIN)</li>
 * </ul>
 * </p>
 */
public class Router {
    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    private final AuthController authController = new AuthController();
    private final UserController userController = new UserController();
    private final AdminController adminController = new AdminController();

    /**
     * Регистрация всех маршрутов и подключение фильтров аутентификации.
     *
     * @param server экземпляр HttpServer
     */
    public void registerRoutes(@NotNull HttpServer server) {

        // Маршруты без ограничений
        server.createContext("/signup", new RequestLogger(authController::handleSignUp)::handle);
        server.createContext("/signin",    new RequestLogger(authController::handleSignIn)::handle);

        // Маршруты для пользователей (Auth фильтр с проверкой роли USER):
        // Создание OTP кода
        HttpContext newCtx = server.createContext("/otp/new",  new RequestLogger(userController::newOtp)::handle);
        // Добавляем проверку JWT
        newCtx.getFilters().add(new AuthFilter(UserRole.USER));

        // Проверка OTP кода
        HttpContext checkCtx = server.createContext("/otp/check",  new RequestLogger(userController::checkOtp)::handle);
        // Добавляем проверку JWT
        checkCtx.getFilters().add(new AuthFilter(UserRole.USER));

        // Маршруты для администраторов (Auth фильтр с проверкой роли ADMIN):
        // Конфигурирование OTP
        HttpContext configCtx = server.createContext("/admin/config",  new RequestLogger(adminController::updateOtpConfig)::handle);
        // Добавляем проверку JWT
        configCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));

        // Операции с пользователями (создание/кдаление) в зависимости от метода
        HttpContext usersCtx = server.createContext("/admin/users", new RequestLogger(exchange -> {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("GET")) {
                adminController.listUsers(exchange);
            } else if (method.equalsIgnoreCase("DELETE")) {
                adminController.deleteUser(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        })::handle);
        // Добавляем проверку JWT
        usersCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));
    }


    private static final class RequestLogger {
        HttpHandler handler;

        public RequestLogger(@NotNull HttpHandler handler) {
            this.handler = handler;
        }

        public void handle(@NotNull HttpExchange exchange) throws IOException  {
            logger.info("Register request [{}] with path {}", exchange.getRequestMethod(), exchange.getRequestURI());
            handler.handle(exchange);
        }
    }
}
