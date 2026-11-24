import { useEffect, useState } from 'react'
import { errorMessage, get } from './api.ts'
import type { DatasetReport } from './types.ts'

export default function DatasetPage() {
  const [report, setReport] = useState<DatasetReport | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { get<DatasetReport>('/dataset/report').then(setReport).catch(e => setError(errorMessage(e))) }, [])

  if (error) return <div className="alert alert-danger" role="alert">{error}</div>
  if (!report) return <p>Loading…</p>
  return (
    <>
      <article className="card shadow-sm mb-3">
        <div className="card-body">
          <h2 className="h5 card-title">Combined dataset</h2>
          <p>{report.programs} programs from {Object.keys(report.programsPerUniversity).length} {Object.keys(report.programsPerUniversity).length === 1 ? 'university' : 'universities'}, in one format.</p>
          <ul>{Object.entries(report.programsPerUniversity).map(([name, n]) => <li key={name}>{name}: {n} (<a href={`/api/dataset.csv?university=${encodeURIComponent(name)}`} download={`${name}.csv`}>CSV</a>)</li>)}</ul>
          <a className="btn btn-primary me-2" href="/api/dataset.csv" download>Download combined CSV</a>
          <a className="btn btn-outline-primary" href="/api/dataset" target="_blank" rel="noreferrer">View JSON</a>
        </div>
      </article>
      <div className="row g-3">
        <div className="col-lg-5">
          <article className="card shadow-sm">
            <div className="card-body">
              <h3 className="h5 card-title">Programs with a value in each column</h3>
              <table className="table table-sm table-striped mb-0">
                <tbody>{Object.entries(report.filledPerColumn).map(([column, n]) => <tr key={column}><td>{column}</td><td className="text-end">{n}</td></tr>)}</tbody>
              </table>
            </div>
          </article>
        </div>
        <div className="col-lg-7">
          <article className="card shadow-sm">
            <div className="card-body">
              <h3 className="h5 card-title">Values that need checking</h3>
              {report.issues.length === 0 ? <p className="mb-0">None. Every value fits the common format.</p> : (
                <table className="table table-sm mb-0">
                  <thead><tr><th>Program</th><th>Column</th><th>Value</th><th>Reason</th></tr></thead>
                  <tbody>{report.issues.map((i, n) => <tr key={n}><td>{i.university}: {i.program}</td><td>{i.column}</td><td>{i.value}</td><td>{i.reason}</td></tr>)}</tbody>
                </table>
              )}
            </div>
          </article>
        </div>
      </div>
    </>
  )
}
