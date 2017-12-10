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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import io.barracks.commons.util.Endpoint;

@Component
public class AuthorizationServiceClient {

    private static final Endpoint AUTHENTICATE_API_KEY_ENDPOINT = Endpoint.from(HttpMethod.POST, "/device/authenticate");

    private String authorizationServiceBaseUrl;

    private RestTemplate restTemplate;

    public AuthorizationServiceClient(
            @Value("${io.barracks.authorizationservice.base_url}") String authorizationServiceBaseUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.authorizationServiceBaseUrl = authorizationServiceBaseUrl;
        this.restTemplate = restTemplateBuilder.build();
    }

    public User authenticateApiKey(ApiKey apiKey) {
        try {
            final ResponseEntity<User> responseEntity = restTemplate.exchange(
                    AUTHENTICATE_API_KEY_ENDPOINT.withBase(authorizationServiceBaseUrl).body(apiKey).getRequestEntity(),
                    User.class
            );
            return responseEntity.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new AuthorizationServiceClientException(e, apiKey);
        }
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
