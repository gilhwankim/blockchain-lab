import { useEffect, useMemo, useState } from 'react';

const STEPS = [
  {
    key: 'initial',
    label: '초기 상태',
    title: 'Genesis 기준으로 tx가 mempool에 대기 중',
    description: 'Alice와 Dave가 받은 UTXO를 사용하려는 transaction이 아직 block에 들어가기 전 상태다.',
    canonical: ['Genesis'],
    branchA: ['A1 후보'],
    branchB: ['B1 후보', 'B2 후보'],
    mempool: ['Alice -> Bob 60', 'Dave -> Erin 45'],
    restored: [],
    rejected: [],
    balances: { Alice: 100, Bob: 0, Carol: 0, Dave: 50, Erin: 0, Miner: 0 },
    focus: 'mempool',
  },
  {
    key: 'branch-a',
    label: 'Branch A 확정',
    title: 'Branch A가 canonical chain이 되며 tx가 mempool에서 제거',
    description: 'block A1에 들어간 transaction은 confirmed로 취급되어 mempool에서 빠진다.',
    canonical: ['Genesis', 'A1'],
    branchA: ['A1: Bob +60, Erin +45'],
    branchB: ['B1 후보', 'B2 후보'],
    mempool: [],
    restored: [],
    rejected: [],
    balances: { Alice: 40, Bob: 60, Carol: 0, Dave: 0, Erin: 45, Miner: 0 },
    focus: 'confirmed',
  },
  {
    key: 'strong-branch',
    label: '강한 Branch B 등장',
    title: 'Branch B의 누적 work가 더 커지며 reorg 준비',
    description: 'B1과 B2가 들어오면서 기존 A1보다 더 강한 branch가 된다. 이제 canonical chain 교체가 필요하다.',
    canonical: ['Genesis', 'A1'],
    branchA: ['A1: 기존 canonical'],
    branchB: ['B1: Carol +70', 'B2: 추가 work'],
    mempool: [],
    restored: [],
    rejected: [],
    balances: { Alice: 40, Bob: 60, Carol: 0, Dave: 0, Erin: 45, Miner: 0 },
    focus: 'reorg',
  },
  {
    key: 'replayed',
    label: 'Reorg 적용',
    title: '새 canonical chain 기준으로 UTXO와 mempool을 다시 정리',
    description: 'Alice의 Bob tx는 B branch와 충돌해 버려지고, Dave의 Erin tx는 여전히 유효해서 mempool로 복구된다.',
    canonical: ['Genesis', 'B1', 'B2'],
    branchA: ['A1: removed'],
    branchB: ['B1: Carol +70', 'B2: Miner +10'],
    mempool: ['Dave -> Erin 45'],
    restored: ['Dave -> Erin 45'],
    rejected: ['Alice -> Bob 60'],
    balances: { Alice: 30, Bob: 0, Carol: 70, Dave: 50, Erin: 0, Miner: 10 },
    focus: 'replayed',
  },
];

const FLOW = [
  '새 canonical chain 선택',
  '빈 UTXO Set 생성',
  'Genesis부터 Tip까지 재생',
  'mempool 재검증',
];

