-- 比较线程标识与锁中的是否一致
if (ARGV[1] == redis.call('get', KEYS[1])) then
    -- 释放锁
    return redis.call('del', KEYS[1])
end
return 0

-- -- 动态传参: 
-- -- 锁的key
-- local key = KEYS[1]
-- -- 当前线程标识
-- local threadId = ARGV[1]

-- -- 获取锁中的线程标识
-- local id = redis.call('get', key)
-- -- 比较线程标识与锁中的是否一致
-- if (threadId == id) then
--     -- 释放锁
--     return redis.call('del', key)
-- end
-- return 0

-- -- 具体参数:
-- -- 锁的key
-- local key = "lock:order:5"
-- -- 当前线程标识
-- local threadId = "akldljawkjfoi-39"

-- -- 获取锁中的线程标识
-- local id = redis.call('get', key)
-- -- 比较线程标识与锁中的是否一致
-- if (threadId == id) then
--     -- 释放锁
--     return redis.call('del', key)
-- end
-- return 0

