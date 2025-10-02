package com.example.extractor.repository;

import com.example.extractor.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    boolean existsById(String id);

}
