-- ============================================
-- 社交功能：好友申请 + 帖子 + 点赞
-- ============================================
USE xx_marketing;

-- 好友申请表
DROP TABLE IF EXISTS tb_friend_request;
CREATE TABLE tb_friend_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    from_user_id BIGINT NOT NULL COMMENT '发起方用户ID',
    to_user_id BIGINT NOT NULL COMMENT '接收方用户ID',
    from_phone VARCHAR(20) NOT NULL COMMENT '发起方手机号',
    to_phone VARCHAR(20) NOT NULL COMMENT '接收方手机号',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING-待处理, ACCEPTED-已通过, REJECTED-已拒绝',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_from_user (from_user_id),
    INDEX idx_to_user (to_user_id),
    INDEX idx_status (status),
    UNIQUE KEY uk_from_to (from_user_id, to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友申请表';

-- 帖子表
DROP TABLE IF EXISTS tb_post;
CREATE TABLE tb_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '作者用户ID',
    content VARCHAR(2000) NOT NULL COMMENT '帖子内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子表';

-- 点赞表
DROP TABLE IF EXISTS tb_post_like;
CREATE TABLE tb_post_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_post_id (post_id),
    UNIQUE KEY uk_post_user (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞表';

-- ============================================
-- 好友关系辅助视图（双向关系）
-- 实际查询时用 EXISTS 子查询判断是否为好友
-- 判断条件: 存在 ACCEPTED 的申请记录，无论方向
-- ============================================
