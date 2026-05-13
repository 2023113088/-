-- 首次启动 MySQL 容器时执行；创建三个业务库并授权给应用账号（非 root 跑业务）
CREATE DATABASE IF NOT EXISTS userdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bookdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS borrowdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'library'@'%' IDENTIFIED BY 'library_secret_change_me';
GRANT ALL PRIVILEGES ON userdb.* TO 'library'@'%';
GRANT ALL PRIVILEGES ON bookdb.* TO 'library'@'%';
GRANT ALL PRIVILEGES ON borrowdb.* TO 'library'@'%';
FLUSH PRIVILEGES;
