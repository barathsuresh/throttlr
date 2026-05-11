local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('PEXPIRE', KEYS[1], ARGV[2])
end

local ttl = redis.call('PTTL', KEYS[1])
if ttl < 0 then
    ttl = tonumber(ARGV[2])
end

local limit = tonumber(ARGV[1])
local allowed = 0
if current <= limit then
    allowed = 1
end

local remaining = limit - current
if remaining < 0 then
    remaining = 0
end

local retryAfter = 0
if allowed == 0 then
    retryAfter = ttl
end

return { allowed, remaining, ttl, retryAfter }
