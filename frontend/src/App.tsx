import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent, RefObject } from 'react'
import { createReport, getStocks } from './api'
import { submitReportFeedback } from './api/feedbackApi'
import type { Report, ReportSentimentAnalysis, Stock } from './types'
import './App.css'

type LoadState = 'idle' | 'loading' | 'success' | 'error'

type FeedbackState = {
  helpful: string
  mostUseful: string[]
  missing: string[]
  reuseIntent: string
  email: string
  comment: string
}

const initialFeedback: FeedbackState = {
  helpful: '',
  mostUseful: [],
  missing: [],
  reuseIntent: '',
  email: '',
  comment: '',
}

const loadingSteps = [
  '관련 뉴스를 수집하고 있습니다',
  '호재·악재 가능성을 분석하고 있습니다',
  '가격 영향 포인트를 정리하고 있습니다',
]

const usefulOptions = ['뉴스 요약', '호재·악재 분류', '가격 영향 가능성', '판단 근거', '체크 포인트']
const missingOptions = ['뉴스가 부족함', '근거가 약함', '판단이 애매함', '화면이 복잡함', '신뢰하기 어려움']

const sentimentLabels: Record<string, string> = {
  POSITIVE: '호재',
  NEUTRAL: '중립',
  NEGATIVE: '악재',
  '악재 가능성': '악재',
  '부담 요인': '악재',
}

const newsTypeLabels: Record<string, string> = {
  DIRECT: '핵심 뉴스',
  INDIRECT: '관련 이슈',
}

function stockLabel(stock: Stock | undefined, fallback = '') {
  if (!stock) {
    return fallback
  }

  return stock.displayName || stock.stockName
}

function formatDateTime(value: string) {
  if (!value) {
    return '시간 정보 없음'
  }

  return value.replace('T', ' ').slice(0, 16)
}

function normalizeSentiment(value: string) {
  return sentimentLabels[value] ?? value
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
    return '중립'
  }

  if (value >= 0.2) {
    return '호재'
  }

  if (value <= -0.2) {
    return '악재'
  }

  return '중립'
}

function formatScore(value: number | undefined) {
  if (value === undefined || Number.isNaN(value)) {
    return '-'
  }

  return value > 0 ? `+${value.toFixed(1)}` : value.toFixed(1)
}

function scorePosition(value: number | undefined) {
  if (value === undefined || Number.isNaN(value)) {
    return '50%'
  }

  const clamped = Math.max(-1, Math.min(1, value))
  return `${((clamped + 1) / 2) * 100}%`
}

function eventDateLabel(value: string | null) {
  return value?.trim() || '일정 미정'
}

