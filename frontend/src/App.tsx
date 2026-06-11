import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent, MutableRefObject } from 'react'
import { createReport, getStocks, getTodayReport } from './api'
import { submitReportFeedback } from './api/feedbackApi'
import type { Report, ReportReferencedNews, ReportSentimentAnalysis, Stock } from './types'
import './App.css'

type LoadState = 'idle' | 'loading' | 'success' | 'error'

type FeedbackState = {
  lossAvoidanceHelp: string
  informationGapReduced: string
  trustScore: string
  mostUseful: string[]
  painPoints: string[]
  reuseIntent: string
  willingnessToPay: string
  email: string
  comment: string
}

const initialFeedback: FeedbackState = {
  lossAvoidanceHelp: '',
  informationGapReduced: '',
  trustScore: '',
  mostUseful: [],
  painPoints: [],
  reuseIntent: '',
  willingnessToPay: '',
  email: '',
  comment: '',
}

const sentimentLabels: Record<string, string> = {
  POSITIVE: '호재',
  NEUTRAL: '중립',
  NEGATIVE: '악재 가능성',
  '악재 가능성': '부담 요인',
  '부담 요인': '부담 요인',
}

const directionLabels: Record<string, string> = {
  UP: '상승 요인 우세',
  DOWN: '하락 리스크 존재',
  NEUTRAL: '방향 불명확',
  MIXED: '변동성 주의',
  중립: '방향 불명확',
  혼조: '변동성 주의',
  '판단 어려움': '방향 불명확',
  '상승 요인 우세': '긍정 요인 우세',
  '하락 리스크 존재': '부담 요인 우세',
}

const confidenceLabels: Record<string, string> = {
  HIGH: '높음',
  MEDIUM: '중간',
  LOW: '낮음',
}

const newsTypeLabels: Record<string, string> = {
  DIRECT: '직접 뉴스',
  INDIRECT: '간접 영향 뉴스',
}

const analysisSteps = [
  '직접 뉴스 수집',
  '간접 변수 확인',
  '이벤트 후보 검색',
  '방향성 판단',
  '체크 포인트 정리',
]

const whyCards = [
  {
    label: '손실 회피',
    title: '중요한 이슈를 놓치면 손실로 이어질 수 있습니다',
    copy:
      '실적 발표, 금리, 환율, 정책 변화처럼 종목에 영향을 줄 수 있는 뉴스는 늦게 확인할수록 판단 여지가 줄어듭니다.',
  },
  {
    label: '정보 격차',
    title: '같은 뉴스도 해석 속도가 다릅니다',
    copy:
      '뉴스를 많이 보는 것보다 중요한 건 내 종목과 연결되는지 빠르게 구분하는 것입니다.',
  },
  {
    label: '판단 근거',
    title: '남의 말보다 내 판단 근거가 필요합니다',
    copy:
      '브리플은 참고 뉴스, 영향 요인, 체크 이벤트를 한 번에 정리해 오늘 무엇을 먼저 봐야 할지 보여줍니다.',
  },
]

const singleChoiceQuestions = [
  {
    id: 'lossAvoidanceHelp' as const,
    question: '이 브리프가 중요한 뉴스를 놓치지 않는 데 도움이 될 것 같나요?',
    options: ['매우 도움됨', '도움됨', '보통', '도움 안 됨'],
  },
  {
    id: 'informationGapReduced' as const,
    question: '남들보다 늦게 뉴스를 해석한다는 불안감을 줄여줄 것 같나요?',
    options: ['많이 줄어듦', '어느 정도 줄어듦', '잘 모르겠음', '줄어들지 않음'],
  },
  {
    id: 'trustScore' as const,
    question: '호재·중립·악재 판단 근거를 신뢰할 수 있었나요?',
    options: ['신뢰됨', '어느 정도 신뢰됨', '판단 어려움', '신뢰 어려움'],
  },
  {
    id: 'reuseIntent' as const,
    question: '이 브리프를 다시 사용해보고 싶나요?',
    options: ['매일 받아보고 싶음', '관심 종목 이슈가 있을 때만', '가끔 확인하고 싶음', '다시 쓸지는 모르겠음'],
  },
  {
    id: 'willingnessToPay' as const,
    question: '이 정도 분석이 더 정교해진다면 월 얼마까지 이용할 의향이 있나요?',
    options: ['무료면 이용', '1,000~3,000원', '5,000원 내외', '10,000원 이상', '이용 의향 없음'],
  },
]

