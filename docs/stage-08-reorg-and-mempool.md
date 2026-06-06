# Stage 08. Reorg와 Mempool 복구

## 이번 단계의 목표

Stage 07에서는 reorg가 발생하면 canonical chain 기준으로 UTXO 상태를 다시 계산했다. 이번 단계에서는 그 다음 문제를 다룬다.

canonical chain에서 빠진 block 안에 있던 transaction은 어떻게 될까?

답은 단순하지 않다. 어떤 transaction은 다시 mempool로 돌아갈 수 있고, 어떤 transaction은 더 이상 유효하지 않아서 버려져야 한다.

## 왜 mempool 복구가 필요한가?

mempool은 아직 block에 들어가지 않은 pending transaction을 보관하는 공간이다.

transaction이 block에 포함되면 node는 보통 그 transaction을 mempool에서 제거한다. 이미 확정된 transaction을 다시 pending 상태로 둘 필요가 없기 때문이다.

하지만 reorg가 발생하면 상황이 바뀐다. 예전에 canonical chain에 포함되어 확정된 것처럼 보였던 block이 canonical chain에서 빠질 수 있다. 그러면 그 block 안의 transaction도 더 이상 확정된 transaction이 아니다.

이때 node는 빠진 block의 transaction을 다시 mempool에 넣을지 판단해야 한다.

## 이번 단계의 처리 규칙

이번 구현에서는 reorg가 발생하면 mempool을 다음 순서로 정리한다.

1. 새 canonical chain에 추가된 block의 transaction을 mempool에서 제거한다.
2. canonical chain에서 제거된 block의 transaction을 확인한다.
3. coinbase transaction은 mempool에 넣지 않는다.
4. 일반 transaction은 현재 UTXO 상태 기준으로 다시 검증한다.
5. 아직 유효하면 mempool에 다시 넣는다.
6. 이미 소비된 UTXO를 참조하는 등 유효하지 않으면 버린다.

## 쉬운 예시

genesis에서 Alice와 Dave가 각각 UTXO를 받았다고 하자.

branch A에는 두 transaction이 들어 있다.

- Alice가 Bob에게 60을 보낸다.
- Dave가 Erin에게 45를 보낸다.

그런데 나중에 branch B가 더 큰 누적 work를 가지면서 canonical chain이 된다.

branch B에서는 Alice가 같은 genesis UTXO를 Carol에게 70 보냈다. 따라서 Alice가 Bob에게 보낸 transaction은 더 이상 유효하지 않다. 이미 같은 UTXO가 branch B에서 Carol에게 소비되었기 때문이다.

반면 Dave가 Erin에게 보낸 transaction은 branch B와 충돌하지 않는다. Dave의 UTXO는 아직 소비되지 않았기 때문에 이 transaction은 다시 mempool로 돌아갈 수 있다.

## 구현한 코드

`Mempool.reconcileAfterReorg()`는 `ReorgPlan`과 현재 `UtxoSet`을 받아 mempool을 정리한다.

결과는 `ReorgMempoolResult`로 반환한다.

- `removedConfirmedTransactions`: 새 canonical chain에 포함되어 mempool에서 제거된 transaction
- `restoredTransactions`: reorg로 빠졌지만 다시 mempool에 들어간 transaction
- `rejectedTransactions`: reorg로 빠졌지만 현재 UTXO 기준으로 유효하지 않아 버려진 transaction

## 중요한 관찰

reorg는 consensus와 state만의 문제가 아니다. mempool도 영향을 받는다.

실제 blockchain node는 reorg가 발생했을 때 다음 요소들을 함께 갱신해야 한다.

- canonical chain
- UTXO 또는 account state
- mempool
- transaction confirmation status

이번 단계에서는 그중 mempool 복구의 가장 기본적인 규칙을 구현했다.

## 이번 단계에서 일부러 단순화한 것

이번 구현은 transaction 하나씩 현재 UTXO 상태에 대해 검증한다. 따라서 mempool 안에서 서로 의존하는 transaction 묶음은 아직 깊게 다루지 않는다.

예를 들어 reorg로 빠진 transaction A가 새 output을 만들고, transaction B가 그 output을 소비하는 구조라면 실제 mempool은 둘을 package처럼 함께 다룰 수 있다. 하지만 이번 단계에서는 개념을 명확히 하기 위해 현재 chain state 기준으로 독립적으로 유효한 transaction만 복구한다.
