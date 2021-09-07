package skanucafe;

import skanucafe.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired MessageRepository messageRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCancelled_SendMessage(@Payload DeliveryCancelled deliveryCancelled){

        if(!deliveryCancelled.validate()) return;

        System.out.println("\n\n##### listener SendMessage : " + deliveryCancelled.toJson() + "\n\n");

        Message message = new Message();
        message.setOrderId(deliveryCancelled.getOrderId());
        message.setDeliveryId(deliveryCancelled.getId());
        message.setStatus(deliveryCancelled.getStatus());
        message.setProduct(deliveryCancelled.getProduct());
        message.setPaymentId(deliveryCancelled.getPaymentId());
        messageRepository.save(message);

    }
    
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryStarted_SendMessage(@Payload DeliveryStarted deliveryStarted){

        if(!deliveryStarted.validate()) return;

        System.out.println("\n\n##### listener SendMessage : " + deliveryStarted.toJson() + "\n\n");

        Message message = new Message();
        message.setOrderId(deliveryStarted.getOrderId());
        message.setDeliveryId(deliveryStarted.getId());
        message.setStatus(deliveryStarted.getStatus());
        message.setProduct(deliveryStarted.getProduct());
        message.setPaymentId(deliveryStarted.getPaymentId());
        messageRepository.save(message);

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_SendMessage(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener SendMessage : " + paymentApproved.toJson() + "\n\n");

        Message message = new Message();
        message.setOrderId(paymentApproved.getOrderId());
        message.setStatus(paymentApproved.getStatus());
        message.setProduct(paymentApproved.getProduct());
        message.setPaymentId(paymentApproved.getId());
        messageRepository.save(message);

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCancelled_SendMessage(@Payload PaymentCancelled paymentCancelled){

        if(!paymentCancelled.validate()) return;

        System.out.println("\n\n##### listener SendMessage : " + paymentCancelled.toJson() + "\n\n");

        Message message = new Message();
        message.setOrderId(paymentCancelled.getOrderId());
        message.setStatus(paymentCancelled.getStatus());
        message.setProduct(paymentCancelled.getProduct());
        message.setPaymentId(paymentCancelled.getId());
        messageRepository.save(message);

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}