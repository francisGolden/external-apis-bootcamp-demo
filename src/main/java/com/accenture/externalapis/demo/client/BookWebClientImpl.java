package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.springframework.core.codec.DecodingException;

@Component
public class BookWebClientImpl implements BookWebClient {

    private WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder.baseUrl(properties.baseUrl()).build();
        // Optional/bonus: this service doesn't require auth, but in a real API you would
        // often also add builder.defaultHeader("Authorization", "Bearer " + token) here.
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return fetchBookSafely(id);
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return webClient.get()
                .uri("/books")
                .retrieve()
                .bodyToFlux(BookDto.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorMap(WebClientResponseException.NotFound.class, e ->
                        new ClientException("Books endpoint not found. Message: " + e.getMessage(), e))
                .onErrorMap(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().is4xxClientError()) {
                        return new ClientException("Client error (4xx) while fetching books list. Message: " + e.getMessage(), e);
                    }
                    return new ClientException("Server error (5xx) while fetching books list. Message: " + e.getMessage(), e);
                })
                .onErrorMap(WebClientRequestException.class, e ->
                        new ClientException("Connection refused / timeout - the external service is unreachable. Message: " + e.getMessage(), e))
                .onErrorMap(DecodingException.class, e ->
                        new ClientException("Decoding error / invalid JSON for the books list. Message: " + e.getMessage(), e))
                .onErrorMap(TimeoutException.class, e ->
                        new ClientException("Timeout while fetching books list.", e))
                .onErrorMap(e -> !(e instanceof ClientException), e ->
                        new ClientException("Unexpected error in the reactive stream for the books list. Message: " + e.getMessage(), e));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        Mono<BookDto> bookDtoMono = fetchBookSafely(id1);
        Mono<BookDto> bookDtoMono2 = fetchBookSafely(id2);

        return Mono.zip(bookDtoMono, bookDtoMono2)
                .map(tuple -> List.of(
                        tuple.getT1(),
                        tuple.getT2()
                ));
    }

    private Mono<BookDto> fetchBookSafely(Long id) {
        return webClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .bodyToMono(BookDto.class)
                .timeout(Duration.ofSeconds(3))
                .retry(3)
                .onErrorMap(DecodingException.class, e ->
                        new ClientException("DecodingException for the requested resource with id " + id + ". Message: " + e.getMessage(), e)
                )
                .onErrorMap(WebClientResponseException.NotFound.class, e ->
                        new ClientException("WebClientResponseException. Resource with id " + id + " not found. Message: " + e.getMessage(), e)
                )
                .onErrorMap(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().is4xxClientError()) {
                        return new ClientException("Client Error (4xx). Resource id: " + id + ". Message: " + e.getMessage(), e);
                    } else if (e.getStatusCode().is5xxServerError()) {
                        return new ClientException("Server Error (5xx). Resource id: " + id + ". Message: " + e.getMessage(), e);
                    }
                    return new ClientException("WebClientResponseException. Unexpected HTTP error for id " + id + ". " + e.getMessage(), e);
                })
                .onErrorMap(WebClientRequestException.class, e ->
                        new ClientException("Connection refused / timeout - the external service is unreachable. Message: " + e.getMessage(), e)
                ).onErrorMap(TimeoutException.class, e -> new ClientException("TimeoutException. The request resource did not receive a response in a timely manner."));
    }
}
