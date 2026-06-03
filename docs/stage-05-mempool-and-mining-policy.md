# Stage 5: Mempool and Mining Policy

## 목표

이번 단계의 목표는 transaction이 block에 들어가기 전 어디에 머무는지, 그리고 miner가 어떤 기준으로 transaction을 선택하는지 이해하는 것이다.

blockchain에서 transaction은 만들어지자마자 block에 들어가지 않는다.  
먼저 node의 `mempool`에 들어간 뒤, miner가 block candidate를 만들 때 선택된다.

```text
wallet -> transaction -> mempool -> block candidate -> mined block
```

## Mempool이 필요한 이유

mempool은 아직 block에 포함되지 않은 pending transaction들의 임시 저장소다.

node는 transaction을 받으면 바로 저장하지 않고 먼저 검증한다.

```text
1. transaction 수신
2. signature 검증
3. UTXO 존재 여부 검증
4. input/output 금액 검증
5. 통과하면 mempool에 저장
```

## Mining Policy

miner는 mempool에 있는 모든 transaction을 무조건 block에 넣지 않는다.

이유는 다음과 같다.

1. block 크기에는 제한이 있다.
2. 같은 UTXO를 소비하는 transaction들이 동시에 있을 수 있다.
3. miner는 보통 더 높은 fee를 주는 transaction을 선호한다.

이번 단계에서는 단순한 policy를 사용한다.

```text
fee가 높은 transaction부터 선택한다.
최대 transaction 개수까지만 선택한다.
선택 과정에서 double spend가 생기면 뒤 transaction은 제외한다.
```

## 왜 submit 검증만으로 부족한가?

각 transaction을 mempool에 넣을 때는 현재 UTXO set 기준으로 유효할 수 있다.

하지만 두 transaction이 같은 UTXO를 소비한다면 둘 다 개별적으로는 유효해 보일 수 있다.

```text
Tx1 spends UTXO A
Tx2 spends UTXO A
```

둘을 같은 block candidate에 함께 넣으면 double spend가 된다.

그래서 block candidate를 만들 때는 UTXO set 복사본에 transaction을 순서대로 적용해보며 충돌을 걸러야 한다.

## 이번 단계의 검증 규칙

1. coinbase transaction은 mempool에 들어갈 수 없다.
2. mempool은 유효한 regular transaction만 받는다.
3. mempool entry는 transaction fee를 함께 저장한다.
4. block candidate는 fee가 높은 순서로 transaction을 선택한다.
5. 같은 UTXO를 쓰는 transaction이 여러 개면 먼저 선택된 transaction만 block candidate에 들어간다.
6. block candidate 생성은 실제 UTXO set을 변경하지 않는다.

## 테스트 시나리오

1. 유효한 transaction은 mempool에 들어간다.
2. coinbase transaction은 mempool에 들어갈 수 없다.
3. invalid transaction은 mempool에 들어갈 수 없다.
4. block candidate는 fee가 높은 transaction을 먼저 선택한다.
5. 같은 UTXO를 소비하는 transaction이 여러 개면 하나만 선택된다.
6. candidate 생성 후에도 실제 UTXO set은 그대로 유지된다.

## 이번 단계에서 일부러 하지 않는 것

이번 단계에서는 아직 다음을 구현하지 않는다.

- network broadcast
- mempool expiration
- replace-by-fee
- block size를 byte 단위로 계산
- fee rate 계산
- orphan transaction

이번 단계에서는 오직 **pending transaction을 검증해 모아두고, block candidate를 만드는 과정**에 집중한다.

## 넘어가기 전 체크

다음 질문에 답할 수 있으면 다음 단계로 넘어간다.

1. mempool은 block인가, 임시 대기열인가?
2. mempool에 들어간 transaction은 확정된 transaction인가?
3. 왜 miner는 fee가 높은 transaction을 선호하는가?
4. 개별 transaction은 유효하지만 같은 block에 함께 넣으면 invalid가 될 수 있는 이유는 무엇인가?
5. block candidate를 만들 때 실제 UTXO set이 아니라 복사본으로 검증하는 이유는 무엇인가?