function App() {
  const [stepIndex, setStepIndex] = useState(0);
  const [autoPlay, setAutoPlay] = useState(false);
  const step = STEPS[stepIndex];

  useEffect(() => {
    if (!autoPlay) {
      return undefined;
    }

    const timer = window.setInterval(() => {
      setStepIndex((current) => (current + 1) % STEPS.length);
    }, 2600);

    return () => window.clearInterval(timer);
  }, [autoPlay]);

  const progress = useMemo(() => ((stepIndex + 1) / STEPS.length) * 100, [stepIndex]);

  const goNext = () => setStepIndex((current) => Math.min(current + 1, STEPS.length - 1));
  const goPrev = () => setStepIndex((current) => Math.max(current - 1, 0));
  const reset = () => {
    setAutoPlay(false);
    setStepIndex(0);
  };

  return (
    <main className="app-shell">
      <section className="topbar">
        <div>
          <p className="eyebrow">Stage 08 Simulation</p>
          <h1>Reorg와 Mempool 복구</h1>
        </div>
        <div className="step-meter" aria-label="진행률">
          <span>{stepIndex + 1}</span>
          <div className="meter-track">
            <div className="meter-fill" style={{ width: `${progress}%` }} />
          </div>
          <span>{STEPS.length}</span>
        </div>
      </section>

      <section className="control-strip">
        <div className="segmented" role="tablist" aria-label="시뮬레이션 단계">
          {STEPS.map((item, index) => (
            <button
              key={item.key}
              className={index === stepIndex ? 'active' : ''}
              type="button"
              onClick={() => setStepIndex(index)}
            >
              {item.label}
            </button>
          ))}
        </div>
        <div className="actions">
          <button type="button" className="ghost" onClick={goPrev} disabled={stepIndex === 0}>
            이전
          </button>
          <button type="button" className="primary" onClick={goNext} disabled={stepIndex === STEPS.length - 1}>
            다음
          </button>
          <button type="button" className={autoPlay ? 'toggle on' : 'toggle'} onClick={() => setAutoPlay((value) => !value)}>
            자동
          </button>
          <button type="button" className="ghost" onClick={reset}>
            초기화
          </button>
        </div>
      </section>

      <section className="stage-summary">
        <div>
          <p className="stage-label">{step.label}</p>
          <h2>{step.title}</h2>
          <p>{step.description}</p>
        </div>
        <div className={`state-chip ${step.focus}`}>{focusLabel(step.focus)}</div>
      </section>

      <section className="workspace">
        <ChainPanel title="Canonical Chain" blocks={step.canonical} accent="canonical" />
        <ForkPanel step={step} />
        <MempoolPanel step={step} />
        <BalancePanel balances={step.balances} />
      </section>

      <section className="flow-panel">
        <div className="flow-header">
          <p className="eyebrow">Reorg 처리 순서</p>
          <strong>block 목록과 ledger state, mempool을 함께 맞춘다</strong>
        </div>
        <div className="flow-steps">
          {FLOW.map((item, index) => (
            <div key={item} className={index <= stepIndex ? 'flow-step active' : 'flow-step'}>
              <span>{index + 1}</span>
              <p>{item}</p>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}

function ChainPanel({ title, blocks, accent }) {
  return (
    <article className={`panel chain-panel ${accent}`}>
      <header>
        <p className="panel-kicker">chain view</p>
        <h3>{title}</h3>
      </header>
      <div className="block-row">
        {blocks.map((block, index) => (
          <div key={`${block}-${index}`} className="block-card">
            <span className="block-index">#{index}</span>
            <strong>{block}</strong>
          </div>
        ))}
      </div>
    </article>
  );
}

function ForkPanel({ step }) {
  return (
    <article className="panel fork-panel">
      <header>
        <p className="panel-kicker">fork choice</p>
        <h3>Branch 비교</h3>
      </header>
      <div className="fork-lanes">
        <ForkLane label="Branch A" blocks={step.branchA} tone={step.focus === 'replayed' ? 'removed' : 'warm'} />
        <ForkLane label="Branch B" blocks={step.branchB} tone={step.focus === 'reorg' || step.focus === 'replayed' ? 'strong' : 'cool'} />
      </div>
    </article>
  );
}

function ForkLane({ label, blocks, tone }) {
  return (
    <div className={`fork-lane ${tone}`}>
      <span>{label}</span>
      <div>
        {blocks.map((block) => (
          <b key={block}>{block}</b>
        ))}
      </div>
    </div>
  );
}

function MempoolPanel({ step }) {
  return (
    <article className="panel mempool-panel">
      <header>
        <p className="panel-kicker">pending tx</p>
        <h3>Mempool</h3>
      </header>
      <div className="mempool-list">
        {step.mempool.length === 0 ? <p className="empty">대기 중인 transaction 없음</p> : null}
        {step.mempool.map((item) => (
          <div key={item} className="tx-card pending">
            {item}
          </div>
        ))}
      </div>
      <div className="decision-grid">
        <DecisionList title="복구됨" items={step.restored} tone="restored" />
        <DecisionList title="거절됨" items={step.rejected} tone="rejected" />
      </div>
    </article>
  );
}

function DecisionList({ title, items, tone }) {
  return (
    <div className={`decision ${tone}`}>
      <span>{title}</span>
      {items.length === 0 ? <p>없음</p> : items.map((item) => <p key={item}>{item}</p>)}
    </div>
  );
}

function BalancePanel({ balances }) {
  const max = Math.max(...Object.values(balances), 1);

  return (
    <article className="panel balance-panel">
      <header>
        <p className="panel-kicker">UTXO state</p>
        <h3>잔액 상태</h3>
      </header>
      <div className="balance-list">
        {Object.entries(balances).map(([name, amount]) => (
          <div key={name} className="balance-row">
            <div>
              <strong>{name}</strong>
              <span>{amount}</span>
            </div>
            <div className="balance-track">
              <div className="balance-fill" style={{ width: `${(amount / max) * 100}%` }} />
            </div>
          </div>
        ))}
      </div>
    </article>
  );
}

function focusLabel(focus) {
  const labels = {
    mempool: 'pending transaction 관찰',
    confirmed: 'confirmed tx 제거',
    reorg: '더 강한 branch 감지',
    replayed: 'UTXO replay + mempool 복구',
  };
  return labels[focus];
}

export default App;
