package skanucafe;

import skanucafe.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PolicyHandler{
    @Autowired PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCancelled_CancelPayment(@Payload OrderCancelled orderCancelled){

        if(!orderCancelled.validate()) return;

        System.out.println("\n\n##### listener CancelPayment : " + orderCancelled.toJson() + "\n\n");

        if ("OrderCancelled".equals(orderCancelled.getStatus())) {

            List<Payment> paymentList = paymentRepository.findByOrderId(orderCancelled.getId());

            for (Payment payment : paymentList) {
                
                    payment.setStatus("PaymentCancelled");
                    paymentRepository.save(payment);
            }
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}