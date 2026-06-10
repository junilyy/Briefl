import { useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

const problemCards = [
  {
    index: '01',
    title: '뉴스가 여러 곳에 흩어져 있습니다',
    description:
      '관심 종목 관련 뉴스가 포털, 경제지, 기업 기사, 산업 기사로 나뉘어 있어 필요한 흐름만 모아보기 어렵습니다.',
  },
  {
    index: '02',
    title: '제목만으로 맥락 판단이 어렵습니다',
    description:
      '기사 제목만 보고 호재인지 악재인지, 혹은 아직 해석이 필요한 중립 이슈인지 구분하는 데 시간이 들어갑니다.',
  },
  {
    index: '03',
    title: '중요한 이슈를 놓치기 쉽습니다',
    description:
      '바쁜 일정 속에서 뉴스를 뒤늦게 확인하거나, 관심 종목에 의미 있는 이벤트를 나중에 발견하는 경우가 생깁니다.',
  },
]

const solutionCards = [
  {
    index: 'A',
    title: '관심 종목별 뉴스 수집',
    description:
      '입력한 종목을 기준으로 경제 뉴스와 기업 관련 뉴스를 모아 종목별 흐름을 한 번에 볼 수 있게 정리합니다.',
  },
  {
    index: 'B',
    title: '호재 / 악재 / 중립 분류',
    description:
      '기사를 그대로 나열하지 않고, 종목에 영향을 줄 수 있는 방향성을 AI가 분류해 해석 시간을 줄입니다.',
  },
  {
    index: 'C',
    title: '이벤트와 반응 흐름 정리',
    description:
      '실적 발표, 정책 발언, 업황 이벤트 같은 변동 포인트와 뉴스 이후 시장 반응을 함께 확인할 수 있게 설계합니다.',
  },
]

const heroSignals = [
  {
    label: '관심 종목 중심',
    value: 'MY',
    description: '사용자가 보고 싶은 종목 기준으로 관련 뉴스를 우선 정리합니다.',
  },
  {
    label: '가격 영향 가능성',
    value: '+/-',
    description: '가격 예측이 아니라 뉴스가 만들 수 있는 방향성과 맥락을 표시합니다.',
  },
  {
    label: '예정 이벤트',
    value: 'D-',
    description: '실적 발표, 정책 일정, 업황 코멘트처럼 확인할 일정을 함께 보여줍니다.',
  },
]

const reportStats = [
  { label: '호재 이슈', value: '1' },
  { label: '중립 이슈', value: '1' },
  { label: '부담 요인 후보', value: '1' },
]

const reactionItems = [
  { label: '장중 반응', value: '+1.8%', tone: 'up' },
  { label: '거래대금', value: '평균 대비 1.3배', tone: 'flat' },
  { label: '외국인 수급', value: '순매수 우위', tone: 'up' },
]

const reportFeed = [
  {
    tone: 'good',
    label: '호재',
    title: '반도체 업황 회복 기대',
    description:
      '산업 수요 회복 관련 기사들이 이어지며 해당 종목에 긍정적으로 읽힐 수 있는 배경 뉴스로 정리됩니다.',
  },
  {
    tone: 'neutral',
    label: '중립',
    title: '환율 변동성 확대',
    description:
      '직접적인 영향 방향을 단정하기 어렵고 추가 해석이 필요한 거시 변수는 중립으로 표시해 구분합니다.',
  },
  {
    tone: 'bad',
    label: '악재 가능성',
    title: '경쟁사 신제품 출시',
    description:
      '경쟁 환경 변화가 예상될 때는 부정적 가능성 이슈로 표기하되 확정적인 판단처럼 보이지 않도록 맥락 중심으로 설명합니다.',
  },
]

const timelineItems = [
  {
    date: '04/28',
    title: '미국 주요 정책 발언 예정',
    description:
      '시장 전반 변동성과 반도체 섹터 심리에 영향을 줄 수 있는 매크로 이벤트 예시입니다.',
  },
  {
    date: '04/30',
    title: '글로벌 빅테크 실적 발표 집중',
    description:
      '관련 기업의 가이던스와 업황 코멘트가 섹터 뉴스 해석에 영향을 줄 수 있습니다.',
  },
  {
    date: '05/02',
    title: '반도체 장비 업계 컨퍼런스',
    description:
      '공급망과 수요 전망 관련 발언이 이어질 경우 관련 종목 브리프에 반영될 수 있습니다.',
  },
]

const hypothesisCards = [
  {
    label: 'X',
    title: '관심 종목 뉴스 정리 수요',
    description:
      '사용자가 직접 뉴스를 찾아보는 시간을 줄여주는 종목 중심 브리프가 실제 문제를 해결하는지 확인합니다.',
  },
  {
    label: 'Y',
    title: '반응과 이벤트 맥락의 가치',
    description:
      '뉴스 요약뿐 아니라 발표 이후 시장 반응과 예정 이벤트가 함께 있을 때 더 유용하게 느끼는지 검증합니다.',
  },
  {
    label: 'Z',
    title: '출시 후 이용 의향',
    description:
      '무료 또는 유료 조건에서 어떤 수준의 이용 의향이 있는지 파악해 기능 우선순위와 가격 가설을 조정합니다.',
  },
]

const initialFeedbackForm = {
  contact: '',
  ageGroup: '20대',
  newsFrequency: '매일',
  willingnessToPay: '무료면 이용',
  advice: '',
}

function App() {
  const [feedbackForm, setFeedbackForm] = useState(initialFeedbackForm)
  const [formStatus, setFormStatus] = useState('')

  const updateFeedbackField = (
    field: keyof typeof initialFeedbackForm,
    value: string,
  ) => {
    setFeedbackForm((current) => ({ ...current, [field]: value }))
    setFormStatus('')
  }

  const handleFeedbackSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (!feedbackForm.contact.trim()) {
      setFormStatus('이메일 주소를 입력해주세요.')
      return
    }

    if (!feedbackForm.advice.trim()) {
      setFormStatus('서비스 개선 의견을 입력해주세요.')
      return
    }

    setFeedbackForm(initialFeedbackForm)
    setFormStatus(
      '의견이 임시 접수되었습니다. 실제 저장 연동은 다음 단계에서 연결합니다.',
    )
  }

  return (
    <main className="page-shell">
      <section className="hero-section" aria-labelledby="hero-title">
        <div className="container">
          <nav className="top-nav" aria-label="브리플 소개">
            <a className="brand" href="#hero-title" aria-label="BRIEFL 홈">
              <span className="brand-mark" aria-hidden="true">
                B
              </span>
              <span className="brand-copy">
                <span className="brand-name">BRIEFL</span>
                <span className="brand-subtitle">브리플</span>
              </span>
            </a>
            <span className="nav-chip">AI 투자 뉴스 브리프 서비스</span>
          </nav>

          <div className="hero-layout">
            <div className="hero-copy">
              <span className="eyebrow">브리플 Preview</span>
              <h1 id="hero-title">
                내 관심 종목에 영향을 주는 뉴스만, AI가 매일 정리합니다
              </h1>
              <p>
                흩어진 경제 뉴스와 기업 뉴스를 AI가 분석해 호재, 악재,
                중립으로 정리하고 관심 종목별 요약 브리프와 주요 이벤트를
                함께 제공합니다.
              </p>

              <div className="hero-actions" aria-label="주요 이동">
                <a className="primary-button" href="#solution">
                  서비스 흐름 보기
                </a>
                <a className="secondary-button" href="#example-report">
                  예시 리포트 보기
                </a>
              </div>

              <p className="notice-box">
                브리플은 투자 추천이나 매수, 매도 판단을 제공하지 않습니다.
                뉴스 요약, 시장 이벤트 정리, 투자 판단 보조용 정보 브리프를
                목표로 합니다.
              </p>
            </div>

            <aside className="hero-preview" aria-label="브리플 요약 예시">
              <div className="preview-header">
                <span className="preview-badge">Today Brief</span>
                <span className="preview-date">미리보기</span>
              </div>
              <div className="preview-title">
                <h2>뉴스, 반응, 다음 이벤트를 한 화면에서</h2>
                <p>
                  단순 기사 모음이 아니라 오늘 읽어야 할 뉴스와 시장 반응,
                  곧 예정된 이벤트를 연결해 보여주는 경험을 지향합니다.
                </p>
              </div>

              <div className="signal-stack">
                <article className="signal-card positive">
                  <span>호재 가능성</span>
                  <strong>반도체 업황 회복 기대</strong>
                  <p>수요 회복 관련 기사를 긍정 요인으로 묶어 표시합니다.</p>
                </article>
                <article className="signal-card neutral">
                  <span>중립 관찰</span>
                  <strong>환율 변동성 확대</strong>
                  <p>방향을 단정하기 어려운 거시 변수는 별도 맥락으로 둡니다.</p>
                </article>
                <article className="signal-card caution">
                  <span>부담 요인 후보</span>
                  <strong>경쟁사 신제품 출시</strong>
                  <p>경쟁 구도 변화 가능성을 과장 없이 점검 대상으로 표시합니다.</p>
                </article>
              </div>
            </aside>
          </div>
        </div>
      </section>

      <section className="section" id="problem" aria-labelledby="problem-title">
        <div className="container">
          <div className="section-heading">
            <span className="eyebrow">Problem</span>
            <h2 id="problem-title">뉴스는 많은데, 판단할 시간은 부족합니다</h2>
            <p>
              투자 관련 뉴스를 매일 직접 확인하더라도 실제로 관심 종목에 어떤
              영향을 줄 수 있는지 빠르게 파악하기는 쉽지 않습니다.
            </p>
          </div>

          <div className="card-grid">
            {problemCards.map((card) => (
              <article className="info-card" key={card.index}>
                <span className="card-index">{card.index}</span>
                <h3>{card.title}</h3>
                <p>{card.description}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="section section-alt" id="solution" aria-labelledby="solution-title">
        <div className="container">
          <div className="section-heading">
            <span className="eyebrow">Solution</span>
            <h2 id="solution-title">
              관심 종목 기준으로 AI가 핵심 뉴스만 정리합니다
            </h2>
            <p>
              브리플은 종목 추천이 아니라 사용자가 이미 관심 있는 종목을 더
              빠르게 이해할 수 있도록 뉴스 흐름과 이벤트 맥락을 구조화합니다.
            </p>
          </div>

          <div className="solution-layout">
            <div className="card-grid solution-cards">
              {solutionCards.map((card) => (
                <article className="info-card" key={card.index}>
                  <span className="card-index">{card.index}</span>
                  <h3>{card.title}</h3>
                  <p>{card.description}</p>
                </article>
              ))}
            </div>

            <aside className="insight-panel">
              <h3>복잡한 뉴스 흐름을 읽기 쉬운 투자 브리프로</h3>
              <p>
                기사 제목을 나열하는 대신 오늘 읽어야 할 뉴스, 관찰된 반응,
                다가오는 변동 이벤트를 하나의 흐름으로 정리합니다.
              </p>
              <div className="hero-signals">
                {heroSignals.map((signal) => (
                  <div className="hero-signal" key={signal.label}>
                    <strong>{signal.value}</strong>
                    <span>{signal.label}</span>
                    <p>{signal.description}</p>
                  </div>
                ))}
              </div>
            </aside>
          </div>
        </div>
      </section>

      <section
        className="section report-section"
        id="example-report"
        aria-labelledby="report-title"
      >
        <div className="container">
          <div className="section-heading">
            <span className="eyebrow">Example Report</span>
            <h2 id="report-title">이런 형태의 리포트를 제공할 예정입니다</h2>
            <p>
              아래 화면은 실제 투자 조언이 아닌 예시 리포트입니다. 브리플이
              어떤 방식으로 뉴스, 반응, 이벤트를 함께 정리할지 보여주는
              샘플입니다.
            </p>
          </div>

          <article className="report-card" aria-label="삼성전자 브리프 예시">
            <div className="report-top">
              <div>
                <span className="preview-badge">BRIEFL Sample</span>
                <h3>삼성전자 브리프 예시</h3>
                <p>
                  경제 뉴스, 산업 뉴스, 기업 관련 이슈와 발표 이후 반응,
                  예정 이벤트를 함께 정리한 예시 화면입니다.
                </p>
              </div>
              <span className="example-label">예시 리포트입니다</span>
            </div>

            <div className="report-grid">
              <section className="report-summary" aria-labelledby="summary-title">
                <span className="report-chip">오늘의 브리프</span>
                <h4 id="summary-title">한눈에 보는 핵심 변화</h4>
                <p>
                  기사 제목만 모아두지 않고 오늘의 핵심 뉴스와 관찰된 반응,
                  곧 확인해야 할 이벤트를 한 번에 요약하는 구조를 목표로 합니다.
                </p>

                <div className="summary-stats" aria-label="뉴스 분류 요약">
                  {reportStats.map((stat) => (
                    <div className="summary-stat" key={stat.label}>
                      <strong>{stat.value}</strong>
                      <span>{stat.label}</span>
                    </div>
                  ))}
                </div>

                <div className="summary-report">
                  <strong>오늘의 요약 문장</strong>
                  <p>
                    반도체 업황 회복 기대가 긍정적으로 언급된 가운데, 환율
                    변수는 중립으로 분류되었고 경쟁사 신제품 이슈는 점검이
                    필요한 부담 요인 후보로 정리되었습니다.
                  </p>
                </div>

                <div className="reaction-strip">
                  <strong>뉴스 발표 이후 관찰된 반응</strong>
                  <div className="reaction-grid">
                    {reactionItems.map((item) => (
                      <div className="reaction-item" key={item.label}>
                        <span>{item.label}</span>
                        <b className={item.tone}>{item.value}</b>
                      </div>
                    ))}
                  </div>
                </div>
              </section>

              <section className="report-feed" aria-labelledby="feed-title">
                <h4 id="feed-title">세부 뉴스 정리</h4>
                {reportFeed.map((item) => (
                  <article className="feed-item" key={item.title}>
                    <span className={`signal-badge ${item.tone}`}>{item.label}</span>
                    <strong>{item.title}</strong>
                    <p>{item.description}</p>
                  </article>
                ))}

                <div className="event-timeline">
                  <h5>다가오는 체크 이벤트</h5>
                  <p>
                    주가 변동 가능성을 높일 수 있는 일정은 뉴스와 분리해서
                    시간순으로 보여주는 형태를 가정했습니다.
                  </p>
                  <div className="timeline-list">
                    {timelineItems.map((item) => (
                      <div className="timeline-row" key={item.date}>
                        <em>{item.date}</em>
                        <div>
                          <span>{item.title}</span>
                          <small>{item.description}</small>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </section>
            </div>
          </article>
        </div>
      </section>

      <section className="section feedback-section" id="feedback" aria-labelledby="feedback-title">
        <div className="container">
          <div className="section-heading">
            <span className="eyebrow">Service Feedback</span>
            <h2 id="feedback-title">브리플 서비스 개선 의견을 받고 있습니다</h2>
            <p>
              실제 사용자에게 더 도움이 되는 투자 뉴스 브리프가 되려면 어떤
              정보와 화면 구성이 필요한지 확인하고 있습니다. 아래 의견은
              기능 우선순위와 리포트 구성 방향을 다듬는 데 활용됩니다.
            </p>
          </div>

          <div className="feedback-layout">
            <aside className="hypothesis-panel" aria-labelledby="hypothesis-title">
              <h3 id="hypothesis-title">XYZ 가설을 검증합니다</h3>
              <p>
                브리플은 단순 관심 신청보다 구체적인 사용 맥락을 확인해
                어떤 기능부터 만들어야 할지 판단하려고 합니다.
              </p>
              <div className="hypothesis-list">
                {hypothesisCards.map((card) => (
                  <article className="hypothesis-card" key={card.label}>
                    <span>{card.label}</span>
                    <div>
                      <strong>{card.title}</strong>
                      <p>{card.description}</p>
                    </div>
                  </article>
                ))}
              </div>
            </aside>

            <form className="feedback-form" onSubmit={handleFeedbackSubmit}>
              <div className="form-grid">
                <label className="form-group form-group-full">
                  <span>이메일</span>
                  <input
                    type="email"
                    value={feedbackForm.contact}
                    placeholder="이메일 주소를 입력해주세요"
                    onChange={(event) =>
                      updateFeedbackField('contact', event.target.value)
                    }
                  />
                  <small>후속 안내와 의견 확인 목적으로만 활용됩니다.</small>
                </label>

                <label className="form-group">
                  <span>연령대</span>
                  <select
                    value={feedbackForm.ageGroup}
                    onChange={(event) =>
                      updateFeedbackField('ageGroup', event.target.value)
                    }
                  >
                    <option value="10대">10대</option>
                    <option value="20대">20대</option>
                    <option value="30대">30대</option>
                    <option value="40대">40대</option>
                    <option value="50대 이상">50대 이상</option>
                  </select>
                </label>

                <label className="form-group">
                  <span>투자 관련 뉴스 확인 빈도</span>
                  <select
                    value={feedbackForm.newsFrequency}
                    onChange={(event) =>
                      updateFeedbackField('newsFrequency', event.target.value)
                    }
                  >
                    <option value="매일">매일</option>
                    <option value="주 2~3회">주 2~3회</option>
                    <option value="가끔">가끔</option>
                    <option value="거의 안 봄">거의 안 봄</option>
                  </select>
                </label>

                <label className="form-group form-group-full">
                  <span>정식 출시 시 월 이용 의향</span>
                  <select
                    value={feedbackForm.willingnessToPay}
                    onChange={(event) =>
                      updateFeedbackField('willingnessToPay', event.target.value)
                    }
                  >
                    <option value="무료면 이용">무료면 이용</option>
                    <option value="만원대 이하">만원대 이하</option>
                    <option value="만원대">만원대</option>
                    <option value="2만원대">2만원대</option>
                    <option value="3만원 이상">3만원 이상</option>
                  </select>
                </label>

                <label className="form-group form-group-full">
                  <span>서비스 개선 의견</span>
                  <textarea
                    value={feedbackForm.advice}
                    placeholder="브리플을 보며 느낀 점이나 개선 의견을 자유롭게 남겨주세요"
                    onChange={(event) =>
                      updateFeedbackField('advice', event.target.value)
                    }
                  />
                </label>
              </div>

              <div className="submit-row">
                <p>
                  현재 화면에서는 실제 저장 없이 입력 흐름만 확인합니다. 저장
                  연동은 Google Apps Script로 별도 연결할 예정입니다.
                </p>
                <button className="primary-button" type="submit">
                  의견 제출하기
                </button>
              </div>

              {formStatus && (
                <p className="form-status" role="status">
                  {formStatus}
                </p>
              )}
            </form>
          </div>
        </div>
      </section>
    </main>
  )
}

export default App
