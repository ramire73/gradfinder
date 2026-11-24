// Shapes of the JSON sent by the backend (see ApiController).
export interface Program {
  university: string
  title: string
  url: string
  crawledAt: string
  fields: Record<string, string>
}
export interface SearchResult {
  total: number
  hits: { program: Program; score: number }[]
  suggestions: Record<string, string[]>
}
export type Counts = Record<string, number>
export type FilterOptions = Record<string, string[]>
export type Filters = Record<string, string>
export interface PageLink { title: string; url: string }
export interface IndexEntry extends PageLink { count: number }
export interface Frequency { word: string; url: string; count: number }
export interface Spelling { word: string; correct: boolean; suggestions: string[] }
export interface CrawlStatus { running: boolean; message: string; saved: number }
export interface Stats { programs: number; vocabulary: number }
export interface DatasetReport {
  programs: number
  programsPerUniversity: Counts
  filledPerColumn: Counts
  issues: { university: string; program: string; column: string; value: string; reason: string }[]
}
