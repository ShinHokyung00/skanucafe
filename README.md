# skanucafe

# 서비스 시나리오

### 기능적 요구사항

1. 고객이 커피(음료)를 주문(Order)한다.
2. 고객이 지불(Pay)한다.
3. 결제 서비스(payment)에 결제를 진행하게 되고 '지불'처리 된다.
4. 결제 '승인' 처리가 되면 주방에서 음료를 제조한다.
5. 고객과 매니저는 마이페이지를 통해 진행상태(OrderTrace)를 확인할 수 있다.
6. 음료가 준비되면 배달(Delivery)을 한다.
7. 고객이 취소(Cancel)하는 경우 지불 및 제조, 배달이 취소가 된다.
8. 결제, 배송 상태가 변경될 때 마다 고객에게 Message로 알려준다.

### 비기능적 요구사항

1. 트랜잭션
   1. 결제가 되지 않으면, 주문은 받아지지 않아야 한다. - Sync 방식
2. 장애격리
   1. 배송이 수행되지 않더라도 주문은 지속적으로 받을 수 있어야 한다. - Async(event-driven)
   2. 결제 시스템이 과중되면 주문(Order)을 잠시 후 처리하도록 유도한다. - Circuit breaker, fallback
3. 성능
   1. 마이페이지에서 주문상태(OrderTrace) 확인한다. - CQRS


# 분석/설계
## Event Storming 결과

### 이벤트 도출
### 부적격 이벤트 탈락
### 완성된 1차 모형

### 완성된 최종 모형 ( 시나리오 점검 후 )
![image](https://user-images.githubusercontent.com/44763296/132294391-af6c28d1-a389-483e-8b65-45708b2c9635.png)


## 헥사고날 아키텍처 다이어그램 도출
```
- Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
- 호출관계에서 Pub/Sub 과 Req/Resp 를 구분함
- 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐
```

# 구현
- 분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n, 8088 이다)

```
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
```


# DDD(Domain Driven Design) 의 적용

- msaez.io 를 통해 구현한 Aggregate 단위로 Entity 를 선언 후, 구현을 진행하였다. Entity Pattern 과 Repository Pattern 을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용하였다.
```
# Order 서비스의 Order.java

package skanucafe;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String product;
    private Integer qty;
    private String status;
    private Long cost;

    @PostPersist
    public void onPostPersist(){

        OrderPlaced orderPlaced = new OrderPlaced();
        BeanUtils.copyProperties(this, orderPlaced);

        skanucafe.external.Payment payment = new skanucafe.external.Payment();
        payment.setOrderId(this.getId());
        payment.setProduct(this.getProduct());
        payment.setStatus("PaymentApproved");
        payment.setQty(this.getQty());
        payment.setCost(this.getCost());
        OrderApplication.applicationContext.getBean(skanucafe.external.PaymentService.class)
            .approvePayment(payment);
        
        orderPlaced.publishAfterCommit();

    }

    @PostUpdate
    public void onPostUpdate(){
        OrderCancelled orderCancelled = new OrderCancelled();
        BeanUtils.copyProperties(this, orderCancelled);
        orderCancelled.publishAfterCommit();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public Long getCost() {
        return cost;
    }

    public void setCost(Long cost) {
        this.cost = cost;
    }
}
```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
# Order 서비스의 OrderRepository.java