function App() {
  const [stocks, setStocks] = useState<Stock[]>([])
  const [stockInput, setStockInput] = useState('')
  const [report, setReport] = useState<Report | null>(null)
  const [generatedStockName, setGeneratedStockName] = useState('')
  const [stockState, setStockState] = useState<LoadState>('loading')
  const [reportState, setReportState] = useState<LoadState>('idle')
  const [message, setMessage] = useState('')
  const [limitVisible, setLimitVisible] = useState(false)
  const reportRef = useRef<HTMLElement | null>(null)
  const feedbackRef = useRef<HTMLElement | null>(null)

  useEffect(() => {
    getStocks()
      .then((items) => {
        setStocks(items)
        setStockInput(stockLabel(items[0], ''))
        setStockState('success')
      })
      .catch((error: Error) => {
        setStockState('error')
        setMessage(`지원 종목을 불러오지 못했습니다. ${error.message}`)
      })
  }, [])

  const selectedStock = useMemo(() => resolveStock(stockInput, stocks), [stockInput, stocks])
  const selectedStockLabel = stockLabel(selectedStock, stockInput)

  const scrollToReport = () => {
    window.setTimeout(() => {
      reportRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 100)
  }

  const scrollToFeedback = () => {
    window.requestAnimationFrame(() => {
      window.setTimeout(() => {
        feedbackRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }, 180)
    })
  }

  const handleHeroExperience = () => {
    setMessage('아래 리포트 생성 영역에서 관심 종목을 확인해보세요.')
    scrollToReport()
  }

  const handleCreateReport = async () => {
    if (stockState !== 'success') {
      setMessage('지원 종목을 불러오는 중입니다.')
      return
    }

    if (!selectedStock) {
      setLimitVisible(true)
      setMessage('아직 지원하지 않는 종목입니다. 받고 싶은 종목을 피드백으로 남겨주세요.')
      scrollToFeedback()
      return
    }

    if (generatedStockName && generatedStockName !== selectedStock.stockName) {
      setLimitVisible(true)
      scrollToFeedback()
      return
    }

    setLimitVisible(false)
    setReportState('loading')
    setMessage('AI가 오늘의 뉴스 방향을 분석하고 있습니다.')
    scrollToReport()

    try {
      const data = await createReport(selectedStock.stockName)
      setReport(data)
      setGeneratedStockName(selectedStock.stockName)
      setReportState('success')
      setMessage('무료 리포트가 생성되었습니다.')
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
      <TopNav />
      <HeroSection
        stockInput={stockInput}
        reportState={reportState}
        onStockInput={setStockInput}
        onExperience={handleHeroExperience}
      />
      <ProblemSection />
      <FeatureSection />
      <ReportGenerator
        refTarget={reportRef}
        report={report}
        reportState={reportState}
        selectedStockLabel={selectedStockLabel}
        stocks={stocks}
        stockInput={stockInput}
        stockState={stockState}
        message={message}
        onStockInput={setStockInput}
        onCreate={handleCreateReport}
      />
      <LimitModal visible={limitVisible} onBetaClick={scrollToFeedback} />
      <FeedbackSection
        refTarget={feedbackRef}
        report={report}
        selectedStockLabel={selectedStockLabel}
        forceVisible={limitVisible}
      />
    </main>
  )
}

function resolveStock(value: string, stocks: Stock[]) {
  const normalized = value.trim().toLowerCase()
  if (!normalized) {
    return undefined
  }

  return stocks.find((stock) => {
    const names = [stock.stockName, stock.displayName].map((item) => item.toLowerCase())
    return names.includes(normalized)
  })
}

function TopNav() {
  return (
    <nav className="top-nav" aria-label="브리플">
      <a className="brand" href="#page-title" aria-label="BRIEFL 홈">
        <span className="brand-mark" aria-hidden="true">
          <span />
        </span>
        <span className="brand-copy">
          <span className="brand-name">BRIEFL</span>
          <span className="brand-subtitle">브리플</span>
        </span>
      </a>
      <span className="nav-status">AI News Risk Radar</span>
    </nav>
  )
}

function HeroSection({
  stockInput,
  reportState,
  onStockInput,
  onExperience,
}: {
  stockInput: string
  reportState: LoadState
  onStockInput: (value: string) => void
  onExperience: () => void
}) {
  return (
    <section className="hero-section" aria-labelledby="page-title">
      <div className="hero-copy">
        <span className="eyebrow">AI 기반 관심 주식 뉴스 분석</span>
        <h1 id="page-title">
          <span>내 주식 떨어지고 나서야</span>
          <span>악재 뉴스를 찾고 있나요?</span>
        </h1>
        <p>
          관심 종목을 입력하면 오늘 나온 뉴스를 AI가 모아 호재·악재·중립 가능성과 가격 영향
          포인트를 1분 만에 정리해드립니다.
        </p>
        <HeroExperienceForm
          stockInput={stockInput}
          reportState={reportState}
          onStockInput={onStockInput}
          onExperience={onExperience}
        />
      </div>

      <HeroPreviewCard />
    </section>
  )
}

function HeroExperienceForm({
  stockInput,
  reportState,
  onStockInput,
  onExperience,
}: {
  stockInput: string
  reportState: LoadState
  onStockInput: (value: string) => void
  onExperience: () => void
}) {
  return (
    <div className="stock-search hero-entry">
      <label htmlFor="hero-stock-input">관심 종목</label>
      <div className="stock-search-row">
        <input
          id="hero-stock-input"
          value={stockInput}
          placeholder="예: 삼성전자"
          onChange={(event) => onStockInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              onExperience()
            }
          }}
        />
        <button
          className="primary-button"
          type="button"
          disabled={reportState === 'loading'}
          onClick={onExperience}
        >
          AI 리포트 체험하기
        </button>
      </div>
    </div>
  )
}

