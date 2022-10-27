package com.ap.microservices.core.product;

import com.ap.microservices.core.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {"spring.data.mongodb.port: 0"})
class ProductServiceApplicationTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	private ProductRepository repository;

	@BeforeEach
	public void setupDb() {
		repository.deleteAll();
	}

	@Test
	public void getProductById() {

	}

	@Test
	public void duplicateError() {


	}

	@Test
	public void deleteProduct() {

	}

	@Test
	public void getProductInvalidParameterString() {

	}

	@Test
	public void getProductNotFound() {

	}

	@Test
	public void getProductInvalidParameterNegativeValue() {

	}


}
