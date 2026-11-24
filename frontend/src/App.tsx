import { useState } from 'react'
import SearchPage from './SearchPage.tsx'
import ToolsPage from './ToolsPage.tsx'
import CrawlerPage from './CrawlerPage.tsx'
import DatasetPage from './DatasetPage.tsx'

const TABS = { Search: SearchPage, 'Text tools': ToolsPage, Crawler: CrawlerPage, Dataset: DatasetPage }
type Tab = keyof typeof TABS

export default function App() {
  const [tab, setTab] = useState<Tab>('Search')
  const Page = TABS[tab]
  return (
    <>
      <header className="navbar navbar-expand bg-primary shadow-sm" data-bs-theme="dark">
        <div className="container flex-wrap">
          <span className="navbar-brand fw-bold">GradFinder</span>
          <nav className="navbar-nav">
            {(Object.keys(TABS) as Tab[]).map(t => (
              <button key={t} type="button" className={`nav-link ${t === tab ? 'active fw-semibold' : ''}`}
                aria-current={t === tab ? 'page' : undefined} onClick={() => setTab(t)}>{t}</button>
            ))}
          </nav>
        </div>
      </header>
      <main className="container py-4"><Page /></main>
    </>
  )
}
