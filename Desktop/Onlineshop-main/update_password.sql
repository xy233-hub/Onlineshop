USE onlineshop;
UPDATE seller SET password = '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW' WHERE username = 'seller1';
SELECT * FROM seller;