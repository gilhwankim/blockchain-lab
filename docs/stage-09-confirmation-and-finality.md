# Stage 09. Confirmation과 Finality

## 이번 단계의 목표

Stage 08에서는 reorg가 발생했을 때 mempool을 어떻게 정리하고 복구하는지 구현했다. 이번 단계에서는 사용자가 가장 자주 궁금해하는 질문을 다룬다.

이 transaction은 정말 확정된 걸까?

blockchain에서는 transaction이 block에 들어갔다고 해서 즉시 완전히 안전해졌다고 보지 않는다. 그 block 위에 새로운 block이 더 쌓일수록 reorg로 뒤집힐 가능성이 줄어든다. 이 깊이를 confirmation이라고 부른다.

## Confirmation이란?

transaction이 들어간 block부터 현재 canonical tip까지 몇 개의 block이 이어져 있는지를 confirmation 수로 계산한다.

예를 들어 transaction이 10번 block에 들어갔고 현재 canonical tip이 12번 block이라면 confirmation 수는 3이다.

```text
10번 block: transaction 포함
11번 block
12번 block: 현재 tip

confirmations = 3
```

즉 transaction이 포함된 block 자기 자신도 confirmation 1개로 센다.

## Finality란?

finality는 더 이상 쉽게 뒤집히지 않는다고 판단하는 기준이다. 이 프로젝트에서는 학습을 위해 단순한 규칙을 사용한다.

```text
confirmations >= finalityThreshold 이면 FINALIZED
```

예를 들어 `finalityThreshold`가 3이면 transaction이 포함된 block 위로 충분히 chain이 이어졌을 때 `FINALIZED` 상태가 된다.

실제 blockchain마다 finality의 의미는 다르다. Bitcoin 같은 PoW chain은 확률적 finality에 가깝고, Ethereum PoS는 checkpoint 기반 finality 개념을 함께 사용한다. 이번 단계에서는 먼저 confirmation depth라는 공통 기초를 이해하는 데 집중한다.

## Transaction 상태

이번 구현에서는 transaction 상태를 네 가지로 나눈다.

- `PENDING`: mempool에 있지만 아직 canonical chain에 포함되지 않은 상태
- `CONFIRMED`: canonical chain에 포함되었지만 finality threshold에는 아직 도달하지 않은 상태
- `FINALIZED`: canonical chain에 포함되었고 충분한 confirmation을 얻은 상태
- `UNKNOWN`: canonical chain에도 없고 mempool에도 없는 상태

## Reorg와 상태 변화

reorg가 발생하면 confirmed였던 transaction도 canonical chain에서 빠질 수 있다.

빠진 transaction이 현재 UTXO 기준으로 여전히 유효하면 mempool로 돌아가 `PENDING`이 된다. 반대로 새 canonical chain과 충돌하면 mempool에도 들어가지 못하고 `UNKNOWN` 상태가 된다.

이 흐름은 Stage 08에서 구현한 mempool 복구와 직접 연결된다.

## 구현한 코드

`TransactionConfirmationTracker`는 다음 정보를 기준으로 transaction 상태를 계산한다.

1. 현재 canonical chain
2. 현재 mempool
3. finality threshold

canonical chain 안에서 transaction을 찾으면 block height와 tip height를 비교해 confirmation 수를 계산한다.

canonical chain에 없지만 mempool에 있으면 `PENDING`으로 본다.

둘 다 없으면 `UNKNOWN`으로 본다.

## 중요한 관찰

transaction 상태는 고정된 값이 아니다. node가 바라보는 canonical chain과 mempool 상태에 따라 계속 바뀐다.

그래서 실제 서비스에서 결제나 입금을 처리할 때는 단순히 "block에 들어갔다"만 보지 않고 confirmation 수를 함께 확인한다.
