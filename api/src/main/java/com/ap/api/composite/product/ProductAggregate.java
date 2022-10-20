package com.ap.api.composite.product;

import lombok.Value;

import java.util.List;

@Value
public class ProductAggregate {
    int productId;
    String name;
    int weight;
    List<RecommendationSummary> recommendations;
    List<ReviewSummary> reviews;
    ServiceAddresses serviceAddresses;

    public ProductAggregate(
            int productId,
            String name,
            int weight,
            List<RecommendationSummary> recommendations,
            List<ReviewSummary> reviews,
            ServiceAddresses serviceAddresses) {

        this.productId = productId;
        this.name = name;
        this.weight = weight;
        this.recommendations = recommendations;
        this.reviews = reviews;
        this.serviceAddresses = serviceAddresses;
    }

}
