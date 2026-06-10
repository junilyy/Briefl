import { useEffect, useMemo, useState } from 'react'
import { createReport, getStocks, getTodayReport } from './api'
import type { Report, ReportReferencedNews, Stock } from './types'
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

const newsTypeLabels: Record<string, string> = {
  DIRECT: '직접 뉴스',
  INDIRECT: '간접 영향 뉴스',
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

function sentimentTone(value: string) {
  if (value.includes('호재') || value === 'POSITIVE') {
    return 'positive'
  }

  if (value.includes('악재') || value === 'NEGATIVE') {
    return 'negative'
  }

  return 'neutral'
}

function eventDateLabel(value: string | null) {
  return value?.trim() || '일정 미정'
}

function faviconUrl(source: string) {
  return `https://www.google.com/s2/favicons?domain=${encodeURIComponent(source)}&sz=96`
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
                요약, 종합 분석, 가격 영향 가능성, 체크 이벤트를 화면에
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
        <p>Naver News 검색과 AI 종합 분석을 순서대로 실행합니다.</p>
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
          <span>종합 분석</span>
          <span>참고 뉴스</span>
          <span>AI 체크 이벤트</span>
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
        <span className={`sentiment-pill ${sentimentTone(report.overallSentiment)}`}>
          {sentimentLabels[report.overallSentiment] ?? report.overallSentiment}
        </span>
        <p>{report.briefSummary}</p>
      </div>

      <section className="analysis-panel" aria-label="호재 중립 악재 종합 분석">
        <div className="section-title-row">
          <h3>AI 종합 분석</h3>
          <span>기사별 평가가 아닌 참고 뉴스 기반 요약</span>
        </div>
        {report.sentimentAnalyses.length > 0 ? (
          <div className="analysis-grid">
            {report.sentimentAnalyses.map((analysis) => (
              <article
                className={`analysis-card ${sentimentTone(analysis.sentiment)}`}
                key={analysis.sentiment}
              >
                <span className={`sentiment-pill ${sentimentTone(analysis.sentiment)}`}>
                  {sentimentLabels[analysis.sentiment] ?? analysis.sentiment}
                </span>
                <p>{analysis.summary}</p>
                {analysis.keyPoints.length > 0 && (
                  <ul>
                    {analysis.keyPoints.map((point) => (
                      <li key={point}>{point}</li>
                    ))}
                  </ul>
                )}
                {analysis.relatedNewsTitles.length > 0 && (
                  <div className="related-news">
                    <strong>관련 참고 뉴스 {analysis.relatedNewsTitles.length}건</strong>
                    {analysis.relatedNewsTitles.slice(0, 3).map((title) => (
                      <span key={title}>{title}</span>
                    ))}
                    {analysis.relatedNewsTitles.length > 3 && (
                      <em>외 {analysis.relatedNewsTitles.length - 3}건</em>
                    )}
                  </div>
                )}
              </article>
            ))}
          </div>
        ) : (
          <p className="empty-copy">표시할 종합 분석이 없습니다.</p>
        )}
      </section>

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

      <ReferencedNewsList items={report.referencedNews} />

      <section className="events-panel" aria-label="체크 이벤트">
        <div className="section-title-row dark">
          <h3>AI 체크 이벤트</h3>
          <span>참고 뉴스 맥락에서 도출</span>
        </div>
        {report.checkEvents.length > 0 ? (
          <div className="event-list">
            {report.checkEvents.map((event) => (
              <article className="event-item" key={`${event.date}-${event.event}`}>
                <time>{eventDateLabel(event.date)}</time>
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

function ReferencedNewsList({ items }: { items: ReportReferencedNews[] }) {
  return (
    <section className="news-panel" aria-label="참고 뉴스">
      <div className="section-title-row">
        <h3>참고 뉴스</h3>
        <span>{items.length}건</span>
      </div>
      {items.length > 0 ? (
        <div className="news-list">
          {items.map((item) => (
            <article className="news-item" key={`${item.title}-${item.url}`}>
              <div className="news-thumb" aria-hidden="true">
                <img src={faviconUrl(item.source)} alt="" loading="lazy" />
              </div>
              <div className="news-item-top">
                <span className="signal-badge neutral">
                  {newsTypeLabels[item.newsType] ?? item.newsType}
                </span>
                <span>{formatDateTime(item.publishedAt)}</span>
              </div>
              <a href={item.url} target="_blank" rel="noreferrer">
                {item.title}
              </a>
              <small>{item.source}</small>
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
