import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent, RefObject } from 'react'
import { createReport, getStocks } from './api'
import { submitReportFeedback, submitVisitorLog } from './api/feedbackApi'
import type { Report, ReportSentimentAnalysis, Stock } from './types'
import './App.css'

type LoadState = 'idle' | 'loading' | 'success' | 'error'

type FeedbackState = {
  lossAvoidanceHelp: string
  mostUseful: string[]
  willingnessToPay: string
  expectedFeature: string
  email: string
  comment: string
}

type FeedbackModalMode = 'reportFeedback' | 'moreReports'

const initialFeedback: FeedbackState = {
  lossAvoidanceHelp: '',
  mostUseful: [],
  willingnessToPay: '',
  expectedFeature: '',
  email: '',
  comment: '',
}

const loadingSteps = [
  '관련 뉴스를 수집하고 있습니다',
  '호재·악재 가능성을 분석하고 있습니다',
  '가격 영향 변수를 정리하고 있습니다',
]

const usefulOptions = ['뉴스 요약', '호재/악재 분류', '가격 영향 가능성', '간접 이슈 분석', '체크 이벤트', '판단 근거']
const willingnessOptions = ['무료면 이용', '1,000~3,000원', '5,000원 내외', '10,000원 이상', '이용 의향 없음']
const expectedFeatureOptions = [
  '과거 주가 그래프와 주요 뉴스 이벤트 연결',
  '뉴스 신뢰도 표시',
  '금리·환율·전쟁 등 매크로 영향 분석 강화',
  '실시간으로 큰 이슈가 터졌을 때 알림',
]

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
  const [guideModalOpen, setGuideModalOpen] = useState(false)
  const [guideModalMode, setGuideModalMode] = useState<FeedbackModalMode>('reportFeedback')
  const reportRef = useRef<HTMLElement | null>(null)
  const visitorLoggedRef = useRef(false)

  useEffect(() => {
    getStocks()
      .then((items) => {
        setStocks(items)
        setStockState('success')
      })
      .catch((error: Error) => {
        setStockState('error')
        setMessage(`지원 종목을 불러오지 못했습니다. ${error.message}`)
      })
  }, [])

  useEffect(() => {
    if (visitorLoggedRef.current) {
      return
    }

    visitorLoggedRef.current = true
    submitVisitorLog().catch((error) => {
      console.warn('방문자 로그 전송에 실패했습니다.', error)
    })
  }, [])

  const selectedStock = useMemo(() => resolveStock(stockInput, stocks), [stockInput, stocks])
  const selectedStockLabel = stockLabel(selectedStock, stockInput)

  const scrollToReport = () => {
    window.setTimeout(() => {
      reportRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 100)
  }

  const handleHeroExperience = () => {
    setMessage('아래 리포트 생성 영역에서 관심 종목을 확인해보세요.')
    scrollToReport()
  }

  const openMoreReportsModal = () => {
    setGuideModalMode('moreReports')
    setGuideModalOpen(true)
  }

  const handleCreateReport = async () => {
    if (stockState !== 'success') {
      setMessage('지원 종목을 불러오는 중입니다.')
      return
    }

    if (!selectedStock) {
      setLimitVisible(true)
      setMessage('체험 가능한 종목이 아닙니다. 종목, 횟수 제한 없이 이용하고 싶다면 서비스를 신청해주세요.')
      openMoreReportsModal()
      return
    }

    if (generatedStockName && generatedStockName !== selectedStock.stockName) {
      setLimitVisible(true)
      openMoreReportsModal()
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
    <>
      <LaunchIntro />
      <main className="page-shell">
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
          guidePanelVisible={Boolean(report) && !limitVisible}
          onGuideClick={() => {
            setGuideModalMode('reportFeedback')
            setGuideModalOpen(true)
          }}
        />
        <FeedbackModal
          open={guideModalOpen}
          mode={guideModalMode}
          onClose={() => setGuideModalOpen(false)}
          report={report}
          selectedStockLabel={selectedStockLabel}
        />
      </main>
    </>
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
      <div className="hero-final-layout">
        <div className="hero-copy">
          <h1 id="page-title">
            <span>내 주식 떨어지고 나서야</span>
            <span>악재 뉴스를 찾고 있나요?</span>
          </h1>
          <p>
            떨어지고 난 뒤에 뉴스를 찾지 않도록, AI가 관심 종목의 악재 신호와 가격 영향 포인트를
            1분 만에 정리해드립니다.
          </p>
          <HeroExperienceForm
            stockInput={stockInput}
            reportState={reportState}
            onStockInput={onStockInput}
            onExperience={onExperience}
          />
        </div>

        <HeroPreviewCard />
      </div>
    </section>
  )
}

function LaunchIntro() {
  return (
    <div className="launch-intro" aria-hidden="true">
      <div className="launch-grid" />
      <div className="launch-scan" />
      <div className="launch-copy">
        <span className="launch-line launch-line-one">더 이상 손해보기 싫다면</span>
        <span className="launch-line launch-line-two">한눈에 원하는 주식 정보를 찾고 싶다면</span>
        <span className="launch-brand">BRIEFL</span>
      </div>
    </div>
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
      <div className="preview-report-head">
        <div>
          <span>AI NEWS BRIEF</span>
          <strong>삼성전자 오늘의 리포트</strong>
          <small>LIVE PREVIEW</small>
        </div>
        <div className="preview-judgement-card">
          <span>종합 판단</span>
          <div>
            <strong>악재</strong>
            <em>-0.7</em>
          </div>
          <div className="preview-risk-meter" aria-label="뉴스 영향 점수 범위">
            <span>-1.0</span>
            <div>
              <i />
            </div>
            <span>+1.0</span>
          </div>
        </div>
      </div>

      <div className="preview-main-grid">
        <section className="preview-summary-card">
          <span>뉴스 요약</span>
          <p>경쟁사 신제품과 환율 변동성이 함께 부각되며 단기 부담 요인이 먼저 확인됩니다.</p>
        </section>
        <section className="preview-impact-card">
          <span>가격 영향 변수</span>
          <strong>악재 요인 우세</strong>
          <p>실적 발표 전 수요 전망과 경쟁사 가격 전략을 함께 봐야 합니다.</p>
        </section>
      </div>

      <div className="preview-news-list" aria-label="핵심 뉴스 신호">
        <div className="preview-block-title">
          <strong>핵심 뉴스</strong>
          <span>3건</span>
        </div>
        <div className="news-line negative">
          <span>악재</span>
          <strong>경쟁사 신제품 출시, 점유율 압박 가능성</strong>
        </div>
        <div className="news-line neutral">
          <span>중립</span>
          <strong>환율 변동성 확대, 실적 영향 추가 확인</strong>
        </div>
        <div className="news-line positive">
          <span>호재</span>
          <strong>메모리 업황 회복 기대는 하단 방어 요인</strong>
        </div>
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
        <span>체험 가능 종목</span>
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
        <h2 id="problem-title">
          <span>문제는 뉴스가 많은 게 아니라,</span>
          <span>중요한 신호를 늦게 아는 것입니다.</span>
        </h2>
      </div>
      <div className="target-grid">
        <article>
          <span>초보 투자자</span>
          <h3>뉴스를 봐도 의미 해석이 어려운 분</h3>
          <p>
            기사 제목만으로는 호재인지 악재인지 판단하기 어렵습니다. BRIEFL은 뉴스의 방향성과
            근거를 먼저 정리합니다.
          </p>
        </article>
        <article>
          <span>관심 종목 다수 보유자</span>
          <h3>악재를 뒤늦게 확인해본 경험이 있는 분</h3>
          <p>
            관심 종목이 많을수록 중요한 이슈를 놓치기 쉽습니다. BRIEFL은 놓치기 쉬운 악재
            신호와 간접 이슈까지 함께 보여줍니다.
          </p>
        </article>
      </div>
      <p className="problem-punchline">뉴스를 더 많이 보게 하는 것이 아니라, 놓치면 손해가 될 신호를 먼저 정리합니다.</p>
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
      title: '가격 영향 변수 정리',
      copy: '실적, 정책, 산업, 수급 중 어떤 요인을 확인해야 하는지 정리합니다.',
    },
  ]

  return (
    <section className="feature-section" aria-labelledby="feature-title">
      <div className="section-heading">
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
  guidePanelVisible,
  onGuideClick,
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
  guidePanelVisible: boolean
  onGuideClick: () => void
}) {
  return (
    <section className="generator-section" id="report-generator" ref={refTarget}>
      <div className="generator-copy">
        <h2>대표 종목으로 BRIEFL 리포트를 테스트해보세요.</h2>
        <p>
          삼성전자, NAVER, 카카오, Tesla, NVIDIA 중 하나를 선택해 관심 종목 뉴스 분석 리포트가
          악재 신호와 가격 영향 변수를 어떻게 정리하는지 확인해보세요.
        </p>
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
      {message && (
        <p className={`generator-status ${message.includes('체험 가능한 종목') ? 'error' : ''}`}>
          {message}
        </p>
      )}
      <ServiceGuidePanel
        visible={guidePanelVisible}
        mode="afterReport"
        onGuideClick={onGuideClick}
      />
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
        <p>종합 판단, 핵심 뉴스, 가격 영향 변수, 판단 근거, 체크 포인트를 카드형으로 보여드립니다.</p>
      </div>
    )
  }

  const topNews = report.referencedNews.slice(0, 5)

  return (
    <article className="report-result report-output" aria-label="AI 리포트 결과">
      <div className="report-header-row">
        <div>
          <span>AI NEWS BRIEF</span>
          <h3>{report.stockName} 오늘의 리포트</h3>
          <p>{report.reportDate}</p>
        </div>
        <div className={`judgement-badge ${impactTone(report.newsImpactScore)}`}>
          <span>종합 판단</span>
          <div className="judgement-score-row">
            <strong>{impactLabel(report.newsImpactScore)}</strong>
            <em>{formatScore(report.newsImpactScore)}</em>
          </div>
          <div
            className="impact-meter-wrap"
            tabIndex={0}
            aria-label="뉴스 영향 점수 기준: -0.2 이하는 악재, -0.2 초과부터 0.2 미만은 중립, 0.2 이상은 호재입니다."
          >
            <div className="impact-meter" aria-hidden="true">
              <i style={{ left: scorePosition(report.newsImpactScore) }} />
            </div>
            <div className="impact-meter-labels" aria-hidden="true">
              <span>-1.0</span>
              <span>+1.0</span>
            </div>
            <div className="impact-meter-tooltip" aria-hidden="true">
              <span className="negative">악재 -1.0 ~ -0.2</span>
              <span className="neutral">중립 -0.2 ~ +0.2</span>
              <span className="positive">호재 +0.2 ~ +1.0</span>
            </div>
          </div>
        </div>
      </div>

      <div className="report-main-grid">
        <section className="report-summary-card">
          <span>뉴스 요약</span>
          <p>{report.briefSummary}</p>
        </section>
        <section className={`impact-card ${sentimentTone(report.priceImpact.direction)}`}>
          <span>가격 영향 변수</span>
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
          <h4>체크할 포인트</h4>
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

function ServiceGuidePanel({
  visible,
  mode,
  onGuideClick,
}: {
  visible: boolean
  mode: 'limit' | 'afterReport'
  onGuideClick: () => void
}) {
  if (!visible) {
    return null
  }

  const isAfterReport = mode === 'afterReport'

  return (
    <section className="limit-panel" role="region" aria-labelledby="guide-title">
      <div className="limit-copy">
        <span className="limit-kicker">서비스 안내 신청</span>
        <h2 id="guide-title">
          {isAfterReport ? '횟수와 종목 제한 없이 이용해보고 싶다면' : '다른 종목도 제한 없이 분석해보고 싶다면'}
        </h2>
        <p>
          {isAfterReport
            ? '피드백과 이메일을 남겨주시면 더 많은 종목을 제한 없이 확인할 수 있는 서비스 이용 절차를 안내드릴게요.'
            : '10초 피드백과 이메일을 남기면 횟수와 종목 제한 없이 이용 가능한 서비스 안내를 받아볼 수 있습니다.'}
        </p>
      </div>
      <div className="limit-action-card">
        <strong className="limit-free-benefit">지금 신청하면 3개월 무료</strong>
        <button className="primary-button" type="button" onClick={onGuideClick}>
          신청하러가기
        </button>
      </div>
    </section>
  )
}

function FeedbackModal({
  open,
  mode,
  onClose,
  report,
  selectedStockLabel,
}: {
  open: boolean
  mode: FeedbackModalMode
  onClose: () => void
  report: Report | null
  selectedStockLabel: string
}) {
  const [feedback, setFeedback] = useState<FeedbackState>(initialFeedback)
  const [submitState, setSubmitState] = useState<LoadState>('idle')

  const updateSingle = (key: keyof FeedbackState, value: string) => {
    setFeedback((current) => ({ ...current, [key]: value }))
  }

  const toggleMulti = (key: 'mostUseful', value: string) => {
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
        mostUseful: feedback.mostUseful,
        willingnessToPay: feedback.willingnessToPay,
        expectedFeature: feedback.expectedFeature,
        comment: feedback.comment,
      })
      setSubmitState('success')
    } catch (error) {
      console.error(error)
      setSubmitState('error')
    }
  }

  if (!open) {
    return null
  }

  const isMoreReportsMode = mode === 'moreReports'

  return (
    <div className="feedback-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="feedback-section"
        role="dialog"
        aria-modal="true"
        aria-labelledby="feedback-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="feedback-modal-header">
          <div>
            <h2 id="feedback-title">
              {isMoreReportsMode
                ? '추가 리포트 안내를 받아보세요'
                : '방금 본 AI 브리프가 실제로 도움이 되었나요?'}
            </h2>
            {isMoreReportsMode ? (
              <div className="feedback-modal-copy">
                <p>짧은 피드백과 이메일을 남겨주시면 횟수와 종목 제한 없이 이용 가능한 서비스 절차를 안내드릴게요.</p>
                <small>지금 신청하면 3개월 무료 혜택을 먼저 안내드립니다.</small>
              </div>
            ) : (
              <p>짧게 선택해주시면 다음 버전 개선에 반영하겠습니다.</p>
            )}
          </div>
          <button type="button" className="modal-close-button" onClick={onClose} aria-label="닫기">
            닫기
          </button>
        </div>

        <form className="feedback-form" onSubmit={handleSubmit}>
          <FeedbackChoice
            title="이 브리프가 중요한 뉴스를 놓치지 않는 데 도움이 될 것 같나요?"
            options={['매우 도움됨', '도움됨', '보통', '도움 안 됨']}
            selected={feedback.lossAvoidanceHelp}
            onSelect={(value) => updateSingle('lossAvoidanceHelp', value)}
          />
          <FeedbackChoice
            title="가장 유용했던 부분은 무엇인가요?"
            options={usefulOptions}
            selectedValues={feedback.mostUseful}
            onToggle={(value) => toggleMulti('mostUseful', value)}
          />
          <FeedbackChoice
            title="분석이 더 정교해진다면 월 얼마까지 이용할 의향이 있나요?"
            options={willingnessOptions}
            selected={feedback.willingnessToPay}
            onSelect={(value) => updateSingle('willingnessToPay', value)}
          />
          <FeedbackChoice
            title="다음 버전에서 가장 기대되는 기능은 무엇인가요?"
            options={expectedFeatureOptions}
            selected={feedback.expectedFeature}
            onSelect={(value) => updateSingle('expectedFeature', value)}
          />

          <div className="feedback-fields">
            <label>
              <span className="feedback-label-row">
                추가 의견이 있다면 남겨주세요.
                <em>선택</em>
              </span>
              <textarea
                value={feedback.comment}
                placeholder="예: 도움이 된 부분, 더 보완되면 좋을 부분 등"
                onChange={(event) => updateSingle('comment', event.target.value)}
              />
            </label>
            <label className="feedback-email-field">
              <span className="feedback-label-row">
                서비스 이용 안내를 받을 이메일
                <em>선택</em>
              </span>
              <input
                type="email"
                value={feedback.email}
                placeholder="이메일 주소를 입력해주세요"
                onChange={(event) => updateSingle('email', event.target.value)}
              />
            </label>
          </div>

          <div className="feedback-submit-row">
            <button className="primary-button" type="submit" disabled={submitState === 'loading'}>
              {submitState === 'loading' ? '제출 중...' : '피드백 제출 및 서비스 신청하기'}
            </button>
            {submitState === 'success' && (
              <p>의견이 저장되었습니다. 추가 리포트와 서비스 안내에 참고하겠습니다.</p>
            )}
            {submitState === 'error' && <p>피드백 저장 중 문제가 발생했습니다.</p>}
          </div>
        </form>
      </section>
    </div>
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
