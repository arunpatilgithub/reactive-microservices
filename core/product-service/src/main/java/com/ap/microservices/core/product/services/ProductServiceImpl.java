package com.ap.microservices.core.product.services;

import com.ap.api.core.product.Product;
import com.ap.api.core.product.ProductService;
import com.ap.microservices.core.product.persistence.ProductEntity;
import com.ap.microservices.core.product.persistence.ProductRepository;
import com.ap.util.exceptions.InvalidInputException;
import com.ap.util.exceptions.NotFoundException;
import com.ap.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

import static reactor.core.publisher.Mono.error;

@Slf4j
@RestController
public class ProductServiceImpl implements ProductService {

    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil, ProductRepository repository, ProductMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Product> getProduct(int productId) {

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        return repository.findByProductId(productId)
                         .switchIfEmpty(error(new NotFoundException("No product found for productId: " + productId)))
                         .log()
                         .map(e -> mapper.entityToApi(e))
                         .map(e -> {e.setServiceAddress(serviceUtil.getServiceAddress()); return e;});
    }

    @Override
    public Product createProduct(Product body) throws ExecutionException, InterruptedException {

        if (body.getProductId() < 1) throw new InvalidInputException("Invalid productId: " + body.getProductId());

        ProductEntity entity = mapper.apiToEntity(body);
        Mono<Product> newEntity = repository.save(entity)
                                            .log()
                                            .onErrorMap(
                                                    DuplicateKeyException.class,
                                                    ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
                                            .map(e -> mapper.entityToApi(e));

        return newEntity.toFuture().get();

    }

    @Override
    public void deleteProduct(int productId) {

        //Idempotent behavior.
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        log.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        repository.findByProductId(productId).log().map(e -> repository.delete(e)).flatMap(e -> e).block();

    }
}
