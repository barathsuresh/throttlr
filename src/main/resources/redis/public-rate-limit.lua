local current = redis.call('INCR', KEYS[1])
local limit = tonumber(ARGV[1])
local window_ms = tonumber(ARGV[2])

if current == 1 then
  redis.call('PEXPIRE', KEYS[1], window_ms)
end

local ttl = redis.call('PTTL', KEYS[1])
if ttl < 0 then
  redis.call('PEXPIRE', KEYS[1], window_ms)
  ttl = window_ms
end

if current > limit then
  return {0, 0, ttl}
end

return {1, limit - current, 0}
