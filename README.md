# skanucafe

# 서비스 시나리오

### 기능적 요구사항

1. 고객이 커피(음료)를 주문(Order)한다.
2. 고객이 지불(Pay)한다.
3. 결제모듈(payment)에 결제를 진행하게 되고 '지불'처리 된다.
4. 결제 '승인' 처리가 되면 주방에서 음료를 제조한다.
5. 고객과 매니저는 마이페이지를 통해 진행상태(OrderTrace)를 확인할 수 있다.
6. 음료가 준비되면 배달(Delivery)을 한다.
7. 고객이 취소(Cancel)하는 경우 지불 및 제조, 배달이 취소가 된다.


### 비기능적 요구사항

1. 트랜잭션
 - 결제가 되지 않으면, 주문은 받아지지 않아야 한다. - Sync 방식
2. 장애격리
 - 결제가 수행되지 않더라도 주문 취소은 지속적으로 받을 수 있어야 한다. - Async(event-driven)
 - 결제 시스템이 과중되면 주문(Order)을 잠시 후 처리하도록 유도한다. - Circuit breaker, fallback
3. 성능
 - 마이페이지에서 주문상태(OrderTrace) 확인한다. - CQRS


# 분석/설계
## Event Storming 결과


### 완성된 최종 모형 ( 시나리오 점검 후 )
![image](https://user-images.githubusercontent.com/44763296/132294391-af6c28d1-a389-483e-8b65-45708b2c9635.png)


## 헥사고날 아키텍처 다이어그램 도출


# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8085, 8088 이다)

`
   cd order
   mvn spring-boot:run

   cd payment
   mvn spring-boot:run

   cd delivery
   mvn spring-boot:run

   cd ordertrace
   mvn spring-boot:run

   cd message
   mvn spring-boot:run
   
   cd gateway
   mvn spring-boot:run
`


# DDD(Domain Driven Design) 의 적용
- msaez.io 를 통해 구현한 Aggregate 단위로 Entity 를 선언 후, 구현을 진행하였다. Entity Pattern 과 Repository Pattern 을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용하였다.

