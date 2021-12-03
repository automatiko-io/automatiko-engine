package org.acme.orders.demo;

public class Order implements java.io.Serializable {

    static final long serialVersionUID = 1L;

    private java.lang.String orderNumber;
    private java.lang.Boolean shipped;
    private java.lang.Double total;
    private java.lang.Boolean accepted;

    public Order() {
    }

    public java.lang.String getOrderNumber() {
        return this.orderNumber;
    }

    public void setOrderNumber(java.lang.String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public java.lang.Boolean isShipped() {
        return this.shipped;
    }

    public void setShipped(java.lang.Boolean shipped) {
        this.shipped = shipped;
    }

    public java.lang.Double getTotal() {
        return this.total;
    }

    public void setTotal(java.lang.Double total) {
        this.total = total;
    }

    public java.lang.Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(java.lang.Boolean accepted) {
        this.accepted = accepted;
    }

    public Order(java.lang.String orderNumber, java.lang.Boolean shipped, java.lang.Double total) {
        this.orderNumber = orderNumber;
        this.shipped = shipped;
        this.total = total;
    }

    public String toString() {
        return this.orderNumber;
    }

}