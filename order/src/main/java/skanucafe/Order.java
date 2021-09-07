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