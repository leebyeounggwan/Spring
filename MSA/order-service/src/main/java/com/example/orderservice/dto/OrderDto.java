package com.example.orderservice.dto;

import com.example.orderservice.jpa.OrderEntity;
import com.example.orderservice.vo.RequestOrder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
@Getter
@Setter
public class OrderDto implements Serializable {
    private String productId;
    private Integer qty;
    private Integer unitPrice;
    private Integer totalPrice;

    private String orderId;
    private String userId;

    public OrderDto(OrderEntity orderEntity) {
        this.productId = orderEntity.getProductId();
        this.qty = orderEntity.getQty();
        this.unitPrice = orderEntity.getUnitPrice();
        this.totalPrice = orderEntity.getTotalPrice();
        this.orderId = orderEntity.getOrderId();
        this.userId = orderEntity.getUserId();
    }

    public OrderDto(RequestOrder requestOrder) {
        this.productId = requestOrder.getProductId();
        this.qty = requestOrder.getQty();
        this.unitPrice = requestOrder.getUnitPrice();
    }
}
