# Stage 1: Block, Hash, Proof of Work

## Goal

첫 단계의 목표는 블록체인을 "거래 시스템"으로 보기 전에, 먼저 "검증 가능한 변경 이력"으로 이해하는 것이다.

이 단계에서는 아직 지갑, 서명, 트랜잭션, UTXO를 다루지 않는다. 오직 다음 질문에 집중한다.

1. 블록은 무엇을 기록하는가?
2. 블록 해시는 무엇을 보장하는가?
3. 이전 블록 해시를 저장하면 무엇이 달라지는가?
4. nonce와 difficulty는 왜 필요한가?
5. 체인 검증은 어떤 조건을 확인해야 하는가?

## Core Concepts

### Block Header

블록 해시는 블록 전체 객체가 아니라 header를 기준으로 계산한다.

현재 단계의 header 필드:

- `index`: 체인에서의 위치
- `previousHash`: 직전 블록 header hash
- `timestamp`: 블록 생성 시각
- `dataHash`: 블록 body 데이터의 hash
- `difficulty`: 요구되는 작업 난이도
- `nonce`: PoW를 만족시키기 위해 바꾸는 숫자

### Data Hash

현재는 트랜잭션 목록 대신 문자열 `data`를 block body로 사용한다.

블록 header에는 원본 data를 넣지 않고 `dataHash`만 넣는다. 나중에 Merkle root를 넣는 구조로 확장하기 위한 준비다.

### Proof of Work

PoW는 해시가 특정 조건을 만족할 때까지 nonce를 바꿔 반복 계산하는 과정이다.

현재 조건:

```text
hash starts with difficulty number of zeroes
```

예:

```text
difficulty = 4
valid hash = 0000abcd...
```

PoW가 보장하는 것은 "수정 불가능성" 그 자체가 아니다. 정확히는 블록을 수정하려면 이후 블록들의 작업을 다시 해야 하므로 수정 비용이 커진다는 점이다.

### Chain Validation

체인은 다음 조건을 모두 만족해야 valid다.

1. genesis block이 정해진 규칙과 일치한다.
2. 각 블록의 `index`가 이전 블록보다 1 크다.
3. 각 블록의 `previousHash`가 이전 블록의 hash와 일치한다.
4. 각 블록의 hash가 header 필드로 재계산한 값과 일치한다.
5. 각 블록의 hash가 difficulty 조건을 만족한다.
6. 각 블록의 `dataHash`가 body data로 재계산한 값과 일치한다.

## Test Scenarios

1. 새 체인은 genesis block 하나를 가진다.
2. 블록을 추가하면 체인 height가 증가한다.
3. 채굴된 블록 hash는 difficulty 조건을 만족한다.
4. 기존 블록 data가 바뀌면 체인 검증은 실패해야 한다.
5. 기존 블록 previousHash가 바뀌면 체인 검증은 실패해야 한다.

## Completion Criteria

다음 질문에 답할 수 있으면 다음 단계로 넘어간다.

1. 블록 hash는 왜 header 기준으로 계산하는가?
2. `previousHash`는 어떤 공격을 어렵게 만드는가?
3. PoW는 보안인가, 비용인가?
4. difficulty를 높이면 정확히 무엇이 어려워지는가?
5. dataHash와 나중에 배울 Merkle root는 어떤 관계인가?
