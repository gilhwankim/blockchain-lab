# Stage 6: Fork Choice and Reorganization

## 목표

이번 단계의 목표는 blockchain network에서 서로 다른 block 후보가 생겼을 때 node가 어떤 chain을 canonical chain으로 선택하는지 이해하는 것이다.

현실의 network에서는 모든 node가 동시에 같은 block을 보지 않는다.

예를 들어 miner A와 miner B가 거의 동시에 같은 parent 위에 block을 만들 수 있다.

```text
genesis -> block 1A
        -> block 1B
```

이 상황을 fork라고 부른다.

## 핵심 개념

### Canonical Chain

`canonical chain`은 현재 node가 "정식 chain"으로 선택한 block들의 경로다.

fork가 있어도 node는 하나의 canonical chain만 선택한다.

### Fork

fork는 같은 parent를 가진 서로 다른 block들이 존재하는 상황이다.

```text
parent -> child A
       -> child B
```

둘 중 하나는 canonical chain에 남고, 다른 하나는 side branch가 된다.

### Heaviest Chain

Bitcoin을 단순히 "가장 긴 chain"으로 설명하는 경우가 많지만, 더 정확한 감각은 **가장 많은 work가 누적된 chain**이다.

이번 프로젝트에서는 단순화를 위해 block difficulty를 누적해 chain work를 계산한다.

```text
chainWork = sum(block difficulty)
```

실제 Bitcoin의 work 계산은 difficulty 숫자를 단순 합산하는 것보다 복잡하다.  
하지만 이번 단계에서는 "길이보다 누적 작업량이 중요하다"는 개념에 집중한다.

### Reorganization

`reorg`는 node가 canonical chain을 다른 branch로 바꾸는 일이다.

예를 들어 처음에는 A branch를 canonical으로 선택했다가, B branch가 더 많은 work를 가지게 되면 canonical chain이 바뀐다.

```text
Before:
genesis -> A1 -> A2

After:
genesis -> B1 -> B2 -> B3
```

이때 A1, A2는 canonical chain에서 빠지고, B1, B2, B3가 canonical chain에 들어온다.

## 왜 중요한가?

reorg는 transaction confirmation 개념과 직접 연결된다.

어떤 transaction이 block에 들어갔다고 해서 즉시 절대적으로 확정되는 것은 아니다.  
그 block이 속한 branch가 canonical chain에서 밀려나면 transaction도 canonical history에서 빠질 수 있다.

그래서 결제 시스템은 보통 여러 confirmation을 기다린다.

```text
1 confirmation  = transaction 포함 block 위에 아직 추가 block이 거의 없음
6 confirmations = transaction 포함 block 위에 여러 block이 쌓임
```

## 이번 단계의 단순화

이번 구현에서는 다음을 단순화한다.

1. block validation은 `TransactionBlockMiner.isValid`로 확인한다.
2. block의 누적 work는 parent work + difficulty로 계산한다.
3. work가 더 큰 branch를 canonical chain으로 선택한다.
4. work가 같으면 먼저 본 chain을 유지한다.
5. transaction 재적용이나 UTXO rollback은 아직 하지 않는다.

UTXO rollback은 다음 단계에서 더 깊게 다룰 수 있다.

## 테스트 시나리오

1. genesis block만 있을 때 canonical chain height는 0이다.
2. parent가 없는 block은 orphan으로 보관된다.
3. 같은 parent에 두 block이 붙으면 fork가 생긴다.
4. work가 같은 fork에서는 기존 canonical chain을 유지한다.
5. 더 많은 work를 가진 branch가 들어오면 reorg가 발생한다.
6. reorg plan은 removed blocks와 added blocks를 알려준다.

## 넘어가기 전 체크

다음 질문에 답할 수 있으면 다음 단계로 넘어간다.

1. fork는 왜 생기는가?
2. canonical chain은 모든 node에서 항상 즉시 같은가?
3. longest chain보다 heaviest chain이 더 정확한 표현인 이유는 무엇인가?
4. reorg가 transaction finality에 어떤 영향을 주는가?
5. confirmation을 여러 개 기다리는 이유는 무엇인가?
