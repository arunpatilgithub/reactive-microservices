package com.ap.microservices.composite.product.services;

import com.ap.api.core.product.Product;
import com.ap.api.core.product.ProductService;
import com.ap.api.core.recommendation.Recommendation;
import com.ap.api.core.recommendation.RecommendationService;
import com.ap.api.core.review.Review;
import com.ap.api.core.review.ReviewService;
import com.ap.api.event.Event;
import com.ap.util.exceptions.InvalidInputException;
import com.ap.util.exceptions.NotFoundException;
import com.ap.util.http.HttpErrorInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;

import static com.ap.api.event.Event.Type.CREATE;
import static com.ap.api.event.Event.Type.DELETE;
import static reactor.core.publisher.Flux.empty;

@Slf4j
@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private final ObjectMapper mapper;
    private final WebClient webClient;
    private static final String PRODUCT_SERVICE_URL = "http://product";
    private static final String RECOMMENDATION_SERVICE_URL = "http://recommendation";
    private static final String REVIEW_SERVICE_URL = "http://review";

    private final StreamBridge streamBridge;

    private final Scheduler publishEventScheduler;
    @Autowired
    public ProductCompositeIntegration(
            ObjectMapper mapper,
            WebClient.Builder webClient,
            StreamBridge streamBridge,
            @Qualifier("publishEventScheduler") Scheduler publishEventScheduler) {

        this.mapper = mapper;
        this.webClient = webClient.build();
        this.streamBridge = streamBridge;
        this.publishEventScheduler = publishEventScheduler;

    }

    @Override
    public Mono<Product> getProduct(int productId) {
        String url = PRODUCT_SERVICE_URL + "/product/" + productId;
        log.debug("Will call the getProduct API on URL: {}", url);

        return webClient.get().uri(url).retrieve().bodyToMono(Product.class).log().onErrorMap(
                WebClientResponseException.class, ex -> handleException(ex));
    }

    @Override
    public Mono<Product> createProduct(Product body) {
        return Mono.fromCallable(() -> {
            sendMessage("products-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {

        return Mono.fromRunnable(() -> sendMessage("products-out-0", new Event(DELETE, productId, null)))
                   .subscribeOn(publishEventScheduler).then();

    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {

        String url = RECOMMENDATION_SERVICE_URL + "/recommendation?productId=" + productId;

        log.debug("Will call the getRecommendations API on URL: {}", url);

        // Return an empty result if something goes wrong to make it possible for the composite service to return partial responses
        return webClient.get().uri(url).retrieve().bodyToFlux(Recommendation.class).log().onErrorResume(error -> empty());
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {

        return Mono.fromCallable(() -> {
            sendMessage("recommendations-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);
    }

    @Override
    public Mono<Void> deleteRecommendations(int productId) {

        return Mono.fromRunnable(() -> sendMessage("recommendations-out-0", new Event(DELETE, productId, null)))
                   .subscribeOn(publishEventScheduler).then();
    }

    @Override
    public Flux<Review> getReviews(int productId) {

        String url = REVIEW_SERVICE_URL + "/review?productId=" + productId;

        log.debug("Will call the getReviews API on URL: {}", url);

        // Return an empty result if something goes wrong to make it possible for the composite service to return partial responses
        return webClient.get().uri(url).retrieve().bodyToFlux(Review.class).log().onErrorResume(error -> empty());

    }

    @Override
    public Mono<Review> createReview(Review body) {

        return Mono.fromCallable(() -> {
            sendMessage("reviews-out-0", new Event(CREATE, body.getProductId(), body));
            return body;
        }).subscribeOn(publishEventScheduler);

    }

    @Override
    public Mono<Void> deleteReviews(int productId) {

        return Mono.fromRunnable(() -> sendMessage("reviews-out-0", new Event(DELETE, productId, null)))
                   .subscribeOn(publishEventScheduler).then();

    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    private Throwable handleException(Throwable ex) {

        if (!(ex instanceof WebClientResponseException)) {
            log.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
            return ex;
        }

        WebClientResponseException wcre = (WebClientResponseException)ex;

        switch (wcre.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(wcre));

            case UNPROCESSABLE_ENTITY :
                return new InvalidInputException(getErrorMessage(wcre));

            default:
                log.warn("Got a unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
                log.warn("Error body: {}", wcre.getResponseBodyAsString());
                return ex;
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    private void sendMessage(String bindingName, Event event) {
        log.debug("Sending a {} message to {}", event.getEventType(), bindingName);
        Message message = MessageBuilder.withPayload(event)
                                        .setHeader("partitionKey", event.getKey())
                                        .build();
        streamBridge.send(bindingName, message);
    }

}