function HeroPreviewCard() {
  return (
    <aside className="preview-card" aria-label="AI 리포트 미리보기">
      <div className="preview-top">
        <span>LIVE BRIEF PREVIEW</span>
        <strong>삼성전자</strong>
      </div>

      <div className="risk-headline">
        <div>
          <span>오늘의 종합 판단</span>
          <strong>악재 신호 우세</strong>
        </div>
        <b>72</b>
      </div>

      <div className="signal-summary" aria-label="뉴스 신호 요약">
        <div>
          <span className="signal-dot negative" />
          <strong>2</strong>
          <small>악재</small>
        </div>
        <div>
          <span className="signal-dot neutral" />
          <strong>1</strong>
          <small>중립</small>
        </div>
        <div>
          <span className="signal-dot positive" />
          <strong>1</strong>
          <small>호재</small>
        </div>
      </div>

      <div className="preview-news-list" aria-label="핵심 뉴스 신호">
        <div className="news-line negative">
          <span>악재</span>
          <strong>경쟁사 신제품 출시로 점유율 압박 가능성</strong>
        </div>
        <div className="news-line neutral">
          <span>중립</span>
          <strong>환율 변동성 확대, 실적 영향은 추가 확인 필요</strong>
        </div>
        <div className="news-line positive">
          <span>호재</span>
          <strong>메모리 업황 회복 기대는 하단 방어 요인</strong>
        </div>
      </div>

      <div className="preview-check">
        <span>투자자가 바로 볼 것</span>
        <strong>실적 발표 전 수요 전망과 경쟁사 가격 전략</strong>
      </div>
    </aside>
  )
}

function StockSearchForm({
  idPrefix,
  stocks,
  stockInput,
  stockState,
  reportState,
  onStockInput,
  onCreate,
  buttonLabel,
}: {
  idPrefix: string
  stocks: Stock[]
  stockInput: string
  stockState: LoadState
  reportState: LoadState
  onStockInput: (value: string) => void
  onCreate: () => void
  buttonLabel: string
}) {
  const disabled = stockState !== 'success' || reportState === 'loading'

  return (
    <div className="stock-search">
      <label htmlFor={`${idPrefix}-stock-input`}>관심 종목</label>
      <div className="stock-search-row">
        <input
          id={`${idPrefix}-stock-input`}
          list={`${idPrefix}-stock-options`}
          value={stockInput}
          placeholder="예: 삼성전자"
          onChange={(event) => onStockInput(event.target.value)}
        />
        <datalist id={`${idPrefix}-stock-options`}>
          {stocks.map((stock) => (
            <option value={stockLabel(stock)} key={stock.stockName} />
          ))}
        </datalist>
        <button className="primary-button" type="button" disabled={disabled} onClick={onCreate}>
          {reportState === 'loading' ? '분석 중...' : buttonLabel}
        </button>
      </div>
      <div className="quick-stock-row" aria-label="빠른 종목 선택">
        {stocks.slice(0, 5).map((stock) => (
          <button type="button" key={stock.stockName} onClick={() => onStockInput(stockLabel(stock))}>
            {stockLabel(stock)}
          </button>
        ))}
      </div>
    </div>
  )
}

function ProblemSection() {
  return (
    <section className="problem-section" aria-labelledby="problem-title">
      <div className="section-heading">
        <span className="eyebrow">Problem</span>
        <h2 id="problem-title">뉴스를 봐도 어렵고, 안 보면 늦습니다.</h2>
      </div>
      <div className="target-grid">
        <article>
          <span>초보 투자자</span>
          <h3>뉴스를 봐도 해석이 어려운 분</h3>
          <p>
            뉴스 제목은 봤는데 이게 호재인지 악재인지 판단하기 어렵다면, AI가 핵심 내용과
            방향성을 먼저 정리해드립니다.
          </p>
        </article>
        <article>
          <span>경험 투자자</span>
          <h3>관심 종목이 많아 뉴스를 다 챙기기 어려운 분</h3>
          <p>
            이미 투자 중이어도 모든 뉴스와 이슈를 매일 따라가기는 어렵습니다. 종목별 주요
            이슈와 체크 포인트만 빠르게 보여드립니다.
          </p>
        </article>
      </div>
      <p className="problem-punchline">초보자에게는 해석을, 경험자에게는 시간을 줄여드립니다.</p>
    </section>
  )
}

