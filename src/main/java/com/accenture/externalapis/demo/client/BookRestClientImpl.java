package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.http.HttpStatusCode;
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
            BookDto bookDto = restClient
                    .get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ClientException(
                                "Client error " + res.getStatusCode() + " while fetching book with id " + id);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ClientException(
                                "Server error " + res.getStatusCode() + " while fetching book with id " + id);
                    })
                    .body(BookDto.class);

            if (bookDto == null) {
                throw new ClientException("Server responded 200 OK but body was null for book id " + id);
            }

            return bookDto;
        } catch (UnknownContentTypeException e) {
            throw new ClientException("Unexpected content type for book id " + id + ". " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("Connection refused / timeout - the external service is unreachable for book id " + id, e);
        } catch (RestClientException e) {
            throw new ClientException("Unexpected error while fetching book id " + id + ". " + e.getMessage(), e);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] response = restClient
                    .get()
                    .uri("/books")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ClientException(
                                "Client error " + res.getStatusCode() + " while fetching books list");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ClientException(
                                "Server error " + res.getStatusCode() + " while fetching books list");
                    })
                    .body(BookApiResponse[].class);

            if (response == null) {
                return List.of();
            }

            return Arrays.stream(response)
                    .map(book -> new BookDto(book.id(), book.title(), book.author(), book.genre(), book.price()))
                    .toList();

        } catch (UnknownContentTypeException e) {
            throw new ClientException("Unexpected content type for books list. " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("Connection refused / timeout - the external service is unreachable while fetching books list", e);
        } catch (RestClientException e) {
            throw new ClientException("Unexpected error while fetching books list. " + e.getMessage(), e);
        }
    }

}
