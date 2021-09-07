package skanucafe;

public class OrderCancelled extends AbstractEvent {

    private Long id;
    private String product;
    private Integer qty;
    private String status;
    private Long cost;

    public OrderCancelled(){
        super();
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