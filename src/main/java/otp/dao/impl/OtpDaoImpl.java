package otp.dao.impl;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import otp.config.DatabaseManager;
import otp.dao.OtpDao;
import otp.model.Otp;
import otp.model.OtpStatus;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-реализация OtpDao.
 * Управляет записями OTP-кодов в таблице otp_codes.
 */
public class OtpDaoImpl implements OtpDao {
    private static final Logger logger = LoggerFactory.getLogger(OtpDaoImpl.class);

    private static final String INSERT_SQL =
            "INSERT INTO otp_codes (user_id, operation_id, code, status, created_at) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT_BY_CODE_SQL =
            "SELECT id, user_id, operation_id, code, status, created_at FROM otp_codes WHERE code = ?";
    private static final String SELECT_BY_USER_SQL =
            "SELECT id, user_id, operation_id, code, status, created_at FROM otp_codes WHERE user_id = ?";
    private static final String UPDATE_MARK_USED_SQL =
            "UPDATE otp_codes SET status = 'USED' WHERE id = ?";
    private static final String UPDATE_MARK_EXPIRED_SQL =
            "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND created_at < ?";
    private static final String DELETE_BY_USER_SQL =
            "DELETE FROM otp_codes WHERE user_id = ?";

    @Override
    public Otp getByCode(String code) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CODE_SQL)) {

            // Указываем код
            ps.setString(1, code);

            // Выполняем запрос
            try (ResultSet rs = ps.executeQuery()) {

                // Если код получен
                if (rs.next()) {
                    Otp found = mapRow(rs);
                    logger.info("Found OTP by code {}: {}", code, found);
                    return found;
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding OTP by code [{}]: {}", code, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public List<Otp> getByUserId(Long userId) {
        List<Otp> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER_SQL)) {

            // Указываем идентификатор пользователя
            ps.setLong(1, userId);

            // Выполняем запрос
            try (ResultSet rs = ps.executeQuery()) {

                // Добавляем все найденные коды в коллекцию
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            logger.info("Found {} OTP codes for user {}", list.size(), userId);
        } catch (SQLException e) {
            logger.error("Error finding OTP codes for user [{}]: {}", userId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public void save(@NotNull Otp code) {
        // Устанавливаем время создания, если оно не задано
        if (code.getCreatedAt() == null) {
            code.setCreatedAt(LocalDateTime.now());
        }
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            // Указываем идентификатор пользователя
            ps.setLong(1, code.getUserId());

            // Если код привязан к операции, то указываем её
            if (code.getOperationId() != null) {
                ps.setString(2, code.getOperationId());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }

            // Указываем код
            ps.setString(3, code.getCode());

            // Указываем название статуса
            ps.setString(4, code.getStatus().name());

            // Указываем дату создания
            ps.setTimestamp(5, Timestamp.valueOf(code.getCreatedAt()));

            // Выполняем запрос
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Saving OTP code failed, no rows affected.");
            }

            // Получаем идентификатор внесённой записи и обогащаем ей код
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    code.setId(keys.getLong(1));
                }
            }
            logger.info("Saved OTP code: {}", code);
        } catch (SQLException e) {
            logger.error("Error saving OTP code [{}]: {}", code.getCode(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteByUserId(Long userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_USER_SQL)) {

            // Указываем идентификатор пользователя
            ps.setLong(1, userId);

            // Выполняем запрос
            int affected = ps.executeUpdate();
            logger.info("Deleted {} OTP codes for user {}", affected, userId);
        } catch (SQLException e) {
            logger.error("Error deleting OTP codes for user [{}]: {}", userId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markAsUsed(Long id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_MARK_USED_SQL)) {

            // Указываем идентификатор записи
            ps.setLong(1, id);

            // Выполняем запрос
            int affected = ps.executeUpdate();
            logger.info("Marked OTP id {} as USED ({} rows affected)", id, affected);
        } catch (SQLException e) {
            logger.error("Error marking OTP id [{}] as USED: {}", id, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markAsExpired(Duration ttl) {
        LocalDateTime threshold = LocalDateTime.now().minus(ttl);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_MARK_EXPIRED_SQL)) {

            // Указываем время протухания кода
            ps.setTimestamp(1, Timestamp.valueOf(threshold));

            // Выполняем запрос
            int affected = ps.executeUpdate();
            logger.info("Marked {} OTP codes as EXPIRED older than {}", affected, threshold);
        } catch (SQLException e) {
            logger.error("Error marking expired OTP codes older than {}: {}", threshold, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Преобразует текущую строку ResultSet в объект Otp.
     */
    private Otp mapRow(ResultSet rs) throws SQLException {
        Otp otp = new Otp();
        otp.setId(rs.getLong("id"));
        otp.setUserId(rs.getLong("user_id"));
        String op = rs.getString("operation_id");
        otp.setOperationId(op != null ? op : null);
        otp.setCode(rs.getString("code"));
        otp.setStatus(OtpStatus.valueOf(rs.getString("status")));
        Timestamp ts = rs.getTimestamp("created_at");
        otp.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return otp;
    }
}

