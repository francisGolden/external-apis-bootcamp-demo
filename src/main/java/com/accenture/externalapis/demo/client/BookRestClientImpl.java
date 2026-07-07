package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.util.Arrays;
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
        } catch (UnknownContentTypeException e){
            throw new ClientException("UnknownContentTypeException. Message: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ClientException("HttpClientErrorException: Not found. Message: " + e.getMessage(), e);
        } catch (HttpClientErrorException e) {
            throw new ClientException("HttpClientErrorException. Message: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("HttpServerErrorException. Message: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("Connection refused / timeout - the external service is unreachable Message: " + e.getMessage(), e);
        }

    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] array = restClient
                    .get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);
            if (array == null){
                return List.of();
            }
            return Arrays
                    .stream(array)
                    .map(book -> new BookDto(book.id(), book.title(), book.author(), book.genre(), book.price()))
                    .toList();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ClientException("HttpClientErrorException: Not found. Message: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("HttpServerErrorException. Message: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("Connection refused / timeout - the external service is unreachable Message: " + e.getMessage(), e);
        }
    }
}
