type Params = Record<string, string | number | boolean>

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch('/api' + path, options)
  } catch {
    throw new Error('Cannot reach the server. Is the backend running?')
  }
  const body = await response.json().catch(() => null)
  if (!response.ok) throw new Error(body?.error || 'The server is not responding. Is the backend running?')
  return body as T
}

export function get<T>(path: string, params: Params = {}) {
  const query = new URLSearchParams(Object.entries(params).map(([k, v]) => [k, String(v)]))
  return request<T>(`${path}?${query}`)
}

export function post<T>(path: string, data: unknown) {
  return request<T>(path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })
}

export const errorMessage = (e: unknown) => (e instanceof Error ? e.message : String(e))
