import { useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { errorMessage, get, post } from './api.ts'
import type { Counts, Frequency, IndexEntry, PageLink, Spelling } from './types.ts'

type Values = Record<string, string>
interface ToolProps<T> {
  title: string
  about: string
  children: ReactNode
  action: (values: Values) => Promise<T>
  show: (result: T) => ReactNode
}

function Tool<T>({ title, about, children, action, show }: ToolProps<T>) {
  const [result, setResult] = useState<T | null>(null)
  const [error, setError] = useState('')
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const values: Values = {}
    new FormData(e.currentTarget).forEach((v, k) => { values[k] = String(v) })
    try {
      const answer = await action(values)
      setResult(() => answer); setError('')
    } catch (err) { setError(errorMessage(err)); setResult(null) }
  }
  return (
    <div className="col">
      <article className="card shadow-sm h-100">
        <div className="card-body">
          <h3 className="h5 card-title">{title}</h3>
          <p className="text-secondary small">{about}</p>
          <form className="d-grid gap-2 mb-3" onSubmit={submit}>{children}<button className="btn btn-primary">Run</button></form>
          <div className="output" aria-live="polite">
            {error && <div className="alert alert-danger mb-0">{error}</div>}
            {result !== null && show(result)}
          </div>
        </div>
      </article>
    </div>
  )
}

function Rows({ rows }: { rows: [string, number][] }) {
  if (rows.length === 0) return <p className="mb-0">Nothing found.</p>
  return (
    <table className="table table-sm table-striped mb-0">
      <tbody>{rows.map(([k, v], i) => <tr key={i}><td>{k}</td><td className="text-end">{v}</td></tr>)}</tbody>
    </table>
  )
}

export default function ToolsPage() {
  const [pages, setPages] = useState<PageLink[]>([])
  const [patterns, setPatterns] = useState<string[]>([])
  const [rules, setRules] = useState<string[]>([])
  const [error, setError] = useState('')
  useEffect(() => {
    Promise.all([get<PageLink[]>('/pages'), get<string[]>('/pattern-types'), get<string[]>('/validation-types')])
      .then(([p, t, r]) => { setPages(p); setPatterns(t); setRules(r) }).catch(e => setError(errorMessage(e)))
  }, [])
  const pageOptions = pages.map(p => <option key={p.url} value={p.url}>{p.title}</option>)

  return (
    <>
      {error && <div className="alert alert-danger" role="alert">{error}</div>}
      <div className="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-3">
        <Tool title="Frequency count" about="How often a word occurs on one crawled page."
          action={v => get<Frequency>('/frequency', v)}
          show={r => <p className="mb-0">“{r.word}” occurs <b>{r.count}</b> time{r.count === 1 ? '' : 's'}.</p>}>
          <input className="form-control" name="word" aria-label="Word" placeholder="Word" required />
          <select className="form-select" name="url" aria-label="Page" required>{pageOptions}</select>
        </Tool>
        <Tool title="Spell check" about="Edit distance against every crawled word."
          action={v => get<Spelling>('/spell', v)}
          show={r => r.correct ? <p className="mb-0">“{r.word}” is in the vocabulary.</p>
            : <p className="mb-0">{r.suggestions.length ? 'Did you mean: ' + r.suggestions.join(', ') + '?' : 'No similar words.'}</p>}>
          <input className="form-control" name="word" aria-label="Word" placeholder="For example reserch" required />
        </Tool>
        <Tool title="Inverted index" about="Pages that contain a word, most occurrences first."
          action={v => get<IndexEntry[]>('/index', v)}
          show={r => <Rows rows={r.map(x => [x.title, x.count])} />}>
          <input className="form-control" name="word" aria-label="Word" placeholder="Word" required />
        </Tool>
        <Tool title="Find patterns" about="Regular expressions over the crawled text."
          action={v => get<Counts>('/patterns', v)} show={r => <Rows rows={Object.entries(r)} />}>
          <select className="form-select" name="type" aria-label="Pattern">{patterns.map(p => <option key={p}>{p}</option>)}</select>
          <select className="form-select" name="url" aria-label="Page"><option value="">All pages</option>{pageOptions}</select>
        </Tool>
        <Tool title="Validate data" about="Checks a value with a regular expression."
          action={v => post<{ valid: boolean }>('/validate', v)}
          show={r => <span className={`badge ${r.valid ? 'text-bg-success' : 'text-bg-danger'}`}>{r.valid ? 'Valid' : 'Not valid'}</span>}>
          <select className="form-select" name="type" aria-label="Type">{rules.map(r => <option key={r}>{r}</option>)}</select>
          <input className="form-control" name="value" aria-label="Value" placeholder="Value" required />
        </Tool>
      </div>
    </>
  )
}
