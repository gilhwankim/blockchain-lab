# Stage 4: Merkle Tree and Block Body

## 목표

이번 단계의 목표는 block이 transaction 목록을 어떻게 요약해서 header에 담는지 이해하는 것이다.

Stage 1에서는 block body를 단순 문자열 `data`로 표현했다.  
하지만 실제 block에는 여러 transaction이 들어간다.

```text
Block
  header
  body
    transactions
```

문제는 transaction 목록 전체를 header에 직접 넣으면 header가 너무 커진다는 점이다.  
그래서 block header에는 transaction 전체가 아니라 **Merkle root**만 넣는다.

```text
transactions -> Merkle tree -> Merkle root -> block header
```

## 핵심 개념

### Block Body

`BlockBody`는 transaction 목록을 가진다.

```text
BlockBody(transactions)
```

이번 프로젝트에서는 Stage 3에서 만든 `Transaction`을 그대로 사용한다.

### Merkle Root

Merkle root는 transaction id들을 tree 형태로 hash해서 얻은 최종 hash다.

예를 들어 transaction이 4개라면:

```text
tx1   tx2   tx3   tx4
 |     |     |     |
h1    h2    h3    h4
  \   /       \   /
  h12         h34
     \       /
       root
```

중요한 점은 transaction 하나라도 바뀌면 leaf hash가 바뀌고, 결국 Merkle root도 바뀐다는 것이다.

### Odd Leaf 처리

leaf 개수가 홀수면 마지막 hash를 한 번 복제해서 pair를 만든다.

```text
h1 h2 h3

h1 h2 h3 h3
```

실제 Bitcoin도 Merkle tree 계산에서 비슷한 방식으로 마지막 hash를 복제한다.

### Merkle Proof

Merkle proof는 block 전체 transaction 목록을 몰라도, 특정 transaction이 Merkle root에 포함됐는지 검증할 수 있게 해준다.

proof에는 leaf에서 root까지 올라가는 데 필요한 sibling hash들이 들어간다.

```text
target tx hash + sibling hashes -> root 재계산
```

재계산한 root가 block header의 Merkle root와 같으면, 그 transaction은 block에 포함됐다고 볼 수 있다.

## 왜 중요한가?

Merkle tree는 light client 개념의 출발점이다.

전체 block body를 다 받지 않아도 다음을 검증할 수 있다.

```text
"이 transaction이 이 block에 포함됐는가?"
```

이 구조 덕분에 block header는 작게 유지하면서도, transaction 포함 여부를 효율적으로 증명할 수 있다.

## 검증 규칙

transaction block은 다음 조건을 만족해야 한다.

1. body에는 transaction이 하나 이상 있어야 한다.
2. header의 Merkle root는 body transaction id들로 다시 계산한 Merkle root와 같아야 한다.
3. block hash는 header 필드로 다시 계산한 값과 같아야 한다.
4. block hash는 difficulty 조건을 만족해야 한다.
5. Merkle proof는 target transaction id에서 header의 Merkle root를 재계산할 수 있어야 한다.

## 테스트 시나리오

1. transaction 목록으로 Merkle root를 계산할 수 있다.
2. transaction 순서가 바뀌면 Merkle root도 바뀐다.
3. transaction id 하나가 바뀌면 Merkle root도 바뀐다.
4. 특정 transaction에 대한 Merkle proof를 만들고 검증할 수 있다.
5. 잘못된 target hash로 Merkle proof를 검증하면 실패한다.
6. transaction block의 header Merkle root는 body에서 계산한 값과 같아야 한다.

## 이번 단계에서 일부러 하지 않는 것

이번 단계에서는 아직 다음을 구현하지 않는다.

- block 안 transaction의 UTXO 검증
- block reward와 fee 합산
- mempool에서 transaction 선택
- fork와 chain reorg

이번 단계는 오직 **transaction 목록을 Merkle root로 요약하고 포함 증명을 만드는 것**에 집중한다.

## 넘어가기 전 체크

다음 질문에 답할 수 있으면 다음 단계로 넘어간다.

1. 왜 block header에 transaction 전체가 아니라 Merkle root를 넣는가?
2. transaction 순서가 바뀌면 왜 Merkle root가 바뀌는가?
3. Merkle proof는 무엇을 증명하는가?
4. Merkle proof만으로 transaction의 유효성까지 증명할 수 있는가?
5. light client가 Merkle proof를 왜 필요로 하는가?
