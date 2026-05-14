import { Badge } from '@/components/ui/badge'
import { AppShell } from '@/components/AppShell'
import { API_BASE_URL } from '@/api/axios'

interface EndpointDoc {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  path: string
  title: string
  group: 'Runtime' | 'Apps' | 'Rules' | 'Analytics' | 'System'
  purpose: string
  auth: string
  headers?: string[]
  body?: string
  response: string
  notes?: string[]
}

const endpoints: EndpointDoc[] = [
  {
    method: 'GET',
    path: '/api/health',
    title: 'Check API health',
    group: 'System',
    purpose: 'Use this to confirm the Throttlr API is reachable.',
    auth: 'Public',
    response: `{
  "component": "throttlr",
  "status": "UP",
  "message": "Application is running"
}`,
  },
  {
    method: 'POST',
    path: '/api/check',
    title: 'Run a rate-limit check',
    group: 'Runtime',
    purpose: 'This is the main runtime endpoint your backend calls before allowing a user request.',
    auth: 'App key required',
    headers: ['Content-Type: application/json', 'X-App-Key: throttlr_live_...'],
    body: `{
  "clientId": "user:123"
}`,
    response: `{
  "allowed": true,
  "remaining": 9,
  "retryAfterMs": 0,
  "resetAfterMs": 60000
}`,
    notes: [
      'Call this from your server, not directly from an untrusted browser.',
      'clientId can be a user id, IP key, tenant id, API consumer id, or any stable identity.',
      'If allowed is false, block or delay the request and use retryAfterMs to tell the caller when to retry.',
    ],
  },
  {
    method: 'POST',
    path: '/api/demo/app-key',
    title: 'Create a temporary demo app key',
    group: 'Runtime',
    purpose: 'Use this only for public demos or local playground flows.',
    auth: 'Public',
    response: `{
  "appId": "demo-...",
  "appKey": "throttlr_live_...",
  "clientId": "demo-user",
  "limitPerWindow": 10,
  "windowMs": 60000,
  "expiresInMs": 900000
}`,
    notes: [
      'Demo keys are temporary.',
      'Production integrations should use app keys created from the dashboard app flow.',
    ],
  },
  {
    method: 'GET',
    path: '/api/apps',
    title: 'List apps',
    group: 'Apps',
    purpose: 'Fetch paginated apps owned by the logged-in account.',
    auth: 'JWT required',
    headers: ['Authorization: Bearer <sessionToken>'],
    response: `{
  "items": [
    {
      "appId": "6a...",
      "name": "Acme API",
      "ruleCount": 3,
      "createdAt": 1778374800000
    }
  ],
  "page": 0,
  "size": 10,
  "totalItems": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}`,
    notes: ['Query params: page defaults to 0, size defaults to 10.'],
  },
  {
    method: 'POST',
    path: '/api/apps',
    title: 'Create an app',
    group: 'Apps',
    purpose: 'Create a new application and receive its app key once.',
    auth: 'JWT required',
    headers: ['Authorization: Bearer <sessionToken>', 'Content-Type: application/json'],
    body: `{
  "name": "Acme API"
}`,
    response: `{
  "appId": "6a...",
  "name": "Acme API",
  "appKey": "throttlr_live_...",
  "message": "Save this app key now because it will not be shown again."
}`,
    notes: ['Store appKey securely. Throttlr does not show the raw key again.'],
  },
  {
    method: 'DELETE',
    path: '/api/apps/{appId}',
    title: 'Delete an app',
    group: 'Apps',
    purpose: 'Remove an app and its related runtime access.',
    auth: 'JWT required',
    headers: ['Authorization: Bearer <sessionToken>'],
    response: '204 No Content',
  },
  {
    method: 'GET',
    path: '/api/apps/{appId}/rules',
    title: 'List rules',
    group: 'Rules',
    purpose: 'Fetch paginated rate-limit rules for an app.',
    auth: 'JWT required',
    headers: ['Authorization: Bearer <sessionToken>'],
    response: `{
  "items": [
    {
      "ruleId": "6a...",
      "clientId": "user:*",
      "algorithm": "FIXED_WINDOW",
      "limitPerWindow": 100,
      "windowMs": 60000
    }
  ],
  "page": 0,
  "size": 10,
  "totalItems": 1,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}`,
  },
  {
    method: 'POST',
    path: '/api/apps/{appId}/rules',
    title: 'Create a rule',
    group: 'Rules',
    purpose: 'Define how a clientId or wildcard pattern should be limited.',
    auth: 'JWT required',
    headers: ['Authorization: Bearer <sessionToken>', 'Content-Type: application/json'],
    body: `{
  "clientId": "user:*",
  "algorithm": "FIXED_WINDOW",
  "limitPerWindow": 100,
  "windowMs": 60000
}`,
    response: `{
  "ruleId": "6a...",
  "clientId": "user:*",
  "algorithm": "FIXED_WINDOW",
  "limitPerWindow": 100,
  "windowMs": 60000
}`,
    notes: [
      'Supported algorithms: FIXED_WINDOW, TOKEN_BUCKET, SLIDING_WINDOW.',
      'clientId can be exact, such as user:123, or wildcard, such as user:*, ip:10.0.*, or *.',
    ],
  },
  {
    method: 'PUT',
    path: '/api/apps/{appId}/rules/{clientId}',
    title: 'Update a rule',
    group: 'Rules',
    purpose: 'Change the algorithm, limit, or window for an existing rule.',
    auth: 'JWT required',
    headers: ['Authorization: Bearer <sessionToken>', 'Content-Type: application/json'],
    body: `{
  "clientId": "user:*",
  "algorithm": "TOKEN_BUCKET",
  "limitPerWindow": 50,
  "windowMs": 60000
}`,
    response: `{
  "ruleId": "6a...",
  "clientId": "user:*",
  "algorithm": "TOKEN_BUCKET",
  "limitPerWindow": 50,
  "windowMs": 60000
}`,
    notes: ['clientId cannot be changed during update. Use delete + create if you need a different clientId.'],
  },
  {
    method: 'DELETE',
    path: '/api/apps/{appId}/rules/{clientId}',
    title: 'Delete a rule',
    group: 'Rules',
    purpose: 'Remove a rule from an app.',
    auth: 'JWT required',
    headers: ['Authorization: Bearer <sessionToken>'],
    response: '204 No Content',
  },
  {
    method: 'GET',
    path: '/api/apps/{appId}/analytics',
    title: 'Read current analytics',
    group: 'Analytics',
    purpose: 'Read the current hourly total, allowed, and blocked decision counters.',
    auth: 'JWT required',
    headers: ['Authorization: Bearer <sessionToken>'],
    response: `{
  "appId": "6a...",
  "hour": 1778374800000,
  "total": 120,
  "allowed": 104,
  "blocked": 16
}`,
  },
]

