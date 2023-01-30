-- 1. 需要用到的秒杀券id和用户id以及订单Id, 将其作为参数传入
local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

-- 2. 用voucherId分别拼接成找库存和订单的key
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 3. 判断库存是否充足、是否购买过, 可以购买的话扣减库存
if (tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足返回1
    return 1
end

if (redis.call('sismember', orderKey, userId) == 1) then
    -- 重复下单返回2
    return 2
end

-- 具备购买资格, 库存加-1, 再将用户id加入set以去重
redis.call('incrby', stockKey, -1)
redis.call('sadd', orderKey, userId)
-- 发送消息到消息队列中
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
return 0