function FeatureSection() {
  const features = [
    {
      title: '종목별 뉴스 수집',
      copy: '흩어진 뉴스를 관심 종목 기준으로 모읍니다.',
    },
    {
      title: '호재·악재·중립 분류',
      copy: '뉴스가 어떤 방향의 이슈인지 먼저 구분합니다.',
    },
    {
      title: '가격 영향 포인트 정리',
      copy: '실적, 정책, 산업, 수급 중 무엇을 봐야 하는지 알려드립니다.',
    },
  ]

  return (
    <section className="feature-section" aria-labelledby="feature-title">
      <div className="section-heading">
        <span className="eyebrow">Features</span>
        <h2 id="feature-title">뉴스 요약이 아니라, 판단할 포인트를 정리합니다.</h2>
      </div>
      <div className="feature-grid">
        {features.map((feature, index) => (
          <article key={feature.title}>
            <span>{String(index + 1).padStart(2, '0')}</span>
            <h3>{feature.title}</h3>
            <p>{feature.copy}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

function ReportGenerator({
  refTarget,
  report,
  reportState,
  selectedStockLabel,
  stocks,
  stockInput,
  stockState,
  message,
  onStockInput,
  onCreate,
}: {
  refTarget: RefObject<HTMLElement | null>
  report: Report | null
  reportState: LoadState
  selectedStockLabel: string
  stocks: Stock[]
  stockInput: string
  stockState: LoadState
  message: string
  onStockInput: (value: string) => void
  onCreate: () => void
}) {
  return (
    <section className="generator-section" id="report-generator" ref={refTarget}>
      <div className="generator-copy">
        <span className="eyebrow">Free AI Report</span>
        <h2>무료 리포트 1개를 바로 생성해보세요.</h2>
        <p>관심 종목을 입력하면 오늘 나온 뉴스 기준으로 핵심 판단 카드를 만듭니다.</p>
      </div>
      <StockSearchForm
        idPrefix="generator"
        stocks={stocks}
        stockInput={stockInput}
        stockState={stockState}
        reportState={reportState}
        onStockInput={onStockInput}
        onCreate={onCreate}
        buttonLabel="무료 리포트 생성하기"
      />
      {message && <p className="generator-status">{message}</p>}
      <ReportResult report={report} reportState={reportState} selectedStockLabel={selectedStockLabel} />
    </section>
  )
}

function ReportResult({
  report,
  reportState,
  selectedStockLabel,
}: {
  report: Report | null
  reportState: LoadState
  selectedStockLabel: string
}) {
  if (reportState === 'loading') {
    return (
      <div className="report-result loading-result" aria-live="polite">
        {loadingSteps.map((step, index) => (
          <div className="loading-row" key={step}>
            <span>{index + 1}</span>
            <strong>{step}</strong>
          </div>
        ))}
      </div>
    )
  }

  if (!report) {
    return (
      <div className="report-result empty-result">
        <span>REPORT OUTPUT</span>
        <h3>{selectedStockLabel || '관심 종목'} 리포트가 여기에 표시됩니다.</h3>
        <p>종합 판단, 핵심 뉴스, 뉴스 요약, 판단 근거, 체크 포인트를 카드형으로 보여드립니다.</p>
      </div>
    )
  }

  const topNews = report.referencedNews.slice(0, 5)

  return (
    <article className="report-result" aria-label="AI 리포트 결과">
      <div className="report-header-row">
        <div>
          <span>AI NEWS BRIEF</span>
          <h3>{report.stockName} 오늘의 리포트</h3>
          <p>{report.reportDate}</p>
        </div>
        <div className={`judgement-badge ${impactTone(report.newsImpactScore)}`}>
          <span>종합 판단</span>
          <strong>{impactLabel(report.newsImpactScore)}</strong>
          <em>{formatScore(report.newsImpactScore)}</em>
          <div className="impact-meter" aria-hidden="true">
            <i style={{ left: scorePosition(report.newsImpactScore) }} />
          </div>
        </div>
      </div>

      <div className="report-main-grid">
        <section className="report-summary-card">
          <span>뉴스 요약</span>
          <p>{report.briefSummary}</p>
        </section>
        <section className={`impact-card ${sentimentTone(report.priceImpact.direction)}`}>
          <span>가격 영향 포인트</span>
          <strong>{report.priceImpact.direction}</strong>
          <p>{report.priceImpact.reason}</p>
        </section>
      </div>

      <section className="report-block">
        <div className="block-title-row">
          <h4>핵심 뉴스 목록</h4>
          <span>{topNews.length}건</span>
        </div>
        <div className="core-news-list">
          {topNews.map((news) => (
            <a href={news.url} target="_blank" rel="noreferrer" key={`${news.title}-${news.url}`}>
              <span>{newsTypeLabels[news.newsType] ?? news.newsType}</span>
              <strong>{news.title}</strong>
              <small>{news.source} · {formatDateTime(news.publishedAt)}</small>
            </a>
          ))}
        </div>
      </section>

      <section className="report-block">
        <div className="block-title-row">
          <h4>호재·악재·중립 판단 근거</h4>
        </div>
        <div className="reason-grid">
          {report.sentimentAnalyses.map((analysis) => (
            <AnalysisReasonCard analysis={analysis} key={analysis.sentiment} />
          ))}
        </div>
      </section>

      <section className="report-block">
        <div className="block-title-row">
          <h4>투자자가 체크할 포인트</h4>
          <span>{report.checkEvents.length || 'AI 정리'}</span>
        </div>
        <div className="check-list">
          {report.checkEvents.length > 0 ? (
            report.checkEvents.map((event) => (
              <div key={`${event.date}-${event.event}`}>
                <time>{eventDateLabel(event.date)}</time>
                <strong>{event.event}</strong>
                <p>{event.whyImportant}</p>
              </div>
            ))
          ) : (
            <p>오늘 표시할 체크 포인트가 없습니다.</p>
          )}
        </div>
      </section>

      <p className="report-disclaimer">본 리포트는 뉴스 기반 정보 정리이며, 투자 판단은 본인의 책임입니다.</p>
    </article>
  )
}

function AnalysisReasonCard({ analysis }: { analysis: ReportSentimentAnalysis }) {
  return (
    <article className={`reason-card ${sentimentTone(analysis.sentiment)}`}>
      <span>{normalizeSentiment(analysis.sentiment)}</span>
      <p>{analysis.summary}</p>
      {analysis.keyPoints.length > 0 && (
        <ul>
          {analysis.keyPoints.map((point) => (
            <li key={point}>{point}</li>
          ))}
        </ul>
      )}
    </article>
  )
}

function LimitModal({ visible, onBetaClick }: { visible: boolean; onBetaClick: () => void }) {
  if (!visible) {
    return null
  }

  return (
    <section className="limit-panel" role="alert" aria-labelledby="limit-title">
      <div>
        <span className="eyebrow">Limit</span>
        <h2 id="limit-title">다른 종목도 확인하고 싶으신가요?</h2>
        <p>
          현재 무료 리포트는 IP당 1회 제공됩니다. 더 많은 종목과 매일 업데이트 리포트를
          사용해보고 싶다면 이메일을 남겨주세요.
        </p>
      </div>
      <button className="primary-button" type="button" onClick={onBetaClick}>
        베타 테스트 신청하기
      </button>
    </section>
  )
}

function FeedbackSection({
  refTarget,
  report,
  selectedStockLabel,
  forceVisible,
}: {
  refTarget: RefObject<HTMLElement | null>
  report: Report | null
  selectedStockLabel: string
  forceVisible: boolean
}) {
  const [feedback, setFeedback] = useState<FeedbackState>(initialFeedback)
  const [submitState, setSubmitState] = useState<LoadState>('idle')

  const visible = Boolean(report) || forceVisible

  const updateSingle = (key: keyof FeedbackState, value: string) => {
    setFeedback((current) => ({ ...current, [key]: value }))
  }

  const toggleMulti = (key: 'mostUseful' | 'missing', value: string) => {
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
        lossAvoidanceHelp: feedback.helpful,
        informationGapReduced: '',
        trustScore: '',
        mostUseful: feedback.mostUseful,
        painPoints: feedback.missing,
        reuseIntent: feedback.reuseIntent,
        willingnessToPay: '',
        comment: feedback.comment,
      })
      setSubmitState('success')
    } catch (error) {
      console.error(error)
      setSubmitState('error')
    }
  }

  return (
    <section className={`feedback-section ${visible ? '' : 'is-muted'}`} ref={refTarget} aria-labelledby="feedback-title">
      <div className="section-heading">
        <span className="eyebrow">Beta Feedback</span>
        <h2 id="feedback-title">방금 받은 AI 브리프, 실제로 도움이 되었나요?</h2>
        <p>10초만 선택해주세요. 이후 버전 개선에 반영됩니다.</p>
      </div>

      <form className="feedback-form" onSubmit={handleSubmit}>
        <FeedbackChoice
          title="Q1. 리포트가 도움이 되었나요?"
          options={['매우 도움됨', '도움됨', '보통', '도움 안 됨']}
          selected={feedback.helpful}
          onSelect={(value) => updateSingle('helpful', value)}
        />
        <FeedbackChoice
          title="Q2. 가장 유용했던 부분은 무엇인가요?"
          options={usefulOptions}
          selectedValues={feedback.mostUseful}
          onToggle={(value) => toggleMulti('mostUseful', value)}
        />
        <FeedbackChoice
          title="Q3. 부족했던 부분은 무엇인가요?"
          options={missingOptions}
          selectedValues={feedback.missing}
          onToggle={(value) => toggleMulti('missing', value)}
        />
        <FeedbackChoice
          title="Q4. 다시 사용해보고 싶나요?"
          options={['매일 받고 싶음', '이슈 있을 때만', '가끔 확인', '잘 모르겠음']}
          selected={feedback.reuseIntent}
          onSelect={(value) => updateSingle('reuseIntent', value)}
        />

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
            자유 의견
            <textarea
              value={feedback.comment}
              placeholder="어떤 정보가 있으면 돈을 내고 쓸 만한지, 어떤 분석이 신뢰되는지 남겨주세요."
              onChange={(event) => updateSingle('comment', event.target.value)}
            />
          </label>
        </div>

        <div className="feedback-submit-row">
          <button className="primary-button" type="submit" disabled={submitState === 'loading'}>
            {submitState === 'loading' ? '제출 중...' : '피드백 제출하고 베타 신청하기'}
          </button>
          {submitState === 'success' && <p>피드백이 저장되었습니다.</p>}
          {submitState === 'error' && <p>피드백 저장 중 문제가 발생했습니다.</p>}
        </div>
      </form>
    </section>
  )
}

function FeedbackChoice({
  title,
  options,
  selected,
  selectedValues,
  onSelect,
  onToggle,
}: {
  title: string
  options: string[]
  selected?: string
  selectedValues?: string[]
  onSelect?: (value: string) => void
  onToggle?: (value: string) => void
}) {
  return (
    <fieldset className="feedback-group">
      <legend>{title}</legend>
      <div className="choice-row">
        {options.map((option) => {
          const active = selected === option || Boolean(selectedValues?.includes(option))

          return (
            <button
              className="choice-button"
              type="button"
              aria-pressed={active}
              key={option}
              onClick={() => {
                if (onToggle) {
                  onToggle(option)
                  return
                }
                onSelect?.(option)
              }}
            >
              {option}
            </button>
          )
        })}
      </div>
    </fieldset>
  )
}

export default App
