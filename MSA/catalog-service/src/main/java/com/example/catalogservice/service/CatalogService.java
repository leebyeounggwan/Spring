package com.example.catalogservice.service;

import com.example.catalogservice.jpa.CatalogEntity;

import java.util.Iterator;

public interface CatalogService {
    Iterable<CatalogEntity> getAllCatalogs();

}
