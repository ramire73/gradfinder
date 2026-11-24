import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { errorMessage, get, post } from './api.ts'
import type { CrawlStatus, Stats } from './types.ts'

export default function CrawlerPage() {
  const [status, setStatus] = useState<CrawlStatus | null>(null)
  const [stats, setStats] = useState<Stats | null>(null)
  const [universities, setUniversities] = useState<Record<string, string>>({})
  const [university, setUniversity] = useState('')
  const [limit, setLimit] = useState('30')
  const [refresh, setRefresh] = useState(false)
  const [error, setError] = useState('')

  async function update() {
    try {
      const [s, t] = await Promise.all([get<CrawlStatus>('/crawl'), get<Stats>('/stats')])
      setStatus(s); setStats(t); setError('')
    } catch (e) { setError(errorMessage(e)) }
  }
  useEffect(() => {
    get<Record<string, string>>('/crawlers').then(u => { setUniversities(u); setUniversity(Object.keys(u)[0] ?? '') }).catch(e => setError(errorMessage(e)))
    update()
    const timer = setInterval(update, 2000)
    return () => clearInterval(timer)
  }, [])

  async function start(e: FormEvent) {
    e.preventDefault()
    try { setStatus(await post<CrawlStatus>('/crawl', { university, limit: Number(limit), refresh })) } catch (err) { setError(errorMessage(err)) }
  }

  return (
    <article className="card shadow-sm">
      <div className="card-body">
        <h2 className="h5 card-title">Crawler</h2>
        <p>Selenium opens a Chrome window and visits program pages one at a time. Please leave the window open.</p>
        <form className="d-flex flex-wrap align-items-end gap-3 mb-3" onSubmit={start}>
          <div>
            <label className="form-label" htmlFor="university">University</label>
            <select id="university" className="form-select" required value={university} onChange={e => setUniversity(e.target.value)}>
              {Object.entries(universities).map(([code, name]) => <option key={code} value={code}>{name}</option>)}
            </select>
          </div>
          <div>
            <label className="form-label" htmlFor="limit">Pages to visit</label>
            <input id="limit" className="form-control page-limit" type="number" min="1" max="1000" required
              value={limit} onChange={e => setLimit(e.target.value)} />
          </div>
          <div className="form-check mb-2">
            <input id="refresh" className="form-check-input" type="checkbox" checked={refresh} onChange={e => setRefresh(e.target.checked)} />
            <label className="form-check-label" htmlFor="refresh">Crawl saved pages again</label>
          </div>
          <button className="btn btn-primary" disabled={status?.running || !university}>
            {status?.running && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
            {status?.running ? 'Crawling…' : 'Start crawling'}
          </button>
        </form>
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        {status && <p aria-live="polite">{status.message}{status.running && ` (${status.saved} saved)`}</p>}
        {stats && <p className="text-secondary mb-0">{stats.programs} programs and {stats.vocabulary} different words indexed.</p>}
      </div>
    </article>
  )
}
