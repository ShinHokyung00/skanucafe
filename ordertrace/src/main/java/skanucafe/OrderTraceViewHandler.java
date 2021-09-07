package skanucafe;

import skanucafe.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class OrderTraceViewHandler {


    @Autowired
    private OrderTraceRepository orderTraceRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrderPlaced_then_CREATE_1 (@Payload OrderPlaced orderPlaced) {

        try {

            if (!orderPlaced.validate()) return;

            // view 객체 생성
            OrderTrace orderTrace = new OrderTrace();
            // view 객체에 이벤트의 Value 를 set 함
            orderTrace.setOrderId(orderPlaced.getId());
            orderTrace.setProduct(orderPlaced.getProduct());
            orderTrace.setQty(orderPlaced.getQty());
            orderTrace.setCost(orderPlaced.getCost());
            orderTrace.setStatus(orderPlaced.getStatus());
            // view 레파지 토리에 save
            orderTraceRepository.save(orderTrace);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryStarted_then_UPDATE_1(@Payload DeliveryStarted deliveryStarted) {

        try {

            if (!deliveryStarted.validate()) return;

            // view 객체 조회
            List<OrderTrace> orderTraceList = orderTraceRepository.findByOrderId(deliveryStarted.getOrderId());

            for(OrderTrace orderTrace : orderTraceList){
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                orderTrace.setDeliveryId(deliveryStarted.getId());
                orderTrace.setStatus(deliveryStarted.getStatus());
                // view 레파지 토리에 save
                orderTraceRepository.save(orderTrace);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryCancelled_then_UPDATE_2(@Payload DeliveryCancelled deliveryCancelled) {

        try {

            if (!deliveryCancelled.validate()) return;

            // view 객체 조회
            List<OrderTrace> orderTraceList = orderTraceRepository.findByOrderId(deliveryCancelled.getOrderId());
            for(OrderTrace orderTrace : orderTraceList){
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                orderTrace.setStatus(deliveryCancelled.getStatus());
                // view 레파지 토리에 save
                orderTraceRepository.save(orderTrace);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

