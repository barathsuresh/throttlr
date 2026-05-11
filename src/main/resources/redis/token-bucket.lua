local limit = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'updatedAt')
local tokens = tonumber(bucket[1])
local updatedAt = tonumber(bucket[2])

if tokens == nil then
    tokens = limit
end

if updatedAt == nil then
    updatedAt = now
end

local elapsed = now - updatedAt
if elapsed < 0 then
    elapsed = 0
end

local refillRate = limit / windowMs
tokens = math.min(limit, tokens + (elapsed * refillRate))

local allowed = 0
if tokens >= 1 then
    allowed = 1
    tokens = tokens - 1
end

local remaining = math.floor(tokens)
local retryAfter = 0
if allowed == 0 then
    retryAfter = math.ceil((1 - tokens) / refillRate)
end

redis.call('HMSET', KEYS[1], 'tokens', tokens, 'updatedAt', now)
redis.call('PEXPIRE', KEYS[1], windowMs)

return { allowed, remaining, windowMs, retryAfter }
