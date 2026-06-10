import { useEffect, useMemo, useState } from 'react'
import { createReport, getStocks, getTodayReport } from './api'
import type { Report, ReportIndirectNewsItem, ReportNewsItem, Stock } from './types'
import './App.css'

type LoadState = 'idle' | 'loading' | 'success' | 'error'

const sentimentLabels: Record<string, string> = {
  POSITIVE: '호재',
  NEUTRAL: '중립',
  NEGATIVE: '악재',
}

const directionLabels: Record<string, string> = {
  UP: '상승 가능성',
  DOWN: '하락 가능성',
  NEUTRAL: '중립',
  MIXED: '혼재',
}

const confidenceLabels: Record<string, string> = {
  HIGH: '높음',
  MEDIUM: '보통',
  LOW: '낮음',
}

function formatDateTime(value: string) {
  if (!value) {
    return '시간 정보 없음'
  }

  return value.replace('T', ' ').slice(0, 16)
}

function formatScore(value: number | undefined) {
  if (value === undefined || Number.isNaN(value)) {
    return '-'
  }

  return value.toFixed(1)
}

function App() {
  const [stocks, setStocks] = useState<Stock[]>([])
  const [selectedStock, setSelectedStock] = useState('')
  const [report, setReport] = useState<Report | null>(null)
  const [stockState, setStockState] = useState<LoadState>('loading')
  const [reportState, setReportState] = useState<LoadState>('idle')
  const [message, setMessage] = useState('')

  const selectedStockLabel = useMemo(() => {
    const stock = stocks.find((item) => item.stockName === selectedStock)
    return stock?.displayName ?? selectedStock
  }, [selectedStock, stocks])

  useEffect(() => {
    getStocks()
      .then((items) => {
        setStocks(items)
        setSelectedStock(items[0]?.stockName ?? '')
        setStockState('success')
      })
      .catch((error: Error) => {
        setStockState('error')
        setMessage(`지원 종목을 불러오지 못했습니다. ${error.message}`)
      })
  }, [])

  const handleLoadTodayReport = async () => {
    if (!selectedStock) {
      setMessage('조회할 종목을 선택해주세요.')
      return
    }

    setReportState('loading')
    setMessage('')

    try {
      const data = await getTodayReport(selectedStock)
      setReport(data)
      setReportState('success')
    } catch (error) {
      setReportState('error')
      setMessage(
        error instanceof Error
          ? `오늘 생성된 리포트를 불러오지 못했습니다. ${error.message}`
          : '오늘 생성된 리포트를 불러오지 못했습니다.',
      )
    }
  }

  const handleCreateReport = async () => {
    if (!selectedStock) {
      setMessage('생성할 종목을 선택해주세요.')
      return
    }

    setReportState('loading')
    setMessage('뉴스 수집과 AI 분석을 실행하고 있습니다. 잠시만 기다려주세요.')

    try {
      const data = await createReport(selectedStock)
      setReport(data)
      setReportState('success')
      setMessage('리포트가 생성되었습니다.')
    } catch (error) {
      setReportState('error')
      setMessage(
        error instanceof Error
          ? `리포트 생성에 실패했습니다. ${error.message}`
          : '리포트 생성에 실패했습니다.',
      )
    }
  }

  return (
    <main className="page-shell">
      <section className="app-hero" aria-labelledby="page-title">
        <div className="container">
          <nav className="top-nav" aria-label="브리플">
            <a className="brand" href="#page-title" aria-label="BRIEFL 홈">
              <span className="brand-mark" aria-hidden="true">
                B
              </span>
              <span className="brand-copy">
                <span className="brand-name">BRIEFL</span>
                <span className="brand-subtitle">브리플</span>
              </span>
            </a>
            <span className="api-chip">Backend API Connected</span>
          </nav>

          <div className="workspace-layout">
            <section className="control-panel" aria-label="리포트 생성 컨트롤">
              <span className="eyebrow">Live News Brief</span>
              <h1 id="page-title">관심 종목 뉴스 리포트를 바로 생성합니다</h1>
              <p>
                백엔드의 지원 종목 API와 리포트 생성 API를 호출해 오늘의 뉴스
                요약, 감성 분류, 가격 영향 가능성, 체크 이벤트를 화면에
                표시합니다.
              </p>

              <div className="stock-control">
                <label htmlFor="stock-select">지원 종목</label>
                <select
                  id="stock-select"
                  value={selectedStock}
                  disabled={stockState === 'loading' || reportState === 'loading'}
                  onChange={(event) => {
                    setSelectedStock(event.target.value)
                    setReport(null)
                    setMessage('')
                    setReportState('idle')
                  }}
                >
                  {stocks.map((stock) => (
                    <option key={stock.stockName} value={stock.stockName}>
                      {stock.displayName} · {stock.market}
                    </option>
                  ))}
                </select>
              </div>

              <div className="action-row">
                <button
                  className="primary-button"
                  type="button"
                  disabled={stockState !== 'success' || reportState === 'loading'}
                  onClick={handleCreateReport}
                >
                  {reportState === 'loading' ? '분석 중...' : '오늘 리포트 생성'}
                </button>
                <button
                  className="secondary-button"
                  type="button"
                  disabled={stockState !== 'success' || reportState === 'loading'}
                  onClick={handleLoadTodayReport}
                >
                  저장된 리포트 조회
                </button>
              </div>

              <div className="status-box" data-state={reportState}>
                {stockState === 'loading'
                  ? '지원 종목을 불러오는 중입니다.'
                  : message ||
                    '종목을 선택한 뒤 오늘 리포트를 생성하거나 저장된 리포트를 조회하세요.'}
              </div>

              <p className="notice-box">
                브리플은 투자 추천이나 매수/매도 판단을 제공하지 않습니다.
                뉴스 요약과 가격 영향 가능성은 투자 판단 보조 정보입니다.
              </p>
            </section>

            <ReportDashboard
              report={report}
              selectedStockLabel={selectedStockLabel}
              isLoading={reportState === 'loading'}
            />
          </div>
        </div>
      </section>
    </main>
  )
}

