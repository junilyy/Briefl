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

function App() {
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
                <a className="secondary-button" href="#problem">
                  문제 확인하기
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
    </main>
  )
}

export default App
