package com.ap.microservices.composite.product.services;

import com.ap.api.composite.product.ProductAggregate;
import com.ap.api.composite.product.ProductCompositeService;
import com.ap.api.composite.product.RecommendationSummary;
import com.ap.api.composite.product.ReviewSummary;
import com.ap.api.composite.product.ServiceAddresses;
import com.ap.api.core.product.Product;
import com.ap.api.core.recommendation.Recommendation;
import com.ap.api.core.review.Review;
import com.ap.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

    private final ServiceUtil serviceUtil;
    private final ProductCompositeIntegration productCompositeIntegration;

    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration productCompositeIntegration) {
        this.serviceUtil = serviceUtil;
        this.productCompositeIntegration = productCompositeIntegration;
    }

    @Override
    public Mono<ProductAggregate> getCompositeProduct(int productId) {
        return Mono.zip(
                           values -> createProductAggregate((Product) values[0], (List<Recommendation>) values[1], (List<Review>) values[2], serviceUtil.getServiceAddress()),
                           productCompositeIntegration.getProduct(productId),
                           productCompositeIntegration.getRecommendations(productId).collectList(),
                           productCompositeIntegration.getReviews(productId).collectList())
                   .doOnError(ex -> log.warn("getCompositeProduct failed: {}", ex.toString()))
                   .log();
    }

    @Override
    public Mono<Void> createCompositeProduct(ProductAggregate body) {

        try {

            List<Mono> monoList = new ArrayList<>();

            log.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

            Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            monoList.add(productCompositeIntegration.createProduct(product));

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
                    monoList.add(productCompositeIntegration.createRecommendation(recommendation));
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach(r -> {
                    Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
                    monoList.add(productCompositeIntegration.createReview(review));
                });
            }

            log.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());

            return Mono.zip(r -> "", monoList.toArray(new Mono[0]))
                       .doOnError(ex -> log.warn("createCompositeProduct failed: {}", ex.toString()))
                       .then();

        } catch (RuntimeException re) {
            log.warn("createCompositeProduct failed: {}", re.toString());
            throw re;
        }

    }

    @Override
    public Mono<Void> deleteProduct(int productId) {

        try {
            log.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", productId);

            return Mono.zip(
                               r -> "",
                               productCompositeIntegration.deleteProduct(productId),
                               productCompositeIntegration.deleteRecommendations(productId),
                               productCompositeIntegration.deleteReviews(productId))
                       .doOnError(ex -> log.warn("delete failed: {}", ex.toString()))
                       .log(log.getName(), FINE).then();

        } catch (RuntimeException re) {
            log.warn("deleteCompositeProduct failed: {}", re.toString());
            throw re;
        }
    }

    private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {

        // 1. Setup product info
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        // 2. Copy summary recommendation info, if available
        List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
                recommendations.stream()
                               .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
                               .collect(Collectors.toList());

        // 3. Copy summary review info, if available
        List<ReviewSummary> reviewSummaries = (reviews == null)  ? null :
                reviews.stream()
                       .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
                       .collect(Collectors.toList());

        // 4. Create info regarding the involved microservices addresses
        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }
}
