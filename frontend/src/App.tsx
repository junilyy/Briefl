import './App.css'

function App() {
  return (
    <main className="app-shell">
      <section className="starter-card" aria-labelledby="page-title">
        <div className="brand-row">
          <div className="brand-mark" aria-hidden="true">
            B
          </div>
          <div className="brand-copy">
            <span className="brand-name">BRIEFL</span>
            <span className="brand-subtitle">AI News Brief for Investors</span>
          </div>
        </div>

        <h1 id="page-title">프론트엔드 초기 세팅 완료</h1>
        <p>
          React 기반 BRIEFL 프론트엔드 작업을 시작할 준비가 되었습니다. 다음
          이슈부터 발표 흐름, 리포트 미리보기, XYZ 가설 검증 UI를 순서대로
          구현합니다.
        </p>

        <div className="status-list" aria-label="초기 세팅 상태">
          <div className="status-item">React + TypeScript</div>
          <div className="status-item">Vite Dev Server</div>
          <div className="status-item">Frontend Only</div>
        </div>
      </section>
    </main>
  )
}

export default App
