import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Order {
    private Integer id;
    private String reference;
    private Instant creationDatetime;
    private List<DishOrder> dishOrderList;
    private PaymentStatusEnum paymentStatus;
    private Sale sale;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        checkIfModifiable();
        this.reference = reference;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public void setCreationDatetime(Instant creationDatetime) {
        checkIfModifiable();
        this.creationDatetime = creationDatetime;
    }

    public List<DishOrder> getDishOrderList() {
        return dishOrderList;
    }

    public void setDishOrderList(List<DishOrder> dishOrderList) {
        checkIfModifiable();
        this.dishOrderList = dishOrderList;
    }

    public PaymentStatusEnum getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatusEnum paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }


    private void checkIfModifiable() {
        if (this.paymentStatus == PaymentStatusEnum.PAID) {
            throw new RuntimeException("La commande a déjà été payée et donc ne peut plus être modifiée.");
        }
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", creationDatetime=" + creationDatetime +
                ", paymentStatus=" + paymentStatus +
                ", sale=" + (sale != null ? "Assigned" : "None") +
                '}';
    }


    Double getTotalAmountWithoutVat() {
        throw new RuntimeException("Not implemented");
    }

    Double getTotalAmountWithVat() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id) &&
                Objects.equals(reference, order.reference) &&
                Objects.equals(paymentStatus, order.paymentStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reference, paymentStatus);
    }
}