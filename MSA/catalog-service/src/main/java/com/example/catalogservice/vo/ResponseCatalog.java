package com.example.catalogservice.vo;

import com.example.catalogservice.jpa.CatalogEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseCatalog {
    private String productId;
    private String productName;
    private Integer unitPrice;
    private Integer stock;

    private Date createdAt;

    public ResponseCatalog(CatalogEntity entity) {
        this.productId = entity.getProductId();
        this.productName = entity.getProductName();
        this.unitPrice = entity.getUnitPrice();
        this.stock = entity.getStock();
        this.createdAt = entity.getCreatedAt();
    }
}
