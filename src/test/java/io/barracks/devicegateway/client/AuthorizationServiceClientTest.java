/*
 * MIT License
 *
 * Copyright (c) 2017 Barracks Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.barracks.devicegateway.client;

import io.barracks.devicegateway.client.exception.AuthorizationServiceClientException;
import io.barracks.devicegateway.model.ApiKey;
import io.barracks.devicegateway.model.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@RestClientTest(AuthorizationServiceClient.class)
public class AuthorizationServiceClientTest {

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private AuthorizationServiceClient authorizationServiceClient;

    @Test
    public void authenticateApiKey_shouldReturnAUser_whenApiKeyIsCorrect() throws Exception {
        // Given
        final ApiKey key = new ApiKey("AECDF");
        final User renderedUser = new User("aupif", "email");

        mockServer.expect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.OK).body(
                                "{\n" +
                                        "  \"id\": \"" + renderedUser.getId() + "\",\n" +
                                        "  \"firstName\": \" Alfred\",\n" +
                                        "  \"lastName\": \"Dombard\",\n" +
                                        "  \"email\": \"dombard.alfred@caramail.com\"\n" +
                                        "}"
                        ).contentType(MediaType.APPLICATION_JSON)
                );

        // When
        final User result = authorizationServiceClient.authenticateApiKey(key);

        // Then
        mockServer.verify();
        assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", renderedUser.getId());
    }

    @Test
    public void authenticateApiKey_shouldThrowException_whenApiKeyIsIncorrect() throws Exception {
        // Given
        final ApiKey key = new ApiKey("AECDF");
        final HttpStatus status = HttpStatus.UNAUTHORIZED;
        mockServer.expect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(status)
                );

        // Then When
        assertThatExceptionOfType(AuthorizationServiceClientException.class)
                .isThrownBy(() -> authorizationServiceClient.authenticateApiKey(key))
                .withCauseInstanceOf(HttpClientErrorException.class)
                .matches((e) -> e.getCause().getStatusCode().equals(status));
    }

    @Test
    public void authenticateApiKey_shouldThrowException_whenApiKeyIsMissing() throws Exception {
        final HttpStatus status = HttpStatus.BAD_REQUEST;

        // Given
        mockServer.expect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                );

        // Then When
        assertThatExceptionOfType(AuthorizationServiceClientException.class)
                .isThrownBy(() -> authorizationServiceClient.authenticateApiKey(null))
                .withCauseInstanceOf(HttpClientErrorException.class)
                .matches((e) -> e.getCause().getStatusCode().equals(status));
    }

}