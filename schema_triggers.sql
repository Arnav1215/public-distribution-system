USE PDS_DB;

DROP TRIGGER IF EXISTS trg_check_stock_before_distribution;
DROP TRIGGER IF EXISTS trg_deduct_stock_after_distribution;

DELIMITER $$

CREATE TRIGGER trg_check_stock_before_distribution
BEFORE INSERT ON DISTRIBUTION_TRANSACTION
FOR EACH ROW
BEGIN
    DECLARE v_available INT DEFAULT 0;
    DECLARE v_shopId    INT DEFAULT 0;
    SELECT ShopID INTO v_shopId
    FROM REQUEST
    WHERE RequestID = NEW.RequestID
    LIMIT 1;
    SELECT COALESCE(QuantityAvailable, 0) INTO v_available
    FROM STOCK
    WHERE ShopID = v_shopId AND CommodityID = NEW.CommodityID
    LIMIT 1;
    IF v_available < NEW.QuantityIssued THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Insufficient stock: cannot issue more than available quantity.';
    END IF;
END$$

CREATE TRIGGER trg_deduct_stock_after_distribution
AFTER INSERT ON DISTRIBUTION_TRANSACTION
FOR EACH ROW
BEGIN
    DECLARE v_shopId INT DEFAULT 0;
    SELECT ShopID INTO v_shopId
    FROM REQUEST
    WHERE RequestID = NEW.RequestID
    LIMIT 1;
    UPDATE STOCK
    SET QuantityAvailable = QuantityAvailable - NEW.QuantityIssued
    WHERE ShopID = v_shopId AND CommodityID = NEW.CommodityID;
    INSERT INTO AUDIT_LOG (EntityName, EntityID, OperationType, PerformedBy)
    VALUES ('DISTRIBUTION_TRANSACTION', NEW.TransactionID, 'STOCK_DEDUCTED', 'SYSTEM_TRIGGER');
END$$

DELIMITER ;
