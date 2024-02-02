package com.example.orderservice.service;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.jpa.OrderEntity;
import com.example.orderservice.jpa.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService{
    Environment env;
    OrderRepository orderRepository;

    @Autowired
    public OrderServiceImpl(Environment env, OrderRepository orderRepository) {
        this.env = env;
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderDto createOrder(OrderDto orderDto) {
        orderDto.setOrderId(UUID.randomUUID().toString());
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        OrderEntity orderEntity = new OrderEntity(orderDto);

        orderRepository.save(orderEntity);

        return orderDto;
    }

    @Override
    public OrderDto getOrderByOrderId(String orderId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);

        return new OrderDto(orderEntity);
    }

    @Override
    public Iterable<OrderEntity> getOrdersByUserId(String userId) {

        return orderRepository.findByUserId(userId);
    }
}
