local limit = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local member = ARGV[4]
local cutoff = now - windowMs

redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, cutoff)

local current = redis.call('ZCARD', KEYS[1])
local allowed = 0
if current < limit then
    allowed = 1
    redis.call('ZADD', KEYS[1], now, member)
    current = current + 1
end

redis.call('PEXPIRE', KEYS[1], windowMs)

local remaining = limit - current
if remaining < 0 then
    remaining = 0
end

local resetAfter = windowMs
local retryAfter = 0
if current > 0 then
    local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
    if oldest[2] ~= nil then
        resetAfter = math.max(0, (tonumber(oldest[2]) + windowMs) - now)
    end
end

if allowed == 0 then
    retryAfter = resetAfter
end

return { allowed, remaining, resetAfter, retryAfter }
