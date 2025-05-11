package otp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Сущность одноразового кода (OTP).
 * Содержит информацию о сгенерированном коде, его статусе и времени создания.
 */
public class Otp {
    private Long id;
    private Long userId;
    private String operationId;   // идентификатор операции, к которой привязан код (может быть null)
    private String code;          // сам OTP
    private OtpStatus status;     // статус кода: ACTIVE, EXPIRED, USED
    private LocalDateTime createdAt;

    public Otp() {
    }

    public Otp(Long id,
               Long userId,
               String operationId,
               String code,
               OtpStatus status,
               LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OtpStatus getStatus() {
        return status;
    }

    public void setStatus(OtpStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Otp otp = (Otp) o;
        return Objects.equals(id, otp.id)
                && Objects.equals(userId, otp.userId)
                && Objects.equals(operationId, otp.operationId)
                && Objects.equals(code, otp.code)
                && status == otp.status
                && Objects.equals(createdAt, otp.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, operationId, code, status, createdAt);
    }

    @Override
    public String toString() {
        return "Otp{" +
                "id=" + id +
                ", userId=" + userId +
                ", operationId='" + operationId + '\'' +
                ", code='" + code + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}

