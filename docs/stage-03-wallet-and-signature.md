# Stage 3: Wallet and Digital Signature

## 목표

Stage 2에서는 UTXO의 owner를 단순한 문자열 address로 표현했다.

하지만 실제 블록체인에서는 누구나 transaction을 만들 수 있기 때문에, 어떤 input이 특정 UTXO를 소비하려면 **그 UTXO의 소유자임을 증명**해야 한다.

이번 단계의 목표는 다음 질문에 답하는 것이다.

1. wallet은 무엇을 가지고 있는가?
2. address는 어디서 나오는가?
3. signature는 무엇을 증명하는가?
4. validator는 signature를 어떻게 검증하는가?
5. 왜 transaction 내용이 바뀌면 signature 검증이 실패하는가?

## 핵심 개념

### Private Key

`private key`는 지갑 소유자만 알고 있어야 하는 비밀 값이다.

private key는 transaction에 직접 넣지 않는다.  
대신 transaction 내용을 서명할 때 사용한다.

### Public Key

`public key`는 private key와 쌍을 이루는 공개 값이다.

validator는 public key로 signature가 올바른지 검증할 수 있다.  
하지만 public key만으로 private key를 알아낼 수는 없어야 한다.

### Address

이번 프로젝트에서는 address를 다음처럼 만든다.

```text
address = first 40 chars of sha256(publicKey)
```

실제 Bitcoin/Ethereum의 address 생성 방식은 더 복잡하지만, 학습 단계에서는 "public key에서 address가 파생된다"는 점에 집중한다.

### Signature

signature는 private key로 transaction의 핵심 내용을 서명한 값이다.

이번 단계에서 서명하는 payload는 다음 정보로 만든다.

```text
inputs' previous outpoints + outputs
```

중요한 점은 amount나 receiver가 바뀌면 payload가 바뀌고, 기존 signature는 더 이상 유효하지 않다는 것이다.

## 검증 규칙 추가

Stage 2의 transaction 검증에 다음 규칙을 추가한다.

1. 각 input은 public key를 포함해야 한다.
2. 각 input은 signature를 포함해야 한다.
3. input의 public key에서 계산한 address는 이전 output의 ownerAddress와 같아야 한다.
4. signature는 transaction signing payload에 대해 유효해야 한다.

## 왜 이것이 소유권 증명인가?

UTXO는 ownerAddress로 잠겨 있다.

```text
TxOutput(ownerAddress = AliceAddress, amount = 50)
```

Alice가 이 UTXO를 쓰려면 자신의 public key와 signature를 input에 넣는다.

validator는 다음을 확인한다.

```text
sha256(publicKey) -> AliceAddress 인가?
signature가 publicKey로 검증되는가?
```

둘 다 맞으면, 이 transaction은 Alice의 private key를 가진 사람이 만든 것으로 볼 수 있다.

## 이번 단계에서 일부러 하지 않는 것

이번 단계에서는 아직 다음을 구현하지 않는다.

- Bitcoin script
- ECDSA secp256k1
- transaction malleability
- SegWit
- multi-signature
- hierarchical deterministic wallet

Java 표준 라이브러리에서 바로 사용할 수 있는 `SHA256withECDSA`와 `secp256r1`을 사용한다.  
실제 Bitcoin은 `secp256k1`을 사용한다는 차이는 기억해둔다.

## 테스트 시나리오

1. wallet은 public key에서 address를 만든다.
2. wallet이 만든 transaction은 signature 검증을 통과한다.
3. 다른 wallet의 public key로 UTXO를 쓰려고 하면 실패한다.
4. transaction output amount가 바뀌면 signature 검증이 실패한다.
5. signature가 비어 있으면 검증이 실패한다.

## 넘어가기 전 체크

다음 질문에 답할 수 있으면 다음 단계로 넘어간다.

1. address는 private key인가, public key인가, public key에서 파생된 값인가?
2. signature가 증명하는 것은 "누가 만들었는가"인가, "누가 검증했는가"인가?
3. transaction 내용이 바뀌면 signature가 왜 무효가 되는가?
4. public key에서 address를 다시 계산하는 이유는 무엇인가?
5. UTXO model에서 signature 검증은 input마다 필요한가?
