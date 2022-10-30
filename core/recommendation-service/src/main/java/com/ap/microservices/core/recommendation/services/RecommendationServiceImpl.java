package com.ap.microservices.core.recommendation.services;

import com.ap.api.core.recommendation.Recommendation;
import com.ap.api.core.recommendation.RecommendationService;
import com.ap.microservices.core.recommendation.persistence.RecommendationEntity;
import com.ap.microservices.core.recommendation.persistence.RecommendationRepository;
import com.ap.util.exceptions.InvalidInputException;
import com.ap.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.logging.Level.FINE;

@Slf4j
@RestController
public class RecommendationServiceImpl implements RecommendationService {

    private final ServiceUtil serviceUtil;

    private final RecommendationRepository repository;

    private final RecommendationMapper mapper;

    @Autowired
    public RecommendationServiceImpl(ServiceUtil serviceUtil, RecommendationRepository repository, RecommendationMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {

        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        log.info("Will get recommendations for product with id={}", productId);

        return repository.findByProductId(productId)
                         .log(log.getName(), FINE)
                         .map(e -> mapper.entityToApi(e))
                         .map(e -> setServiceAddress(e));
    }

    @Override
    public Mono<Recommendation> createRecommendation(Recommendation body) {

        if (body.getProductId() < 1) {
            throw new InvalidInputException("Invalid productId: " + body.getProductId());
        }

        RecommendationEntity entity = mapper.apiToEntity(body);
        Mono<Recommendation> newEntity = repository.save(entity)
                                                   .log(log.getName(), FINE)
                                                   .onErrorMap(
                                                           DuplicateKeyException.class,
                                                           ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId()))
                                                   .map(e -> mapper.entityToApi(e));

        return newEntity;
    }

    @Override
    public Mono<Void> deleteRecommendations(int productId) {

        if (productId < 1) {
            throw new InvalidInputException("Invalid productId: " + productId);
        }

        log.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}",
                 productId);
        return repository.deleteAll(repository.findByProductId(productId));
    }

    private Recommendation setServiceAddress(Recommendation e) {
        e.setServiceAddress(serviceUtil.getServiceAddress());
        return e;
    }

}
