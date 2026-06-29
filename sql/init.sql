-- ============================================
-- XX Marketing Platform 数据库初始化脚本
-- 创建数据库: xx_marketing
-- ============================================

CREATE DATABASE IF NOT EXISTS xx_marketing
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE xx_marketing;

-- ============================================
-- 1. 用户表
-- ============================================
DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    password VARCHAR(255) COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT '' COMMENT '头像URL',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色: USER-普通用户, MERCHANT-商家',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 1-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 商家表
-- ============================================
DROP TABLE IF EXISTS tb_merchant;
CREATE TABLE tb_merchant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商家ID',
    user_id BIGINT NOT NULL COMMENT '关联用户ID',
    shop_name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    logo VARCHAR(500) DEFAULT '' COMMENT '店铺Logo',
    description VARCHAR(1000) DEFAULT '' COMMENT '店铺描述',
    type VARCHAR(50) DEFAULT '' COMMENT '店铺类型',
    rating DECIMAL(3,2) DEFAULT 5.00 COMMENT '评分',
    sales INT DEFAULT 0 COMMENT '销量',
    address VARCHAR(500) DEFAULT '' COMMENT '地址',
    business_hours VARCHAR(100) DEFAULT '' COMMENT '营业时间',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-营业, 0-歇业',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_sales (sales)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

-- ============================================
-- 3. 优惠券活动表
-- ============================================
DROP TABLE IF EXISTS tb_coupon_activity;
CREATE TABLE tb_coupon_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    merchant_id BIGINT NOT NULL COMMENT '关联商家ID',
    name VARCHAR(200) NOT NULL COMMENT '活动名称',
    description VARCHAR(1000) DEFAULT '' COMMENT '活动描述',
    amount DECIMAL(10,2) NOT NULL COMMENT '优惠券面值',
    min_amount DECIMAL(10,2) DEFAULT 0.00 COMMENT '最低消费金额',
    total_stock INT NOT NULL COMMENT '总库存',
    remain_stock INT NOT NULL COMMENT '剩余库存',
    max_per_user INT DEFAULT 3 COMMENT '每人限领数量',
    start_time DATETIME NOT NULL COMMENT '活动开始时间',
    end_time DATETIME NOT NULL COMMENT '活动结束时间',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-进行中, 0-已结束, 2-未开始',
    type VARCHAR(50) DEFAULT 'CASH' COMMENT '类型: FULL_REDUCTION-满减, DISCOUNT-折扣, CASH-代金券',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券活动表';

