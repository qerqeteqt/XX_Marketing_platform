-- ========================================
-- 抢券 Lua 脚本
-- 功能：原子性完成库存校验、扣减、用户资格检查
-- 
-- KEYS[1] = coupon:stock:{活动ID}
-- ARGV[1] = coupon:user:{活动ID} （用户已抢集合 Key）
-- ARGV[2] = 用户ID
-- ARGV[3] = 每人最大领取数
--
-- 返回值: 1-成功, -1-库存不足, -2-达到上限, -3-已领取, -4-活动结束
-- ========================================

local stockKey = KEYS[1]
local userKey = ARGV[1]
local userId = ARGV[2]
local maxPerUser = tonumber(ARGV[3])

-- 1. 检查库存
local stock = redis.call('GET', stockKey)
if not stock then
    return {-4}  -- 活动不存在
end

stock = tonumber(stock)
if stock <= 0 then
    return {-1}  -- 库存不足
end

-- 2. 检查用户是否已领取该活动优惠券（Set 判重）
local isMember = redis.call('SISMEMBER', userKey, userId)
if isMember == 1 then
    return {-3}  -- 已领取
end

-- 3. 检查用户领取总数
local userCount = redis.call('SCARD', userKey .. ':' .. userId)
if userCount >= maxPerUser then
    return {-2}  -- 达到上限
end

-- 4. 扣减库存
redis.call('DECR', stockKey)

-- 5. 记录用户领取
redis.call('SADD', userKey, userId)          -- 活动维度的用户记录
redis.call('SADD', userKey .. ':' .. userId, '1')  -- 用户维度的活动计数

-- 6. 返回成功
return {1}
