CREATE DATABASE IF NOT EXISTS flashscale_inventory;
CREATE DATABASE IF NOT EXISTS flashscale_orders;

GRANT ALL PRIVILEGES ON flashscale_inventory.* TO 'flashscale_user'@'%';
GRANT ALL PRIVILEGES ON flashscale_orders.* TO 'flashscale_user'@'%';
FLUSH PRIVILEGES;
