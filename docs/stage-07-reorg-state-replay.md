# Stage 07. Reorg와 UTXO 상태 재생

## 이번 단계의 목표

Stage 06에서는 더 큰 누적 work를 가진 branch가 나타났을 때 canonical chain이 바뀌는 과정을 구현했다. 하지만 실제 blockchain에서는 chain의 tip만 바뀌는 것으로 끝나지 않는다. canonical chain이 바뀌면 그 chain을 기준으로 계산한 ledger state도 함께 바뀌어야 한다.

이번 단계에서는 reorg가 발생했을 때 UTXO 상태를 새 canonical chain 기준으로 다시 계산한다. 이 과정을 통해 "block 순서"와 "잔액 상태"가 분리되어 있으면서도 서로 강하게 연결되어 있다는 점을 확인한다.

## 핵심 개념

UTXO 모델에서 잔액은 account 필드에 저장된 숫자가 아니다. 아직 소비되지 않은 output들의 합계다. 따라서 canonical chain이 바뀌면 어떤 output이 소비되었는지, 어떤 output이 새로 생겼는지도 다시 판단해야 한다.

예를 들어 같은 genesis reward를 Alice가 Bob에게 보내는 block과 Carol에게 보내는 block이 서로 다른 fork에 있을 수 있다. 처음에는 Bob에게 보낸 branch가 canonical chain일 수 있지만, 나중에 Carol에게 보낸 branch가 더 큰 누적 work를 갖게 되면 canonical chain이 바뀐다. 그러면 Bob의 잔액은 사라지고 Carol의 잔액이 생긴다.

## 상태를 다시 계산하는 방식

이번 구현에서는 reorg가 발생하면 removed block을 하나씩 되돌리는 복잡한 방식 대신, genesis부터 새 canonical tip까지 모든 transaction을 순서대로 다시 적용한다.

이 방식은 실제 운영 환경에서는 비효율적일 수 있다. 하지만 학습 단계에서는 다음 장점이 있다.

- 상태 계산 규칙이 명확하다.
- rollback 버그를 피할 수 있다.
- canonical chain과 UTXO state의 관계를 눈으로 확인하기 쉽다.

즉, 지금은 성능보다 정확한 개념 이해를 우선한다.

## 구현한 코드

`CanonicalChainState`는 현재 canonical chain과 그 chain으로부터 계산한 `UtxoSet`을 함께 들고 있다.

`ReorgPlan`이 변경 사항을 포함하면 다음 순서로 처리한다.

1. 새 canonical chain을 가져온다.
2. 빈 `UtxoSet`을 만든다.
3. genesis block부터 tip block까지 모든 transaction을 순서대로 적용한다.
4. 새로 계산된 `UtxoSet`을 현재 상태로 교체한다.

## 테스트 시나리오

테스트는 다음 흐름을 검증한다.

1. genesis에서 Alice에게 100을 지급한다.
2. branch A에서는 Alice가 Bob에게 60을 보낸다.
3. branch B에서는 Alice가 같은 genesis UTXO를 Carol에게 70을 보낸다.
4. 처음에는 branch A가 canonical chain이므로 Bob의 잔액이 60이다.
5. branch B가 더 큰 누적 work를 갖게 되면 reorg가 발생한다.
6. 상태를 다시 재생하면 Bob의 잔액은 0이 되고, Carol의 잔액은 70이 된다.

## 중요한 관찰

reorg는 단순히 "block 목록이 바뀌었다"는 사건이 아니다. 그 block 목록 위에서 계산된 모든 상태가 바뀔 수 있다는 뜻이다.

그래서 실제 blockchain client는 consensus, block storage, mempool, state database를 분리해 관리하면서도, canonical chain 변경이 일어날 때 이 구성요소들을 함께 갱신해야 한다.

다음 단계에서는 이 흐름을 더 현실적으로 확장해서 reorg로 제거된 transaction을 mempool로 되돌리는 문제를 다룰 수 있다.
