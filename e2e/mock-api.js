const http = require('node:http');
const accounts = Array.from({ length: 55 }, (_, i) => ({
  id: `acct_${String(i + 1).padStart(4, '0')}`, name: `Cedar ${String(i + 1).padStart(2, '0')}`,
  industry: 'technology', country: 'US', plan: 'business', monthly_spend_usd: 1234.50,
  open_tickets: 3, days_since_last_login: 12, created_at: '2024-01-02',
  risk_score: i === 0 ? null : i === 1 ? 39 : i === 2 ? 40 : 70,
}));
http.createServer((req, res) => {
  const url = new URL(req.url, 'http://localhost');
  let result;
  if (url.pathname === '/accounts') {
    let filtered = accounts.filter(a =>
      (!url.searchParams.get('industry') || a.industry === url.searchParams.get('industry')) &&
      (!url.searchParams.get('q') || a.name.toLowerCase().includes(url.searchParams.get('q').toLowerCase())) &&
      (url.searchParams.get('high_risk') !== 'true' || a.risk_score >= 70));
    const limit = Number(url.searchParams.get('limit') || 50);
    const offset = Number(url.searchParams.get('offset') || 0);
    result = { accounts: filtered.slice(offset, offset + limit), total: filtered.length, limit, offset };
  } else {
    result = accounts.find(a => `/accounts/${a.id}` === url.pathname);
  }
  res.writeHead(result ? 200 : 404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(result || { detail: 'Not found' }));
}).listen(18102, '127.0.0.1');
