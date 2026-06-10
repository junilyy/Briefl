import type { Report, Stock } from './types'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? 'http://localhost:8080'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  })

  if (!response.ok) {
    let message = '요청을 처리하지 못했습니다.'

    try {
      const errorBody = (await response.json()) as { message?: string }
      message = errorBody.message ?? message
    } catch {
      message = `${response.status} ${response.statusText}`
    }

    throw new Error(message)
  }

  return response.json() as Promise<T>
}

export function getStocks() {
  return request<Stock[]>('/api/stocks')
}

export function getTodayReport(stockName: string) {
  return request<Report>(`/api/reports?stockName=${encodeURIComponent(stockName)}`)
}

export function createReport(stockName: string) {
  return request<Report>('/api/reports', {
    method: 'POST',
    body: JSON.stringify({ stockName }),
  })
}
