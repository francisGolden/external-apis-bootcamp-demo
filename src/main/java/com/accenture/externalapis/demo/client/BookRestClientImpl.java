package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient {

    private RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        // Optional/bonus: this service doesn't require auth, but in a real API you would
        // often also add builder.defaultHeader("Authorization", "Bearer " + token) here.
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public BookDto getBook(Long id) {
        try {
            return restClient
                    .get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ClientException("HttpClientErrorException: Not found. Message: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("HttpServerErrorException: Not found. Message: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("Connection refused / timeout - the external service is unreachable Message: " + e.getMessage(), e);
        }

    }

    @Override
    public List<BookDto> getAllBooks() {
        return List.of();
    }

    // TODO: Implement getBook(Long id) - fetch one book from GET /books/{id} as a
    // BookApiResponse, then map it onto a BookDto (only keep the fields BookDto needs).
    //
    // TODO: Handle the main RestClient error cases and rethrow them as ClientException:
    //  - HttpClientErrorException (4xx, e.g. book not found)
    //  - HttpServerErrorException (5xx, e.g. the faulty/teapot book)
    //  - ResourceAccessException (connection refused / timeout - the external service is unreachable)

    // TODO: Implement getAllBooks() - fetch all books from GET /books as
    // BookApiResponse[], then map each one onto a BookDto. Handle the same error
    // cases as getBook() above.
}
