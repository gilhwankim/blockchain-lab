# Stage 2: UTXO Model

## 목표

이번 단계의 목표는 Bitcoin 스타일 원장에서 **잔고(balance)** 가 단순한 숫자 컬럼으로 저장되지 않는다는 점을 이해하는 것이다.

account model에서는 Alice의 잔고를 이런 식으로 생각하기 쉽다.

```text
Alice.balance = 50
```

하지만 UTXO model에서는 Alice가 쓸 수 있는 금액은 다음 조건을 만족하는 output들의 합이다.

1. Alice의 address로 잠겨 있다.
2. 아직 어떤 transaction input에서도 소비되지 않았다.

```text
Alice spendable balance = Alice가 소유한 unspent output들의 합
```

즉, 잔고는 저장된 값이 아니라 **현재 UTXO set에서 계산되는 파생 값**이다.

## 핵심 개념

### OutPoint

`OutPoint`는 이전 transaction의 특정 output 하나를 가리키는 식별자다.

```text
OutPoint = transactionId + outputIndex
```

transaction 하나는 여러 output을 만들 수 있기 때문에 `transactionId`만으로는 부족하다.  
그래서 몇 번째 output인지 나타내는 `outputIndex`가 함께 필요하다.

### TxOutput

`TxOutput`은 특정 owner address에게 amount를 잠가둔 기록이다.

```text
TxOutput(ownerAddress, amount)
```

현재 단계에서는 소유권을 단순한 문자열 address로 표현한다.  
digital signature를 통한 실제 소유권 증명은 다음 단계에서 다룬다.

### TxInput

`TxInput`은 이전 output을 소비하기 위해 그 output의 `OutPoint`를 참조한다.

```text
TxInput(previousOutPoint)
```

즉, 새로운 transaction은 "내가 이 이전 output을 쓰겠다"라고 선언한다.

### Transaction

일반 transaction은 기존 UTXO를 소비하고, 새로운 UTXO를 만든다.

```text
inputs  -> 기존 output 소비
outputs -> 새로운 output 생성
```

예를 들어 Alice가 50짜리 UTXO 하나를 가지고 있고 Bob에게 30을 보낸다면, transaction은 보통 이렇게 생긴다.

```text
input:
  Alice의 50 UTXO

outputs:
  Bob 30
  Alice 20
```

여기서 Alice 20은 거스름돈(change output)이다.  
UTXO는 일부만 잘라 쓸 수 없기 때문에, 기존 output 전체를 소비하고 남는 금액을 새 output으로 다시 받는다.

### Coinbase Transaction

`coinbase transaction`은 miner에게 block reward를 지급하기 위해 새 coin을 만드는 특수 transaction이다.

일반 transaction은 input이 있어야 하지만, coinbase transaction은 input이 없다.

```text
coinbase:
  inputs: 없음
  outputs: miner reward
```

실제 Bitcoin에서도 block reward는 coinbase transaction을 통해 새로 발행된다.

## 검증 규칙

일반 transaction이 유효하려면 다음 조건을 만족해야 한다.

1. input이 하나 이상 있어야 한다.
2. output이 하나 이상 있어야 한다.
3. 모든 input이 참조하는 OutPoint가 현재 UTXO set에 존재해야 한다.
4. 하나의 transaction 안에서 같은 input을 두 번 쓰면 안 된다.
5. 모든 output amount는 0보다 커야 한다.
6. input amount의 합은 output amount의 합보다 크거나 같아야 한다.

차액은 transaction fee가 된다.

```text
fee = totalInput - totalOutput
```

예를 들어 input이 50이고 output이 Bob 30, Alice 15라면:

```text
input sum = 50
output sum = 45
fee = 5
```

## Double Spend가 실패하는 이유

어떤 transaction이 UTXO `A`를 소비하면, `A`는 UTXO set에서 제거된다.

그 이후 다른 transaction이 다시 `A`를 쓰려고 하면, validator는 `A`를 UTXO set에서 찾을 수 없다.

따라서 검증은 실패한다.

```text
1. UTXO A 존재
2. Tx1이 UTXO A 소비
3. UTXO set에서 A 제거
4. Tx2가 UTXO A를 다시 소비하려고 함
5. A가 없으므로 invalid
```

이 구조가 double spend를 막는 핵심이다.

## 테스트 시나리오

1. coinbase transaction은 spendable balance를 만든다.
2. regular transaction은 기존 UTXO를 소비하고 새 UTXO를 만든다.
3. 같은 UTXO를 두 번 소비하면 실패한다.
4. input보다 더 큰 output을 만들 수 없다.
5. 잔고는 저장된 account field가 아니라 unspent output의 합으로 계산된다.
6. transaction fee는 input sum에서 output sum을 뺀 값이다.

## 이번 단계에서 일부러 하지 않는 것

이번 단계에서는 다음 내용을 아직 구현하지 않는다.

- wallet
- public/private key
- digital signature
- script
- mempool
- block에 transaction 포함

이것들을 한 번에 넣으면 UTXO의 본질이 흐려진다.  
이번 단계에서는 오직 **output을 소비하고 새 output을 만드는 원장 구조**에 집중한다.

## 넘어가기 전 체크

다음 질문에 답할 수 있으면 다음 단계로 넘어간다.

1. 왜 balance는 저장하지 않고 UTXO set에서 계산하는가?
2. `OutPoint`는 정확히 무엇을 식별하는가?
3. 왜 transaction input은 이전 output을 참조하는가?
4. UTXO가 한 번 소비되면 왜 다시 쓸 수 없는가?
5. `input sum - output sum`이 왜 transaction fee가 되는가?
