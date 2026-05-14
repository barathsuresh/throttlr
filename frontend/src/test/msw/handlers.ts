import { http, HttpResponse } from 'msw'

export const handlers = [
  http.get('http://127.0.0.1:8080/api/health', () =>
    HttpResponse.json({ status: 'UP', component: 'throttlr' })
  ),

  http.post('http://127.0.0.1:8080/api/auth/register', () =>
    HttpResponse.json(
      { accountId: 'acc-1', passphrase: 'word1 word2 word3 word4 word5 word6 word7 word8 word9 word10 word11 word12', message: 'Save this.' },
      { status: 201 }
    )
  ),

  http.post('http://127.0.0.1:8080/api/auth/login', () =>
    HttpResponse.json({ sessionToken: 'test-jwt', accountId: 'acc-1' })
  ),

  http.get('http://127.0.0.1:8080/api/auth/me', () =>
    HttpResponse.json({ accountId: 'acc-1' })
  ),

  http.get('http://127.0.0.1:8080/api/apps', () =>
    HttpResponse.json({
      items: [{ appId: 'app-1', name: 'Test App', ruleCount: 2, createdAt: 1700000000000 }],
      page: 0, size: 10, totalItems: 1, totalPages: 1, hasNext: false, hasPrevious: false,
    })
  ),

  http.post('http://127.0.0.1:8080/api/apps', () =>
    HttpResponse.json(
      { appId: 'app-2', name: 'New App', appKey: 'throttlr_live_abc123', message: 'Save this key.' },
      { status: 201 }
    )
  ),

  http.delete('http://127.0.0.1:8080/api/apps/:appId', () =>
    new HttpResponse(null, { status: 204 })
  ),

  http.post('http://127.0.0.1:8080/api/apps/:appId/rotate-key', () =>
    HttpResponse.json({ appId: 'app-1', name: 'Test App', appKey: 'throttlr_live_newkey123', message: 'Save this new key.' })
  ),

  http.get('http://127.0.0.1:8080/api/apps/:appId/analytics', () =>
    HttpResponse.json({ appId: 'app-1', hour: 1700000000000, total: 10, allowed: 7, blocked: 3 })
  ),

  http.get('http://127.0.0.1:8080/api/apps/:appId/rules', () =>
    HttpResponse.json({
      items: [{ ruleId: 'rule-1', clientId: 'user:1', algorithm: 'FIXED_WINDOW', limitPerWindow: 10, windowMs: 60000 }],
      page: 0, size: 10, totalItems: 1, totalPages: 1, hasNext: false, hasPrevious: false,
    })
  ),

  http.post('http://127.0.0.1:8080/api/apps/:appId/rules', () =>
    HttpResponse.json(
      { ruleId: 'rule-2', clientId: 'user:2', algorithm: 'FIXED_WINDOW', limitPerWindow: 5, windowMs: 60000 },
      { status: 201 }
    )
  ),

  http.put('http://127.0.0.1:8080/api/apps/:appId/rules/:clientId', () =>
    HttpResponse.json({ ruleId: 'rule-1', clientId: 'user:1', algorithm: 'TOKEN_BUCKET', limitPerWindow: 5, windowMs: 60000 })
  ),

  http.delete('http://127.0.0.1:8080/api/apps/:appId/rules/:clientId', () =>
    new HttpResponse(null, { status: 204 })
  ),

  http.post('http://127.0.0.1:8080/api/demo/app-key', () =>
    HttpResponse.json(
      {
        appId: 'demo-1',
        appKey: 'throttlr_live_demo123',
        expiresInMs: 900000,
        rules: [
          { clientId: 'demo-user-fixed-window', algorithm: 'FIXED_WINDOW', limitPerWindow: 10, windowMs: 60000 },
          { clientId: 'demo-user-token-bucket', algorithm: 'TOKEN_BUCKET', limitPerWindow: 10, windowMs: 60000 },
          { clientId: 'demo-user-sliding-window', algorithm: 'SLIDING_WINDOW', limitPerWindow: 10, windowMs: 60000 },
        ],
        message: 'Temporary demo app key created.',
      },
      { status: 201 }
    )
  ),

  http.post('http://127.0.0.1:8080/api/check', () =>
    HttpResponse.json({ allowed: true, remaining: 9, resetAfterMs: 60000, retryAfterMs: 0 })
  ),
]
