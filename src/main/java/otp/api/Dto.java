package otp.api;

public class Dto {
    /**
     * DTO для разбора JSON тела PATCH запроса /admin/config.
     */
    static class ConfigRequest {
        public int length;
        public int ttlSeconds;
    }

    /**
     * DTO для разбора JSON тела запроса регистрации.
     */
    static class SignUpRequest {
        public String username;
        public String password;
        public String role;
    }

    /**
     * DTO для разбора JSON тела запроса логина.
     */
    static class LoginRequest {
        public String username;
        public String password;
    }

    /**
     * DTO для разбора JSON тела POST /otp/generate.
     */
    static class GenerateRequest {
        public Long userId;
        public String operationId;
        public String channel;
    }

    /**
     * DTO для разбора JSON тела POST /otp/validate.
     */
    static class ValidateRequest {
        public String code;
    }

}