-- ============================================
-- 4. 优惠券订单表（用户抢到的券）
-- ============================================
DROP TABLE IF EXISTS tb_coupon_order;
CREATE TABLE tb_coupon_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    activity_id BIGINT NOT NULL COMMENT '优惠券活动ID',
    merchant_id BIGINT NOT NULL COMMENT '商家ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '优惠券面值',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-未使用, 1-已使用, 2-已过期',
    use_time DATETIME COMMENT '使用时间',
    expire_time DATETIME COMMENT '过期时间（默认领取后30天）',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_activity_id (activity_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券订单表';

-- ============================================
-- 5. 秒杀优惠券表
-- ============================================
DROP TABLE IF EXISTS tb_seckill_voucher;
CREATE TABLE tb_seckill_voucher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '秒杀ID',
    activity_id BIGINT NOT NULL COMMENT '关联优惠券活动ID',
    price DECIMAL(10,2) NOT NULL COMMENT '秒杀价格',
    stock INT NOT NULL COMMENT '库存数量',
    max_per_user INT DEFAULT 1 COMMENT '每人限购数量',
    begin_time DATETIME NOT NULL COMMENT '秒杀开始时间',
    end_time DATETIME NOT NULL COMMENT '秒杀结束时间',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-进行中, 0-已结束',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_activity_id (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀优惠券表';

-- ============================================
-- 插入测试数据
-- ============================================

-- 测试用户（密码: 123456，user_id 3-14 为商家）
INSERT INTO tb_user (phone, password, nickname, role, status) VALUES
('13800138000', '123456', '小明', 'USER', 1),
('13800138001', '123456', '小红', 'USER', 1),
('13800138002', '123456', '星巴克咖啡', 'MERCHANT', 1),
('13800138003', '123456', '海底捞火锅', 'MERCHANT', 1),
('13800138004', '123456', '肯德基', 'MERCHANT', 1),
('13800138005', '123456', '奈雪の茶', 'MERCHANT', 1),
('13800138006', '123456', '麦当劳', 'MERCHANT', 1),
('13800138007', '123456', '瑞幸咖啡', 'MERCHANT', 1),
('13800138008', '123456', '喜茶', 'MERCHANT', 1),
('13800138009', '123456', '必胜客', 'MERCHANT', 1),
('13800138010', '123456', '西贝莜面村', 'MERCHANT', 1),
('13800138011', '123456', '汉堡王', 'MERCHANT', 1),
('13800138012', '123456', '达美乐披萨', 'MERCHANT', 1),
('13800138013', '123456', '太二酸菜鱼', 'MERCHANT', 1);

-- 12家测试商家（logo 用 emoji，前端会展示）
INSERT INTO tb_merchant (user_id, shop_name, logo, description, type, rating, sales, address, business_hours, status) VALUES
(3,  '星巴克咖啡',   '☕', '全球知名咖啡连锁，臻选阿拉比卡豆',        '咖啡',   4.80, 15230, '北京市朝阳区建国路88号',   '07:00-22:00', 1),
(4,  '海底捞火锅',   '🍲', '以极致服务闻名的川味火锅连锁',          '火锅',   4.90, 28340, '北京市海淀区中关村大街1号', '10:00-02:00', 1),
(5,  '肯德基',      '🍗', '全球最大炸鸡快餐品牌，原味吮指鸡',       '快餐',   4.50, 45120, '北京市西城区西单北大街120号','07:00-23:00', 1),
(6,  '奈雪の茶',     '🍵', '新式茶饮开创者，霸气系列水果茶',         '茶饮',   4.60, 8920,  '北京市东城区王府井大街200号','10:00-22:00', 1),
(7,  '麦当劳',      '🍔', '全球汉堡快餐巨头，巨无霸经典美味',       '快餐',   4.55, 62100, '北京市朝阳区三里屯路19号',   '06:00-23:00', 1),
(8,  '瑞幸咖啡',    '☕', '中国新零售咖啡品牌，大师咖啡平价享受',     '咖啡',   4.40, 38500, '北京市海淀区五道口华清商务会馆','07:00-21:00', 1),
(9,  '喜茶',       '🧋', '灵感之茶，中国新式茶饮头部品牌',          '茶饮',   4.70, 18600, '北京市朝阳区太古里南区B1',   '10:00-22:00', 1),
(10, '必胜客',      '🍕', '全球知名披萨连锁，铁盘披萨经典传承',      '西餐',   4.35, 21900, '北京市西城区金融街购物中心3F', '10:00-22:00', 1),
(11, '西贝莜面村',  '🍜', '西北菜领军品牌，闭着眼睛点道道都好吃',     '中餐',   4.75, 12500, '北京市海淀区世纪金源5F',     '11:00-21:30', 1),
(12, '汉堡王',      '🍔', '火烤牛肉汉堡专家，皇堡经典',             '快餐',   4.30, 16800, '北京市朝阳区望京SOHO T1',   '09:00-22:00', 1),
(13, '达美乐披萨',  '🍕', '全球最大披萨外卖品牌，30分钟必达',        '西餐',   4.45, 14200, '北京市海淀区学院路甲9号',     '10:00-22:00', 1),
(14, '太二酸菜鱼',  '🐟', '老坛子酸菜鱼，酸菜比鱼好吃',              '中餐',   4.85, 21300, '北京市朝阳区大悦城6F',       '11:00-21:00', 1);

-- 优惠券活动（每家店 1-3 张）
INSERT INTO tb_coupon_activity (merchant_id, name, description, amount, min_amount, total_stock, remain_stock, max_per_user, start_time, end_time, status, type) VALUES
-- 星巴克 (merchant_id=1)
(1, '星巴克满50减10',  '全场饮品满50元减10元',                    10.00, 50.00,  200, 200, 3, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(1, '星巴克新品5折券',  '当季新品饮品5折，封顶优惠15元',           15.00, 30.00,  150, 150, 2, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 1, 'DISCOUNT'),
-- 海底捞 (2)
(2, '海底捞满200减50', '全场消费满200元减50元',                    50.00, 200.00, 80,  80,  1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(2, '海底捞30元券',    '全场通用30元代金券，无门槛',                30.00, 0.00,   150, 150, 3, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 1, 'CASH'),
(2, '海底捞88折',      '工作日午市88折，封顶优惠40元',             40.00, 150.00, 100, 100, 2, NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 1, 'DISCOUNT'),
-- 肯德基 (3)
(3, '肯德基满30减5',   '全场消费满30元减5元',                      5.00,  30.00,  500, 500, 5, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(3, '肯德基20元券',    '全场通用20元代金券',                       20.00, 0.00,   300, 300, 3, NOW(), DATE_ADD(NOW(), INTERVAL 25 DAY), 1, 'CASH'),
-- 奈雪 (4)
(4, '奈雪买一送一',    '指定饮品买一送一，最高优惠18元',            18.00, 18.00,  200, 200, 2, NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 1, 'FULL_REDUCTION'),
(4, '奈雪8折券',       '全场饮品8折，封顶优惠15元',                15.00, 20.00,  180, 180, 3, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 1, 'DISCOUNT'),
-- 麦当劳 (5)
(5, '麦当劳满25减5',   '全场消费满25元减5元',                      5.00,  25.00,  600, 600, 5, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(5, '麦当劳10元券',    '全场通用10元代金券，任意消费可用',          10.00, 0.00,   400, 400, 3, NOW(), DATE_ADD(NOW(), INTERVAL 25 DAY), 1, 'CASH'),
-- 瑞幸 (6)
(6, '瑞幸9.9元任选',   '全场饮品9.9元，最高优惠15元',              15.00, 9.99,  350, 350, 3, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 1, 'DISCOUNT'),
(6, '瑞幸满20减5',    '消费满20元减5元',                          5.00,  20.00,  400, 400, 5, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
-- 喜茶 (7)
(7, '喜茶满30减8',    '消费满30元减8元',                          8.00,  30.00,  250, 250, 3, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(7, '喜茶第二杯半价',  '同款饮品第二杯半价，最高优惠18元',          18.00, 18.00,  200, 200, 2, NOW(), DATE_ADD(NOW(), INTERVAL 15 DAY), 1, 'DISCOUNT'),
-- 必胜客 (8)
(8, '必胜客满88减20',  '消费满88元减20元',                         20.00, 88.00,  120, 120, 2, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(8, '必胜客50元券',    '全场通用50代金券',                          50.00, 0.00,   80,  80,  1, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 1, 'CASH'),
-- 西贝 (9)
(9, '西贝满100减25',  '消费满100元减25元',                         25.00, 100.00, 100, 100, 2, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(9, '西贝40元券',     '全场通用40元代金券',                         40.00, 0.00,   80,  80,  1, NOW(), DATE_ADD(NOW(), INTERVAL 25 DAY), 1, 'CASH'),
-- 汉堡王 (10)
(10,'汉堡王满20减5',   '消费满20元减5元',                           5.00,  20.00,  400, 400, 5, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(10,'汉堡王15元券',    '全场通用15元代金券',                         15.00, 0.00,   300, 300, 3, NOW(), DATE_ADD(NOW(), INTERVAL 25 DAY), 1, 'CASH'),
-- 达美乐 (11)
(11,'达美乐满60减15',  '消费满60元减15元',                          15.00, 60.00,  150, 150, 3, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(11,'达美乐25元券',    '全场通用25元代金券',                         25.00, 0.00,   120, 120, 2, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 1, 'CASH'),
-- 太二 (12)
(12,'太二满150减30',   '消费满150元减30元',                          30.00, 150.00, 80,  80,  1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, 'FULL_REDUCTION'),
(12,'太二酸菜鱼券',    '招牌老坛子酸菜鱼立减20元',                   20.00, 0.00,   100, 100, 2, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), 1, 'CASH');

