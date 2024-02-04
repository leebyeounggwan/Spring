package com.example.orderservice.vo;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.jpa.OrderEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseOrder {
    private String productId;
    private Integer qty;
    private Integer unitPrice;
    private Integer totalPrice;
    private Date createdAt;

    private String orderId;

    public ResponseOrder(OrderDto orderDto) {
        this.productId = orderDto.getProductId();
        this.qty = orderDto.getQty();
        this.unitPrice = orderDto.getUnitPrice();
        this.totalPrice = orderDto.getTotalPrice();

        this.orderId = orderDto.getOrderId();
    }

    public ResponseOrder(OrderEntity orderEntity) {
        this.productId = orderEntity.getProductId();
        this.qty = orderEntity.getQty();
        this.unitPrice = orderEntity.getUnitPrice();
        this.totalPrice = orderEntity.getTotalPrice();
        this.createdAt = orderEntity.getCreatedAt();
        this.orderId = orderEntity.getOrderId();
    }
}
