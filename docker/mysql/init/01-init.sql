-- Ensure database exists with proper charset
CREATE DATABASE IF NOT EXISTS g2u_admin
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Grant privileges
GRANT ALL PRIVILEGES ON g2u_admin.* TO 'g2u'@'%';
FLUSH PRIVILEGES;