const usefulOptions = ['뉴스 요약', '호재/악재 분류', '가격 영향 가능성', '간접 이슈 분석', '체크 이벤트', '판단 근거']
const painPointOptions = ['뉴스가 부족함', '분석 근거가 약함', '가격 영향 판단이 애매함', '간접 이슈 연결이 부족함', '화면이 복잡함', '신뢰하기 어려움']

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

  return value > 0 ? `+${value.toFixed(1)}` : value.toFixed(1)
}

function impactTone(value: number | undefined) {
  if (value === undefined || Number.isNaN(value)) {
    return 'neutral'
  }

  if (value >= 0.2) {
    return 'positive'
  }

  if (value <= -0.2) {
    return 'negative'
  }

  return 'neutral'
}

function impactLabel(value: number | undefined) {
  if (value === undefined || Number.isNaN(value)) {
    return '방향 정보 없음'
  }

  if (value >= 0.6) {
    return '긍정 요인 우세'
  }

  if (value >= 0.2) {
    return '긍정 요인 약간 우세'
  }

  if (value <= -0.6) {
    return '부담 요인 우세'
  }

  if (value <= -0.2) {
    return '부담 요인 약간 우세'
  }

  return '방향 불명확'
}

function scorePosition(value: number | undefined) {
  if (value === undefined || Number.isNaN(value)) {
    return '50%'
  }

  const clamped = Math.max(-1, Math.min(1, value))
  return `${((clamped + 1) / 2) * 100}%`
}

function sentimentTone(value: string) {
  if (value.includes('호재') || value.includes('상승') || value.includes('긍정') || value === 'POSITIVE') {
    return 'positive'
  }

  if (
    value.includes('악재') ||
    value.includes('하락') ||
    value.includes('부담') ||
    value === 'NEGATIVE'
  ) {
    return 'negative'
  }

  return 'neutral'
}

function eventDateLabel(value: string | null) {
  return value?.trim() || '일정 미정'
}

function normalizeSentiment(value: string) {
  return sentimentLabels[value] ?? value
}

function stockLabel(stock: Stock | undefined, fallback = '') {
  if (!stock) {
    return fallback
  }

  return stock.displayName || stock.stockName
}

