package com.example.orderservice.jpa;

import com.example.orderservice.dto.OrderDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "orders")
@NoArgsConstructor
public class OrderEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, unique = true)
    private String productId;
    @Column(nullable = false)
    private Integer qty;
    @Column(nullable = false)
    private Integer unitPrice;
    @Column(nullable = false)
    private Integer totalPrice;

    @Column(nullable = false)
    private String userId;
    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false, updatable = false, insertable = false)
    @ColumnDefault(value = "CURRENT_TIMESTAMP")
    private Date createdAt;

    public OrderEntity(OrderDto orderDto) {
        this.productId = orderDto.getProductId();
        this.qty = orderDto.getQty();
        this.unitPrice = orderDto.getUnitPrice();
        this.totalPrice = orderDto.getTotalPrice();
        this.userId = orderDto.getUserId();
        this.orderId = orderDto.getOrderId();
    }
}
