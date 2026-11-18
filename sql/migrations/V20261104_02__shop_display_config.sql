CREATE TABLE IF NOT EXISTS shop_display_config (
 id tinyint NOT NULL PRIMARY KEY,
 display_name varchar(30) NOT NULL,
 logo_url varchar(1024) NOT NULL DEFAULT '',
 introduction varchar(200) NOT NULL DEFAULT '',
 customer_service varchar(100) NOT NULL DEFAULT '',
 update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO shop_display_config(id, display_name) VALUES(1, '陕铺');