function ReportDashboard({
  report,
  selectedStockLabel,
  isLoading,
}: {
  report: Report | null
  selectedStockLabel: string
  isLoading: boolean
}) {
  if (isLoading) {
    return (
      <section className="report-dashboard loading-panel" aria-live="polite">
        <div className="loading-spinner" aria-hidden="true" />
        <h2>뉴스를 수집하고 리포트를 생성하는 중입니다</h2>
        <p>Naver News 검색, AI 분석, 영향 점수 계산을 순서대로 실행합니다.</p>
      </section>
    )
  }

  if (!report) {
    return (
      <section className="report-dashboard empty-panel">
        <span className="preview-badge">No Report</span>
        <h2>{selectedStockLabel || '종목'} 리포트를 기다리고 있습니다</h2>
        <p>
          왼쪽에서 종목을 선택하고 오늘 리포트를 생성하면 실제 API 응답이 이
          영역에 표시됩니다.
        </p>
        <div className="empty-grid" aria-label="표시 예정 데이터">
          <span>요약</span>
          <span>호재/중립/악재</span>
          <span>직접 뉴스</span>
          <span>간접 뉴스</span>
          <span>체크 이벤트</span>
          <span>주의 문구</span>
        </div>
      </section>
    )
  }

  return (
    <section className="report-dashboard" aria-labelledby="report-title">
      <div className="report-header">
        <div>
          <span className="preview-badge">Report #{report.reportId ?? 'new'}</span>
          <h2 id="report-title">{report.stockName} 오늘의 브리프</h2>
          <p>{report.reportDate}</p>
        </div>
        <div className="impact-score">
          <span>뉴스 영향 점수</span>
          <strong>{formatScore(report.newsImpactScore)}</strong>
        </div>
      </div>

      <div className="summary-card">
        <span className={`sentiment-pill ${report.overallSentiment.toLowerCase()}`}>
          {sentimentLabels[report.overallSentiment] ?? report.overallSentiment}
        </span>
        <p>{report.briefSummary}</p>
      </div>

      <div className="metric-grid">
        <MetricCard label="호재" value={report.counts.positive} tone="positive" />
        <MetricCard label="중립" value={report.counts.neutral} tone="neutral" />
        <MetricCard label="악재" value={report.counts.negative} tone="negative" />
      </div>

      <section className="price-impact" aria-label="가격 영향 가능성">
        <h3>가격 영향 가능성</h3>
        <div>
          <strong>
            {directionLabels[report.priceImpact.direction] ??
              report.priceImpact.direction}
          </strong>
          <span>
            신뢰도{' '}
            {confidenceLabels[report.priceImpact.confidence] ??
              report.priceImpact.confidence}
          </span>
        </div>
        <p>{report.priceImpact.reason}</p>
      </section>

      <div className="news-columns">
        <NewsList title="직접 뉴스" items={report.directNews} />
        <NewsList title="간접 뉴스" items={report.indirectNews} />
      </div>

      <section className="events-panel" aria-label="체크 이벤트">
        <h3>체크 이벤트</h3>
        {report.checkEvents.length > 0 ? (
          <div className="event-list">
            {report.checkEvents.map((event) => (
              <article className="event-item" key={`${event.date}-${event.event}`}>
                <time>{event.date}</time>
                <div>
                  <strong>{event.event}</strong>
                  <p>{event.whyImportant}</p>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <p className="empty-copy">오늘 표시할 체크 이벤트가 없습니다.</p>
        )}
      </section>

      <p className="caution-box">{report.caution}</p>
    </section>
  )
}

function MetricCard({
  label,
  value,
  tone,
}: {
  label: string
  value: number
  tone: 'positive' | 'neutral' | 'negative'
}) {
  return (
    <div className={`metric-card ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function NewsList({
  title,
  items,
}: {
  title: string
  items: Array<ReportNewsItem | ReportIndirectNewsItem>
}) {
  return (
    <section className="news-panel" aria-label={title}>
      <h3>{title}</h3>
      {items.length > 0 ? (
        <div className="news-list">
          {items.map((item) => (
            <article className="news-item" key={`${item.title}-${item.url}`}>
              <div className="news-item-top">
                <span className={`signal-badge ${item.sentiment.toLowerCase()}`}>
                  {sentimentLabels[item.sentiment] ?? item.sentiment}
                </span>
                <span>{formatDateTime(item.publishedAt)}</span>
              </div>
              <a href={item.url} target="_blank" rel="noreferrer">
                {item.title}
              </a>
              {'relatedFactor' in item && (
                <small>연관 요인: {item.relatedFactor}</small>
              )}
              <p>{item.reason}</p>
              <div className="score-row">
                <span>영향 {formatScore(item.impactScore)}</span>
                <span>관련도 {formatScore(item.relevance)}</span>
                <span>중요도 {formatScore(item.importance)}</span>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <p className="empty-copy">표시할 뉴스가 없습니다.</p>
      )}
    </section>
  )
}

export default App
