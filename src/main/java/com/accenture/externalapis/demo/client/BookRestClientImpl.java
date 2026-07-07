package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
                        throw new ClientException("Client error:" + res.getStatusCode());
                    })
                    .body(BookDto.class);

            if (bookDto == null) {
                throw new ClientException("ClientException: the server responded with 200 OK but the response was null.");
            }

            return bookDto;
        } catch (UnknownContentTypeException e) {
            throw new ClientException("UnknownContentTypeException for the requested resource with id + " + id + ". Message: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ClientException("HttpClientErrorException. Resource with id " + id + " not found. Message: " + e.getMessage(), e);
        } catch (HttpClientErrorException e) {
            throw new ClientException("HttpClientErrorException. Resource id: + " + id + " not found. Message: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("HttpServerErrorException. Resource id: + " + id + "not found. Message: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("Connection refused / timeout - the external service is unreachable. Message: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ClientException("RestClientException. Invalid JSON or other unexpected error during elaboration of requested book with id " + id + ". " + e.getMessage(), e);
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
        }catch (UnknownContentTypeException e) {
            throw new ClientException("UnknownContentTypeException for the books list. Message: " + e.getMessage(), e);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ClientException("HttpClientErrorException. Books endpoint not found. Message: " + e.getMessage(), e);
        } catch (HttpClientErrorException e) {
            throw new ClientException("HttpClientErrorException while fetching books. Message: " + e.getMessage(), e);
        } catch (HttpServerErrorException e) {
            throw new ClientException("HttpServerErrorException while fetching books. Message: " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new ClientException("Connection refused / timeout - the external service is unreachable. Message: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new ClientException("RestClientException. Unexpected error during elaboration of books list request. " + e.getMessage(), e);
        }
    }
}