package skanucafe;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="orders", path="orders")
public interface OrderRepository extends PagingAndSortingRepository<Order, Long>{

}
```

- 적용 후 REST API 의 테스트
![image](https://user-images.githubusercontent.com/44763296/132296759-91ee6282-0610-42f5-8a62-b7a34e0d7c40.png)


## Gateway 적용
API Gateway를 통하여 마이크로 서비스들의 진입점을 통일하였다.

```
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://localhost:8081
          predicates:
            - Path=/orders/** 
        - id: payment
          uri: http://localhost:8082
          predicates:
            - Path=/payments/** 
        - id: delivery
          uri: http://localhost:8083
          predicates:
            - Path=/deliveries/** 
        - id: ordertrace
          uri: http://localhost:8084
          predicates:
            - Path= /orderTraces/**
        - id: message
          uri: http://localhost:8085
          predicates:
            - Path=/messages/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://order:8080
          predicates:
            - Path=/orders/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: delivery
          uri: http://delivery:8080
          predicates:
            - Path=/deliveries/** 
        - id: ordertrace
          uri: http://ordertrace:8080
          predicates:
            - Path= /orderTraces/**
        - id: message
          uri: http://message:8080
          predicates:
            - Path=/messages/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```


## 폴리글랏 퍼시스턴스
- 배송(delivery) 서비스의 경우, 다른 마이크로 서비스와 달리 hsql을 구현하였다.
- 이를 통해 서비스 간 다른 종류의 데이터베이스를 사용하여도 문제 없이 동작하여 폴리그랏 퍼시스턴스를 충족함.
```
# 배송(delivery) 서비스의 pom.xml

		<dependency>
			<groupId>org.hsqldb</groupId>
			<artifactId>hsqldb</artifactId>
			<version>2.4.1</version>
		</dependency>
```


## 동기식 호출(Req/Res 방식)과 Fallback 처리
- 분석단계에서의 조건 중 하나로 주문(order)->결제(payment) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

- 결제(payment)서비스를 호출하기 위하여 Stub과 FeignClient 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현
```
# 주문(Order) 서비스의 order/external/PaymentService.java

package skanucafe.external;

@FeignClient(name="payment", url="http://localhost:8088")
// @FeignClient(name="payment", url="http://payment:8080")
public interface PaymentService {
    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void approvePayment(@RequestBody Payment payment);

}
```

- 주문을 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# 주문(Order) 서비스의 Order.java

    @PostPersist
    public void onPostPersist(){

        OrderPlaced orderPlaced = new OrderPlaced();
        BeanUtils.copyProperties(this, orderPlaced);

        skanucafe.external.Payment payment = new skanucafe.external.Payment();
        payment.setOrderId(this.getId());
        payment.setProduct(this.getProduct());
        payment.setStatus("PaymentApproved");
        payment.setQty(this.getQty());
        payment.setCost(this.getCost());
        OrderApplication.applicationContext.getBean(skanucafe.external.PaymentService.class)
            .approvePayment(payment);
        
        orderPlaced.publishAfterCommit();

    }
    // 이하 생략
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제(payment) 시스템이 장애가 나면 주문도 못받는다는 것을 확인
```
# 결제(payment) 서비스를 잠시 내려놓음 (ctrl+c)

# 주문처리
http post http://localhost:8088/orders product="coffee" qty=1 cost=1000 status="OrderPlaced"   #Fail
http post http://localhost:8088/orders product="tea" qty=2 cost=2000 status="OrderPlaced"      #Fail
```

![image](https://user-images.githubusercontent.com/44763296/132333751-d7c0bed0-07db-43fe-960f-d2b24809f4ff.png)

```
# 결제 서비스 재기동
cd payment
mvn spring-boot:run

# 주문처리
http post http://localhost:8088/orders product="coffee" qty=1 cost=1000 status="OrderPlaced"   #Success
http post http://localhost:8088/orders product="tea" qty=2 cost=2000 status="OrderPlaced"      #Success
```

![image](https://user-images.githubusercontent.com/44763296/132333938-b6de8d15-c8b7-4c35-9896-f633a97ef6e3.png)



- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)


## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

- 결제가 이루어진 후에 배송(delivery) 시스템으로 이를 알려주는 행위는 동기식이 아니라 비 동기식으로 처리하여 배송 시스템의 처리를 위하여 결제주문이 블로킹 되지 않아도록 처리한다.
- 이를 위하여 결제 서비스에 기록을 남긴 후에 곧바로 결제승인이 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
```
# 결제 서비스의 Payment.java

package skanucafe;

@Entity
@Table(name="Payment_table")
public class Payment {

....

    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();
    }
```

- 배송 서비스에서는 결제승인 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:
```
# 배송 서비스의 PolicyHandler.java

package skanucafe;

@Service
public class PolicyHandler{
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_StartDelivery(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener StartDelivery : " + paymentApproved.toJson() + "\n\n");

        Delivery delivery = new Delivery();
        delivery.setOrderId(paymentApproved.getOrderId());
        delivery.setProduct(paymentApproved.getProduct());
        delivery.setPaymentId(paymentApproved.getId());
        delivery.setQty(paymentApproved.getQty());
        delivery.setStatus("DeliveryStarted");
        deliveryRepository.save(delivery);
    }
```

- 배송 시스템은 주문/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 배송 시스템이 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다:
```
#  배송 서비스를 잠시 내려놓음 (ctrl+c)

#주문처리
http post http://localhost:8088/orders product="coffee" qty=1 cost=1000 status="OrderPlaced"   #Success
http post http://localhost:8088/orders product="tea" qty=2 cost=2000 status="OrderPlaced"      #Success

#주문상태 확인
http localhost:8080/orderTraces     # 주문상태 배송시작으로 안바뀜 확인
```

![image](https://user-images.githubusercontent.com/44763296/132334856-d35db031-6758-4a08-9532-0c0cc3c47903.png)


```
#배송 서비스 기동
cd delivery
mvn spring-boot:run

#주문상태 확인
http localhost:8080/orderTraces     # 모든 주문의 상태가 "DeliveryStarted"으로 확인
```

![image](https://user-images.githubusercontent.com/44763296/132334787-668cc0e5-ade6-4a68-90d2-617560cfc358.png)


## CQRS
- viewer 인 ordertraces 서비스를 별도로 구현하여 아래와 같이 view가 출력된다.
- 주문 수행 후, ordertraces

![image](https://user-images.githubusercontent.com/44763296/132335333-edac89f2-dfe8-4383-95e3-df812d423c83.png)

- 주문 취소 수행 후, ordertraces

![image](https://user-images.githubusercontent.com/44763296/132335464-a157e734-c4da-4f4e-bcbe-7589617c5552.png)


# 운영

## CI/CD 설정
- 각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD 플랫폼은 GCP를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 cloudbuild.yml 에 포함되었다.

## Deploy / Pipeline

- git에서 소스 가져오기
`
git clone https://github.com/ShinHokyung00/skanucafe
`

- Build 및 Azure Container Resistry(ACR) 에 Push 하기
```
cd order
mvn package
az acr build --registry user12acr --image user12acr.azurecr.io/order:latest .

cd ../payment
mvn package
az acr build --registry user12acr --image user12acr.azurecr.io/payment:latest .

cd ../delivery
mvn package
az acr build --registry user12acr --image user12acr.azurecr.io/delivery:latest .

cd ../ordertrace
mvn package
az acr build --registry user12acr --image user12acr.azurecr.io/ordertrace:latest .

cd ../message
mvn package
az acr build --registry user12acr --image user12acr.azurecr.io/message:latest .

cd ../gateway
mvn package
az acr build --registry user12acr --image user12acr.azurecr.io/gateway:latest .
```

- ACR에 정상 Push 완료

![image](https://user-images.githubusercontent.com/44763296/132321583-8b9cb189-26a1-4d48-8c7c-539a3a955f3b.png)


- Kafka 설치 및 배포

![image](https://user-images.githubusercontent.com/44763296/132324599-b4376e1f-2147-42dc-989f-112db76689ca.png)


- Kubernetes Deployment, Service 생성
```
cd order
kubectl apply -f kubernetes/deployment.yml
kubectl apply -f kubernetes/service.yaml

cd ../payment
kubectl apply -f kubernetes/deployment.yml
kubectl apply -f kubernetes/service.yaml

cd ../delivery
kubectl apply -f kubernetes/deployment.yml
kubectl apply -f kubernetes/service.yaml

cd ../ordertrace
kubectl apply -f kubernetes/deployment.yml
kubectl apply -f kubernetes/service.yaml

cd ../message
kubectl apply -f kubernetes/deployment.yml
kubectl apply -f kubernetes/service.yaml

cd ../gateway
kubectl apply -f kubernetes/deployment.yml
kubectl apply -f kubernetes/service.yaml
```

```
# /order/kubernetes/deployment.yml 파일

apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
  replicas: 1
  selector:
    matchLabels:
      app: order
  template:
    metadata:
      labels:
        app: order
    spec:
      containers:
        - name: order
          image: user12acr.azurecr.io/order:latest
          ports:
            - containerPort: 8080
```

- 전체 deploy 완료된 모습

![image](https://user-images.githubusercontent.com/44763296/132324570-a9fd653e-0895-4a27-a2cb-cfe0109514ba.png)


## Persistence Volume

- 비정형 데이터를 관리하기 위해 PVC 생성 파일
```
# pvc.yml

apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: order-disk
spec:
  accessModes:
  - ReadWriteMany
  storageClassName: azurefile
  resources:
    requests:
      storage: 1Gi
```
![image](https://user-images.githubusercontent.com/44763296/132337213-50718ac7-2a70-4fdd-a663-6ab41d90baf1.png)

```
# deploymeny.yml 에 volumes 정보 설정

apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
  replicas: 1
  -- 생략 --
  template:
  -- 생략 --
    spec:
      containers:
        - name: order
        -- 생략 --
          volumeMounts:
            - name: volume
              mountPath: "/mnt/azure"
      volumes:
      - name: volume
        persistentVolumeClaim:
          claimName: order-disk
```
![image](https://user-images.githubusercontent.com/44763296/132337484-b77a440a-b9c1-4fe9-874d-68d1ba52ae60.png)

```
# order 서비스 log 파일이 pvc 에 위치하도록 application.yml 설정

logging:
  level:
    root: info
  file: /mnt/azure/logs/order.log
```
- order 로그 확인

![image](https://user-images.githubusercontent.com/44763296/132337844-2c0c2ffa-a202-428f-9888-bbcb78d47108.png)



## 동기식 호출 / 서킷 브레이킹 / 장애격리

- 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
- 시나리오는 주문(order)-->결제(payment) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml
feign:
  hystrix:
    enabled: true
    
hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

- 피호출 서비스(결제:payment) 의 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
```
# 결제의 Payment.java (Entity)

    @PrePersist
    public void onPrePersist(){  //결제이력을 저장한 후 적당한 시간 끌기

        ....
        
        try {
            Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
```

- 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 실시
```
siege -c100 -t60S -r10 --content-type "application/json" 'http://order:8080/orders POST {"product": "coffee"}'
```
운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 하지만, 63.55% 가 성공하였고, 46%가 실패했다는 것은 고객 사용성에 있어 좋지 않기 때문에 Retry 설정과 동적 Scale out (replica의 자동적 추가,HPA) 을 통하여 시스템을 확장 해주는 후속처리가 필요.

- Retry 의 설정 (istio)
- Availability 가 높아진 것을 확인 (siege)


## Autoscale (HPA:HorizontalPodAutoscaler)

- 특정 수치 이상으로 사용자 요청이 증가할 경우 안정적으로 운영 할 수 있도록 HPA를 설치한다.

- order 서비스에 resource 사용량을 정의한다.
```
# order/kubernetes/deployment.yml 설정

  resources:
    requests:
      memory: "64Mi"
      cpu: "250m"
    limits:
      memory: "500Mi"
      cpu: "500m"
```

- 주문 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 10개까지 늘려준다:
```
kubectl autoscale deploy order --min=1 --max=10 --cpu-percent=15
```
![image](https://user-images.githubusercontent.com/44763296/132338691-d73aa263-154e-4235-9ed3-a23c1a69f2a0.png)

- siege를 활용하여, 부하 생성한다. (30명의 동시사용자가 30초간 부하 발생)
```
siege -c30 -t30S -r10 --content-type "application/json" 'http://order:8080/orders POST {"product": "coffee", "qty": 1, "cost": 1000, "status": "OrderPlaced"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:
```
kubectl get deploy order -w
```
![image](https://user-images.githubusercontent.com/44763296/132342069-315f0001-4cb9-4470-997b-d7f2e311593a.png)

- 어느정도 시간이 흐른 후 (약 30초) 스케일 아웃이 벌어지는 것을 확인할 수 있다:

![image](https://user-images.githubusercontent.com/44763296/132342239-2b19bdd4-d9a3-446e-8e03-2bf36eed7931.png)

- siege 의 로그를 보아도 전체적인 성공률이 높아진 것을 확인 할 수 있다.

![image](https://user-images.githubusercontent.com/44763296/132342165-ea1424f9-1342-46ba-a5e7-7851957cc061.png)


## 무정지 재배포 Zero-Downtime deploy (Readiness Probe)

- 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함
- seige 로 배포작업 직전에 워크로드를 모니터링 함.
```
siege -c1 -t60S -v http://order:8080/orders
```

- Readiness가 설정되지 않은 yml 파일로 배포 진행

![image](https://user-images.githubusercontent.com/44763296/132343512-d432877c-70c5-45f2-a13f-a1823741513d.png)

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인

![image](https://user-images.githubusercontent.com/44763296/132343347-01bbd608-7c77-4819-94f3-3e1f02df8155.png)

- 배포기간중 Availability 가 평소 100%에서 50% 대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함
```
# deployment.yml 의 readiness probe 의 설정
readinessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 10
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 10
```

- 동일한 시나리오로 재배포 한 후 Availability 확인
- 배포 중 pod가 2개가 뜨고, 새롭게 띄운 pod가 준비될 때까지, 기존 pod가 유지됨을 확인

![image](https://user-images.githubusercontent.com/44763296/132343813-c82aab51-3a5d-4c02-90ee-64361abb7cb8.png)

- 배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.

![image](https://user-images.githubusercontent.com/44763296/132343875-1f75e766-ddbf-4ed0-9d2f-a740131d8d03.png)


## Self-healing (Liveness Probe)
- order 서비스의 yml 파일에 Liveness Probe 설정을 바꾸어서, Liveness Probe 가 동작함을 확인
- Liveness Probe 옵션을 추가하되, 서비스 포트가 아닌 8090으로 설정, readiness probe는 미적용
```
        livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8090
            initialDelaySeconds: 5
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```

- order 에 liveness가 발동되었고, 8090 포트에 응답이 없기에 Restart가 발생함

![image](https://user-images.githubusercontent.com/44763296/132344822-f8392a93-21d3-4ab5-bf07-1d7321baf7de.png)

![image](https://user-images.githubusercontent.com/44763296/132344859-685305b7-ddda-4187-a61f-4d47ea920d35.png)

