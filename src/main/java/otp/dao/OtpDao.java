package otp.dao;

import otp.model.Otp;
import java.time.Duration;
import java.util.List;

/**
 * Интерфейс для доступа к данным одноразовых кодов (OTP).
 */
public interface OtpDao {

    /**
     * Ищет запись по самому значению кода.
     * @param code строка кода
     * @return объект Otp или null, если не найден
     */
    Otp getByCode(String code);

    /**
     * Сохраняет новый одноразовый код в БД.
     * @param code объект Otp (id и createdAt могут быть null — будут заполнены БД)
     */
    void save(Otp code);

    /**
     * Удаляет все коды, принадлежащие указанному пользователю.
     * @param userId идентификатор пользователя
     */
    void deleteByUserId(Long userId);

    /**
     * Возвращает все коды, связанные с указанным пользователем.
     * @param userId идентификатор пользователя
     * @return список всех Otp для данного пользователя
     */
    List<Otp> getByUserId(Long userId);

    /**
     * Помечает код с заданным id как использованный.
     * @param id идентификатор записи Otp
     */
    void markAsUsed(Long id);

    /**
     * Помечает все коды старше указанного TTL как просроченные.
     * @param ttl время жизни кода (Duration), все коды с createdAt + ttl &lt; now() станут EXPIRED
     */
    void markAsExpired(Duration ttl);
}

