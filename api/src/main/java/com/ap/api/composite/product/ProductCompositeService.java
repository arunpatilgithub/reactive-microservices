package com.ap.api.composite.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

public interface ProductCompositeService {

    /**
     * Sample usage: curl $HOST:$PORT/product-composite/1
     *
     * @param productId
     * @return the composite product info, if found, else null
     */
    @Operation(
            summary = "${api.product-composite.get-composite-product.summary}",
            description = "${api.product-composite.get-composite-product.description}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "Bad Request,invalid format of the request. See response message " +
                    "for more information."),
            @ApiResponse(responseCode = "404", description = "Not found, the specified id does not exist."),
            @ApiResponse(responseCode = "422", description = "Unprocessable entity,input parameters caused the processing to fail. See response message for more information.")
    })
    @GetMapping(
            value    = "/product-composite/{productId}",
            produces = "application/json")
    Mono<ProductAggregate> getCompositeProduct(@PathVariable int productId);

    /**
     * Sample usage:
     *
     * curl -X POST $HOST:$PORT/product-composite \
     *   -H "Content-Type: application/json" --data \
     *   '{"productId":123,"name":"product 123","weight":123}'
     *
     * @param body
     */
    @Operation(
            summary = "${api.product-composite.create-composite-product.summary}",
            description = "${api.product-composite.create-composite-product.description}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400",
                    description = "Bad Request, invalid format of the request. See response message for more information."),
            @ApiResponse(responseCode = "422",
                    description = "Unprocessable entity, input parameters caused the processing to fail. See response message for more information.")
    })
    @PostMapping(
            value    = "/product-composite",
            consumes = "application/json")
    void createCompositeProduct(@RequestBody ProductAggregate body);

    /**
     * Sample usage:
     *
     * curl -X DELETE $HOST:$PORT/product-composite/1
     *
     * @param productId
     */
    @Operation(
            summary = "${api.product-composite.delete-composite-product.summary}",
            description = "${api.product-composite.delete-composite-product.description}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "Bad Request,invalid format of the request." +
                    "See response message for more information."),
            @ApiResponse(responseCode = "422", description = "Unprocessable entity, input parameters caused the " +
                    "processing to fail. See response message for more information.")
    })
    @DeleteMapping(value = "/product-composite/{productId}")
    void deleteCompositeProduct(@PathVariable int productId);
}
