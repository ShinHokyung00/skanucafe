package skanucafe;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="OrderTrace_table")
public class OrderTrace {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;
        private String product;
        private Integer qty;
        private Long cost;
        private Long orderId;
        private Long deliveryId;
        private String status;
        private Long paymentId;


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
        public Long getCost() {
            return cost;
        }

        public void setCost(Long cost) {
            this.cost = cost;
        }
        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }
        public Long getDeliveryId() {
            return deliveryId;
        }

        public void setDeliveryId(Long deliveryId) {
            this.deliveryId = deliveryId;
        }
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
        public Long getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(Long paymentId) {
            this.paymentId = paymentId;
        }

}