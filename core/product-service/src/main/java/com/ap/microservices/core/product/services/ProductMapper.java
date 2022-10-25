package com.ap.microservices.core.product.services;

import com.ap.api.core.product.Product;
import com.ap.microservices.core.product.persistence.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    //Since the entity class does not have a field for serviceAddress,
    // the entityToApi() method is annotated to ignore serviceAddress.
    @Mappings({
            @Mapping(target = "serviceAddress", ignore = true)
    })
    Product entityToApi(ProductEntity entity);

    //The apiToEntity() method is annotated to ignore the id and version fields that are missing in the API model class.
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    ProductEntity apiToEntity(Product api);
}
