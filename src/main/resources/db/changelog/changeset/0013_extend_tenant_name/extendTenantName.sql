-- Change length of 'name' column to VARCHAR(100)
ALTER TABLE `tenantservice`.`tenant`
MODIFY COLUMN `name` VARCHAR(100);