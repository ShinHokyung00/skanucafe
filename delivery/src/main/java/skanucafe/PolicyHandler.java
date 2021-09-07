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

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCancelled_CancelDelivery(@Payload PaymentCancelled paymentCancelled){

        if(!paymentCancelled.validate()) return;

        System.out.println("\n\n##### listener CancelDelivery : " + paymentCancelled.toJson() + "\n\n");

        if ("PaymentCancelled".equals(paymentCancelled.getStatus())) {
            List<Delivery> deliveryList = deliveryRepository.findByOrderId(paymentCancelled.getOrderId());
        
            for (Delivery delivery : deliveryList) {
                delivery.setStatus("DeliveryCancelled");
                deliveryRepository.save(delivery);
            }
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}

}