const methodStyles: Record<EndpointDoc['method'], string> = {
  GET: 'bg-emerald-100 text-emerald-900 border-emerald-300 dark:bg-emerald-900/30 dark:text-emerald-300 dark:border-emerald-700',
  POST: 'bg-amber-100 text-amber-900 border-amber-300 dark:bg-amber-900/30 dark:text-amber-300 dark:border-amber-700',
  PUT: 'bg-cyan-100 text-cyan-900 border-cyan-300 dark:bg-cyan-900/30 dark:text-cyan-300 dark:border-cyan-700',
  DELETE: 'bg-red-100 text-red-900 border-red-300 dark:bg-red-900/30 dark:text-red-300 dark:border-red-700',
}

function CodeBlock({ children }: { children: string }) {
  return (
    <pre className="max-h-72 overflow-auto rounded-xl border border-white/10 bg-slate-950 p-4 text-[0.8rem] leading-6 text-slate-100 shadow-inner">
      <code>{children}</code>
    </pre>
  )
}

function EndpointCard({ endpoint }: { endpoint: EndpointDoc }) {
  const id = `${endpoint.method}-${endpoint.path}`.replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '').toLowerCase()

  return (
    <section id={id} className="scroll-mt-28 rounded-2xl border border-slate-900/10 bg-white/80 shadow-sm backdrop-blur dark:border-white/10 dark:bg-slate-800/70">
      <div className="border-b border-slate-900/10 p-4 dark:border-white/10 md:p-5">
        <div className="flex flex-wrap items-center gap-3">
          <Badge variant="outline" className={methodStyles[endpoint.method]}>
          {endpoint.method}
          </Badge>
          <code className="break-all rounded-lg bg-slate-950/5 px-2.5 py-1 font-mono text-sm text-slate-950 dark:bg-white/5 dark:text-slate-100">
            {endpoint.path}
          </code>
          <Badge variant="outline" className="ml-auto rounded-full bg-white text-slate-600 dark:bg-slate-800 dark:text-slate-400">
            {endpoint.auth}
          </Badge>
        </div>
        <h2 className="mt-3 font-heading text-xl font-bold tracking-tight md:text-2xl">{endpoint.title}</h2>
        <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600 dark:text-slate-400">{endpoint.purpose}</p>
      </div>

      <div className="grid gap-0 lg:grid-cols-[0.75fr_1.25fr]">
        <div className="space-y-3">
          {endpoint.headers && (
            <div className="border-b border-slate-900/10 p-4 dark:border-white/10 lg:border-r">
              <p className="font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">Headers</p>
              <ul className="mt-3 space-y-2">
                {endpoint.headers.map((header) => (
                  <li key={header} className="break-all rounded-lg bg-slate-950/5 px-3 py-2 font-mono text-xs text-slate-800 dark:bg-white/5 dark:text-slate-200">
                    {header}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {endpoint.notes && (
            <div className="p-4 dark:border-white/10 lg:border-r">
              <p className="font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">Notes</p>
              <ul className="mt-3 space-y-2 text-sm leading-6 text-slate-600 dark:text-slate-400">
                {endpoint.notes.map((note) => (
                  <li key={note} className="flex gap-2">
                    <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-amber-500" />
                    <span>{note}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {!endpoint.headers && !endpoint.notes && (
            <div className="p-4 text-sm text-slate-500 dark:text-slate-400 lg:border-r">
              No headers or notes required.
            </div>
          )}
        </div>

        <div className="space-y-4 p-4">
          {endpoint.body && (
            <div>
              <p className="mb-2 font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">Request body</p>
              <CodeBlock>{endpoint.body}</CodeBlock>
            </div>
          )}
          <div>
            <p className="mb-2 font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">Response</p>
            <CodeBlock>{endpoint.response}</CodeBlock>
          </div>
        </div>
      </div>
    </section>
  )
}

export function Docs() {
  const groupedEndpoints = endpoints.reduce<Record<EndpointDoc['group'], EndpointDoc[]>>(
    (groups, endpoint) => {
      groups[endpoint.group].push(endpoint)
      return groups
    },
    { Runtime: [], Apps: [], Rules: [], Analytics: [], System: [] }
  )

  return (
    <AppShell>
      <div className="mb-8 rounded-[2rem] border border-slate-900/10 bg-white/70 p-5 shadow-sm backdrop-blur dark:border-white/10 dark:bg-slate-800/70 md:p-7">
        <p className="font-mono text-xs font-semibold uppercase tracking-[0.24em] text-slate-500 dark:text-slate-400">API Usage</p>
        <div className="mt-3 grid gap-5 lg:grid-cols-[1fr_auto] lg:items-end">
          <div>
            <h1 className="font-heading text-4xl font-black tracking-[-0.04em] text-slate-950 dark:text-slate-50 md:text-6xl">
              Throttlr API reference
            </h1>
            <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600 dark:text-slate-400 md:text-base">
              Endpoint-by-endpoint usage for calling Throttlr: required method, headers, body, response, and integration notes.
            </p>
          </div>
          <div className="rounded-2xl bg-slate-950 p-4 text-left shadow-inner lg:min-w-96">
            <p className="font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-400">Base URL</p>
            <p className="mt-2 break-all font-mono text-sm text-amber-200">{API_BASE_URL}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[17rem_1fr]">
        <aside className="hidden lg:block">
          <div className="sticky top-28 rounded-2xl border border-slate-900/10 bg-white/75 p-3 shadow-sm backdrop-blur dark:border-white/10 dark:bg-slate-800/70">
            <p className="px-3 py-2 font-mono text-[0.65rem] uppercase tracking-[0.18em] text-slate-500 dark:text-slate-400">Endpoints</p>
            <nav className="space-y-1">
              {endpoints.map((endpoint) => {
                const id = `${endpoint.method}-${endpoint.path}`.replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '').toLowerCase()
                return (
                  <a
                    key={id}
                    href={`#${id}`}
                    className="block rounded-xl px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-950 hover:text-amber-200 dark:text-slate-400 dark:hover:bg-slate-950 dark:hover:text-amber-200"
                  >
                    <span className="mr-2 font-mono text-[0.7rem]">{endpoint.method}</span>
                    {endpoint.title}
                  </a>
                )
              })}
            </nav>
          </div>
        </aside>

        <div className="space-y-8">
          {Object.entries(groupedEndpoints).map(([group, groupEndpoints]) => (
            groupEndpoints.length > 0 && (
              <section key={group} className="space-y-3">
                <div className="flex items-center gap-3">
                  <h2 className="font-heading text-2xl font-bold tracking-tight">{group}</h2>
                  <div className="h-px flex-1 bg-slate-900/10 dark:bg-white/10" />
                </div>
                {groupEndpoints.map((endpoint) => (
                  <EndpointCard key={`${endpoint.method}-${endpoint.path}`} endpoint={endpoint} />
                ))}
              </section>
            )
          ))}
        </div>
      </div>
    </AppShell>
  )
}