function App() {
  const [stocks, setStocks] = useState<Stock[]>([])
  const [selectedStock, setSelectedStock] = useState('')
  const [report, setReport] = useState<Report | null>(null)
  const [stockState, setStockState] = useState<LoadState>('loading')
  const [reportState, setReportState] = useState<LoadState>('idle')
  const [message, setMessage] = useState('')
  const reportRef = useRef<HTMLElement | null>(null)

  const selectedStockItem = useMemo(
    () => stocks.find((item) => item.stockName === selectedStock),
    [selectedStock, stocks],
  )

  const selectedStockLabel = stockLabel(selectedStockItem, selectedStock)

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

  const scrollToReport = () => {
    window.setTimeout(() => {
      reportRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 120)
  }

  const handleLoadTodayReport = async () => {
    if (!selectedStock) {
      setMessage('조회할 종목을 선택해주세요.')
      return
    }

    setReportState('loading')
    setMessage('저장된 리포트를 확인하고 있습니다.')
    scrollToReport()

    try {
      const data = await getTodayReport(selectedStock)
      setReport(data)
      setReportState('success')
      setMessage('저장된 리포트를 불러왔습니다.')
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
    setMessage('직접 뉴스와 간접 이슈를 함께 분석하고 있습니다.')
    scrollToReport()

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
        <span className="api-chip">Live News Brief</span>
      </nav>

      <section className="hero-section" aria-labelledby="page-title">
        <div className="hero-copy">
          <span className="eyebrow">AI Stock News Brief</span>
          <h1 id="page-title">오늘 내 종목에 좋은 뉴스인지, 부담 뉴스인지 바로 확인하세요.</h1>
          <p>
            BRIEFL은 관심 종목 뉴스, 산업 변수, 예정 이벤트를 한 번에 묶어 오늘 볼 핵심 판단만 먼저 보여줍니다.
          </p>
          <div className="hero-points" aria-label="브리플 핵심 기능">
            <span>뉴스 방향</span>
            <span>가격 영향</span>
            <span>체크 이벤트</span>
          </div>
          <div className="hero-actions">
            <button
              className="primary-button"
              type="button"
              disabled={stockState !== 'success' || reportState === 'loading'}
              onClick={handleCreateReport}
            >
              오늘의 AI 브리프 확인하기
            </button>
            <a className="ghost-link" href="#why-it-matters">
              왜 필요한지 보기
            </a>
          </div>
        </div>

        <StockConsole
          stocks={stocks}
          selectedStock={selectedStock}
          selectedStockLabel={selectedStockLabel}
          stockState={stockState}
          reportState={reportState}
          message={message}
          onSelect={(value) => {
            setSelectedStock(value)
            setReport(null)
            setMessage('')
            setReportState('idle')
          }}
          onCreate={handleCreateReport}
          onLoad={handleLoadTodayReport}
        />
      </section>

      <ReportDashboard
        refTarget={reportRef}
        report={report}
        selectedStockLabel={selectedStockLabel}
        isLoading={reportState === 'loading'}
      />

      <FeedbackForm report={report} selectedStockLabel={selectedStockLabel} />

      <section className="why-section" id="why-it-matters" aria-labelledby="why-title">
        <div className="section-heading">
          <span className="eyebrow">Why It Matters</span>
          <h2 id="why-title">투자 뉴스에서 중요한 건 많이 보는 것이 아니라, 놓치지 않는 것입니다.</h2>
        </div>
        <div className="why-grid">
          {whyCards.map((card) => (
            <article className="why-card" key={card.title}>
              <span>{card.label}</span>
              <h3>{card.title}</h3>
              <p>{card.copy}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  )
}

function StockConsole({
  stocks,
  selectedStock,
  selectedStockLabel,
  stockState,
  reportState,
  message,
  onSelect,
  onCreate,
  onLoad,
}: {
  stocks: Stock[]
  selectedStock: string
  selectedStockLabel: string
  stockState: LoadState
  reportState: LoadState
  message: string
  onSelect: (value: string) => void
  onCreate: () => void
  onLoad: () => void
}) {
  const disabled = stockState !== 'success' || reportState === 'loading'

  return (
    <aside className="stock-console" aria-label="브리프 생성">
      <div className="console-header">
        <span>Step 1. 종목 선택</span>
        <strong>{selectedStockLabel || '종목 선택'}</strong>
      </div>

      <div className="stock-button-grid" role="listbox" aria-label="지원 종목">
        {stocks.map((stock) => {
          const active = stock.stockName === selectedStock

          return (
            <button
              className="stock-pill"
              type="button"
              key={stock.stockName}
              aria-pressed={active}
              disabled={reportState === 'loading'}
              onClick={() => onSelect(stock.stockName)}
            >
              <span>{stockLabel(stock)}</span>
              <small>{stock.market}</small>
            </button>
          )
        })}
      </div>

      <p className="console-help">뉴스를 새로 분석하거나, 오늘 이미 만든 리포트를 다시 불러올 수 있습니다.</p>

      <div className="console-actions">
        <button className="primary-button" type="button" disabled={disabled} onClick={onCreate}>
          {reportState === 'loading' ? '분석 중...' : '오늘의 AI 브리프 생성하기'}
        </button>
        <button className="secondary-button" type="button" disabled={disabled} onClick={onLoad}>
          저장된 리포트 조회
        </button>
      </div>

      <div className="status-box" data-state={reportState}>
        {stockState === 'loading'
          ? '지원 종목을 불러오는 중입니다.'
          : message || '종목을 선택하면 실제 백엔드 API를 호출해 오늘의 브리프를 생성합니다.'}
      </div>
    </aside>
  )
}

function ReportDashboard({
  refTarget,
  report,
  selectedStockLabel,
  isLoading,
}: {
  refTarget: MutableRefObject<HTMLElement | null>
  report: Report | null
  selectedStockLabel: string
  isLoading: boolean
}) {
  if (isLoading) {
    return (
      <section className="report-shell loading-panel" ref={refTarget} aria-live="polite">
        <span className="eyebrow">AI Brief Loading</span>
        <h2>오늘 볼 뉴스 방향을 정리하고 있습니다.</h2>
        <div className="loading-track">
          {analysisSteps.map((step, index) => (
            <div className="loading-step" key={step}>
              <span>{index + 1}</span>
              <strong>{step}</strong>
            </div>
          ))}
        </div>
      </section>
    )
  }

  if (!report) {
    return (
      <section className="report-shell empty-panel" ref={refTarget}>
        <span className="eyebrow">Step 2. 리포트 확인</span>
        <h2>{selectedStockLabel || '관심 종목'}의 오늘 판단 카드가 이곳에 표시됩니다.</h2>
        <p>
          생성 버튼을 누르면 오늘 뉴스 방향, 가격 영향, 체크 이벤트, 참고 뉴스가 한 화면에 정리됩니다.
        </p>
      </section>
    )
  }

  return (
    <section className="report-shell" ref={refTarget} aria-labelledby="report-title">
      <div className="report-hero">
        <div>
          <span className="eyebrow">Report #{report.reportId ?? 'new'}</span>
          <h2 id="report-title">{report.stockName} 오늘의 AI 브리프</h2>
          <p>{report.reportDate}</p>
        </div>
      </div>

      <div className="decision-board">
        <div className={`impact-score ${impactTone(report.newsImpactScore)}`}>
          <span>오늘 뉴스 방향</span>
          <strong>{impactLabel(report.newsImpactScore)}</strong>
          <em>{formatScore(report.newsImpactScore)}</em>
          <div className="impact-meter" aria-hidden="true">
            <i style={{ left: scorePosition(report.newsImpactScore) }} />
          </div>
        </div>

        <div className={`summary-strip ${sentimentTone(report.overallSentiment)}`}>
          <span className={`sentiment-pill ${sentimentTone(report.overallSentiment)}`}>
            {normalizeSentiment(report.overallSentiment)}
          </span>
          <strong>핵심 판단</strong>
          <p>{report.briefSummary}</p>
        </div>

        <section
          className={`price-impact ${sentimentTone(report.priceImpact.direction)}`}
          aria-label="가격 영향 가능성"
        >
          <span className="section-kicker">가격 영향 가능성</span>
          <strong>
            {directionLabels[report.priceImpact.direction] ?? report.priceImpact.direction}
          </strong>
          <span className="confidence-chip">
            신뢰도 {confidenceLabels[report.priceImpact.confidence] ?? report.priceImpact.confidence}
          </span>
          <p>{report.priceImpact.reason}</p>
        </section>
      </div>

      <section className="analysis-panel" aria-label="AI 종합 분석">
        <div className="section-title-row">
          <h3>AI 종합 분석</h3>
          <span>호재 · 중립 · 부담 요인</span>
        </div>
        {report.sentimentAnalyses.length > 0 ? (
          <div className="analysis-grid">
            {report.sentimentAnalyses.map((analysis) => (
              <SentimentAnalysisCard analysis={analysis} key={analysis.sentiment} />
            ))}
          </div>
        ) : (
          <p className="empty-copy">표시할 종합 분석이 없습니다.</p>
        )}
      </section>

      <ReferencedNewsList items={report.referencedNews} />

      <section className="events-panel" aria-label="AI 체크 이벤트">
        <div className="section-title-row dark">
          <h3>AI 체크 이벤트</h3>
          <span>이벤트 후보 검색 반영</span>
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

      {report.caution?.trim() && <p className="caution-box">{report.caution}</p>}
    </section>
  )
}

function SentimentAnalysisCard({ analysis }: { analysis: ReportSentimentAnalysis }) {
  return (
    <article className={`analysis-card ${sentimentTone(analysis.sentiment)}`}>
      <span className={`sentiment-pill ${sentimentTone(analysis.sentiment)}`}>
        {normalizeSentiment(analysis.sentiment)}
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
          {analysis.relatedNewsTitles.slice(0, 2).map((title) => (
            <span key={title}>{title}</span>
          ))}
          {analysis.relatedNewsTitles.length > 2 && (
            <em>외 {analysis.relatedNewsTitles.length - 2}건</em>
          )}
        </div>
      )}
    </article>
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

function FeedbackForm({
  report,
  selectedStockLabel,
}: {
  report: Report | null
  selectedStockLabel: string
}) {
  const [feedback, setFeedback] = useState<FeedbackState>(initialFeedback)
  const [submitState, setSubmitState] = useState<LoadState>('idle')

  if (!report) {
    return null
  }

  const updateSingle = (key: keyof FeedbackState, value: string) => {
    setFeedback((current) => ({ ...current, [key]: value }))
  }

  const toggleMulti = (key: 'mostUseful' | 'painPoints', value: string) => {
    setFeedback((current) => {
      const values = current[key]
      const nextValues = values.includes(value)
        ? values.filter((item) => item !== value)
        : [...values, value]

      return { ...current, [key]: nextValues }
    })
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSubmitState('loading')

    try {
      await submitReportFeedback({
        email: feedback.email,
        stockName: report?.stockName ?? selectedStockLabel,
        reportId: report?.reportId ?? null,
        reportDate: report?.reportDate ?? '',
        lossAvoidanceHelp: feedback.lossAvoidanceHelp,
        informationGapReduced: feedback.informationGapReduced,
        trustScore: feedback.trustScore,
        mostUseful: feedback.mostUseful,
        painPoints: feedback.painPoints,
        reuseIntent: feedback.reuseIntent,
        willingnessToPay: feedback.willingnessToPay,
        comment: feedback.comment,
      })
      setSubmitState('success')
    } catch (error) {
      console.error(error)
      setSubmitState('error')
    }
  }

  return (
    <section className="feedback-section" aria-labelledby="feedback-title">
      <div className="section-heading compact">
        <span className="eyebrow">Feedback</span>
        <h2 id="feedback-title">방금 본 AI 브리프가 실제로 도움이 되었나요?</h2>
        <p>10초만에 선택해주세요. 이후 버전 개선에 반영됩니다.</p>
      </div>

      <form className="feedback-form" onSubmit={handleSubmit}>
        {singleChoiceQuestions.map((item) => (
          <fieldset className="feedback-group" key={item.id}>
            <legend>{item.question}</legend>
            <div className="choice-row">
              {item.options.map((option) => (
                <button
                  className="choice-button"
                  type="button"
                  aria-pressed={feedback[item.id] === option}
                  key={option}
                  onClick={() => updateSingle(item.id, option)}
                >
                  {option}
                </button>
              ))}
            </div>
          </fieldset>
        ))}

        <fieldset className="feedback-group">
          <legend>어떤 부분이 가장 유용했나요?</legend>
          <div className="choice-row">
            {usefulOptions.map((option) => (
              <button
                className="choice-button"
                type="button"
                aria-pressed={feedback.mostUseful.includes(option)}
                key={option}
                onClick={() => toggleMulti('mostUseful', option)}
              >
                {option}
              </button>
            ))}
          </div>
        </fieldset>

        <fieldset className="feedback-group">
          <legend>어떤 부분이 부족했나요?</legend>
          <div className="choice-row">
            {painPointOptions.map((option) => (
              <button
                className="choice-button"
                type="button"
                aria-pressed={feedback.painPoints.includes(option)}
                key={option}
                onClick={() => toggleMulti('painPoints', option)}
              >
                {option}
              </button>
            ))}
          </div>
        </fieldset>

        <div className="feedback-fields">
          <label>
            후속 테스트 안내를 받을 이메일을 남겨주세요.
            <input
              type="email"
              value={feedback.email}
              placeholder="email@example.com"
              onChange={(event) => updateSingle('email', event.target.value)}
            />
          </label>
          <label>
            추가로 남기고 싶은 의견이 있다면 적어주세요.
            <textarea
              value={feedback.comment}
              placeholder="어떤 정보가 더 있으면 돈을 낼 만할지, 어떤 분석이 더 신뢰될지 등을 남겨주세요."
              onChange={(event) => updateSingle('comment', event.target.value)}
            />
          </label>
        </div>

        <div className="feedback-submit-row">
          <button className="primary-button" type="submit" disabled={submitState === 'loading'}>
            {submitState === 'loading' ? '저장 중...' : '피드백 제출하기'}
          </button>
          {submitState === 'success' && (
            <p>의견이 저장되었습니다. 다음 버전에서는 신뢰도와 가격 영향 근거를 더 강화해보겠습니다.</p>
          )}
          {submitState === 'error' && <p>피드백 저장 중 문제가 발생했습니다.</p>}
        </div>
      </form>
    </section>
  )
}

export default App
