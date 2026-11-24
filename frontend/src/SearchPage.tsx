import { useEffect, useRef, useState } from 'react'
import { errorMessage, get } from './api.ts'
import type { Counts, FilterOptions, Filters, SearchResult } from './types.ts'

const LABELS: Record<string, string> = { university: 'University', faculty: 'Faculty', degreeLevel: 'Degree level', programType: 'Program type', deliveryMode: 'Delivery' }
const PAGE_SIZE = 10
const CHIP = 'chip btn btn-sm btn-outline-primary rounded-pill'

export default function SearchPage() {
  const [text, setText] = useState('')
  const [query, setQuery] = useState('')
  const [filters, setFilters] = useState<Filters>({})
  const [options, setOptions] = useState<FilterOptions>({})
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<SearchResult | null>(null)
  const [error, setError] = useState('')
  const [completions, setCompletions] = useState<string[]>([])
  const [history, setHistory] = useState<Counts>({})
  const latest = useRef(0)

  async function load(q: string, f: Filters, p: number, record: boolean) {
    const id = ++latest.current // ignore answers that arrive out of order
    try {
      const data = await get<SearchResult>('/search', { q, page: p, record, ...f })
      if (id !== latest.current) return
      setResult(data)
      setError('')
      setHistory(await get<Counts>('/history'))
    } catch (e) {
      if (id === latest.current) { setError(errorMessage(e)); setResult(null) }
    }
  }

  function run(q: string, f = filters, p = 0, record = true) { // filters and paging are not new searches
    setText(q); setQuery(q); setFilters(f); setPage(p); setCompletions([])
    load(q, f, p, record)
  }

  useEffect(() => {
    get<FilterOptions>('/filters').then(setOptions).catch(() => {})
    load('', {}, 0, false)
  }, [])

  // Word completion works on the last word being typed.
  const lastWord = text.match(/[\p{L}\p{N}]+$/u)?.[0] ?? ''
  useEffect(() => {
    if (lastWord.length < 2 || text === query) { setCompletions([]); return }
    let active = true
    const timer = setTimeout(() => get<string[]>('/complete', { prefix: lastWord }).then(c => active && setCompletions(c)).catch(() => {}), 150)
    return () => { active = false; clearTimeout(timer) }
  }, [text, query])

  const replaceWord = (from: string, to: string) => query.split(/\s+/).map(w => (w.toLowerCase() === from ? to : w)).join(' ')
  const pages = result ? Math.ceil(result.total / PAGE_SIZE) : 0

  return (
    <div className="row g-4">
      <section className="col-lg-9 order-lg-2">
        <form className="search input-group input-group-lg shadow-sm" onSubmit={e => { e.preventDefault(); run(text) }}>
          <input className="form-control" aria-label="Search programs" placeholder="Search programs, for example data science" maxLength={100}
            value={text} onChange={e => setText(e.target.value)} onKeyDown={e => e.key === 'Escape' && setCompletions([])} />
          <button className="btn btn-primary px-4">Search</button>
        </form>
        {completions.length > 0 && (
          <div className="chips d-flex flex-wrap gap-2 mt-2">
            {completions.map(c => (
              <button type="button" className={CHIP} key={c} onClick={() => run(text.slice(0, -lastWord.length) + c)}>{c}</button>
            ))}
          </div>
        )}
        {error && <div className="alert alert-danger mt-3" role="alert">{error}</div>}
        {result && <p className="text-secondary mt-3 mb-2">{result.total} program{result.total === 1 ? '' : 's'}</p>}
        {result && Object.entries(result.suggestions).map(([word, list]) => (
          <div className="suggestion alert alert-warning" key={word}>“{word}” was not found.{' '}
            {list.length > 0 && <>Did you mean {list.map(s => (
              <button type="button" className={`${CHIP} mx-1`} key={s} onClick={() => run(replaceWord(word, s))}>{s}</button>
            ))}?</>}
          </div>
        ))}
        {result?.hits.map(({ program: p, score }) => (
          <article className="card shadow-sm mb-3" key={p.url}>
            <div className="card-body">
              <h3 className="h5 card-title mb-1"><a className="text-decoration-none" href={p.url} target="_blank" rel="noreferrer">{p.title}</a></h3>
              <p className="text-secondary mb-2">{[p.university, p.fields.faculty].filter(Boolean).join(' · ')}</p>
              <div className="d-flex flex-wrap gap-2 mb-2">
                {['degreeLevel', 'programType', 'deliveryMode'].map(k => p.fields[k] && <span className="badge text-bg-light border" key={k}>{p.fields[k]}</span>)}
                {score > 0 && <span className="badge text-bg-primary">{score} occurrence{score === 1 ? '' : 's'}</span>}
              </div>
              <details>
                <summary className="text-primary">Details</summary>
                <dl className="mt-3 mb-0">
                  {Object.entries(p.fields).map(([k, v]) => (
                    <div className="row border-top mx-0 py-2" key={k}>
                      <dt className="col-sm-4 px-0">{k.replace(/([A-Z])/g, ' $1')}</dt><dd className="col-sm-8 px-0 mb-0">{v}</dd>
                    </div>
                  ))}
                </dl>
              </details>
            </div>
          </article>
        ))}
        {pages > 1 && (
          <div className="pager d-flex justify-content-center align-items-center gap-3">
            <button type="button" className="btn btn-outline-primary" disabled={page === 0} onClick={() => run(query, filters, page - 1, false)}>Previous</button>
            <span>Page {page + 1} of {pages}</span>
            <button type="button" className="btn btn-outline-primary" disabled={page + 1 >= pages} onClick={() => run(query, filters, page + 1, false)}>Next</button>
          </div>
        )}
      </section>

      <aside className="col-lg-3 order-lg-1">
        <div className="card shadow-sm mb-3">
          <div className="card-body">
            <h2 className="h5">Filters</h2>
            {Object.entries(options).map(([key, values]) => (
              <label className="form-label d-block small" key={key}>{LABELS[key] ?? key}
                <select className="form-select mt-1" value={filters[key] ?? ''} onChange={e => run(query, { ...filters, [key]: e.target.value }, 0, false)}>
                  <option value="">All</option>
                  {values.map(v => <option key={v}>{v}</option>)}
                </select>
              </label>
            ))}
          </div>
        </div>
        <div className="card shadow-sm">
          <div className="card-body">
            <h2 className="h5">Most searched</h2>
            <div className="d-flex flex-wrap gap-2">
              {Object.entries(history).map(([word, count]) => (
                <button type="button" className={CHIP} key={word} onClick={() => run(word)}>{word} <span className="badge text-bg-primary">{count}</span></button>
              ))}
            </div>
            {!Object.keys(history).length && <p className="text-secondary mb-0">No searches yet.</p>}
          </div>
        </div>
      </aside>
    </div>
  )
}
