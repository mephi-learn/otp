# OTP

Backend-сервис на Java для защиты действий пользователей с помощью генерации и проверки One Time Password (OTP) с отправкой через Email, SMS (SMPP эмулятор), Telegram и сохранением в файл.

---

## Функционал

- **Регистрация и аутентификация пользователей** с ролями: `ADMIN` (только один пользователь) и `USER` (неограниченно)
- **Генерация и отправка OTP-кодов** (email, sms, telegram, файл):
- **Проверка OTP-кодов** с учетом статусов: `ACTIVE`, `USED`, `EXPIRED`
- **Администрирование** (настройка TTL и длины OTP, управление пользователями)
- **JWT аутентификация** с проверкой ролей
- **Логирование** всех ключевых операций через SLF4J/Logback

---

## Установка и запуск

### 1. Настройка

Клонируйте репозиторий:

```bash
git clone https://github.com/mephi-learn/otp.git
cd otp
```

Заполните конфигурационные файлы в `src/main/resources`:

- `application.properties` (параметры БД)
- `email.properties` (SMTP сервер)
- `sms.properties` (SMPP эмулятор)
- `telegram.properties` (токен и chatId)

Пример `application.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/otp
db.user=user
db.password=password
```

### 2. Подготовка

Создайте базу данных `otp`:

```sql
CREATE DATABASE otp;
```

Создайте таблицы скриптом: `resources/db/schema.sql`

Наполните таблицы данными: `resources/db/data.sql`

### 3. Сборка и запуск

Соберите проект и запустите приложение:

```bash
mvn clean package
java -jar target/otp.jar
```

---

## Роли и авторизация

- **ADMIN**: полные права управления
  - настройка OTP
  - просмотр и удаление пользователей
- **USER**: ограниченные права
  - генерация и валидация OTP

### Токены

- Генерируются при логине, имеют ограниченный TTL
- Передаются в заголовке:

```http
Authorization: Bearer <token>
```

---

## Примеры API-запросов

### Регистрация администратора (только один раз)

```bash
curl -X POST http://localhost:8000/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"PaSsWoRdAdMiN","role":"ADMIN"}'
```

### Регистрация пользователя

```bash
curl -X POST http://localhost:8000/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"PaSsWoRd","role":"USER"}'
```

### Вход (получение токена)

```bash
curl -X POST http://localhost:8000/signin \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"PaSsWoRd"}'
```

### Генерация OTP

```bash
curl -X POST http://localhost:8000/otp/new \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -d '{"operationId":"op123","channel":"EMAIL"}'
```

### Проверка OTP

```bash
curl -X POST http://localhost:8000/otp/check \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer JWT_TOKEN" \
  -d '{"code":"123456"}'
```

### Действия администратора

```bash
# Изменение параметров OTP
curl -X PATCH http://localhost:8000/admin/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer JWT_ADMIN_TOKEN" \
  -d '{"length":6,"ttlSeconds":300}'

# Просмотр пользователей
curl -X GET http://localhost:8000/admin/users \
  -H "Authorization: Bearer JWT_ADMIN_TOKEN"

# Удаление пользователя
curl -X DELETE http://localhost:8000/admin/users/2 \
  -H "Authorization: Bearer JWT_ADMIN_TOKEN"
```

---

## Тестирование

Используйте **Insomnia** или **curl** для проверки API. Убедитесь, что работают:

- регистрация и аутентификация
- генерация и отправка OTP
- проверка OTP-кодов
- администраторские функции
