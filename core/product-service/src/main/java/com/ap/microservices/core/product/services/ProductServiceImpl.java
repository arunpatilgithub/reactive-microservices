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
    public Product getProduct(int productId) {

        log.debug("/product return the found product for productId = {}", productId);

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        ProductEntity productEntity = repository.findByProductId(productId).orElseThrow(
                () -> new NotFoundException("No product found for productId: " + productId));

        Product newProduct = mapper.entityToApi(productEntity);
        newProduct.setServiceAddress(serviceUtil.getServiceAddress());

        return newProduct;
    }

    @Override
    public Product createProduct(Product body) {

        try {
            ProductEntity productEntity = mapper.apiToEntity(body);
            return mapper.entityToApi(repository.save(productEntity));
        } catch (DuplicateKeyException dke) {
            log.error("Cannot create product with same id: {}", body.getProductId());
            throw new InvalidInputException("Duplicate key, Product Id: " +
                                                    body.getProductId());
        }

    }

    @Override
    public void deleteProduct(int productId) {

        //Idempotent behavior.
        repository.findByProductId(productId).ifPresent(e -> repository.delete(e));

    }
}
