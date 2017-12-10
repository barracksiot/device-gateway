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

package io.barracks.devicegateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.devicegateway.client.AuthorizationServiceClient;
import io.barracks.devicegateway.model.ApiKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SecurityIntegrationTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    public AuthorizationServiceClient authorizationServiceClient;

    private MockRestServiceServer mockServer;
    private RestTemplate restTemplate;
    private String serverUrl;

    @Before
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(authorizationServiceClient.getRestTemplate());
        restTemplate = new RestTemplate();
        serverUrl = "http://localhost:" + port;
    }

    @Test
    public void request_shouldReturnForbidden_whenNoApiKeyIsProvided() throws JsonProcessingException {
        // When / Then
        assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(
                () -> restTemplate.exchange(
                        serverUrl + "/whatever",
                        HttpMethod.GET,
                        new HttpEntity<>(new HttpHeaders()),
                        Void.class
                )
        ).withMessage("403 Forbidden");
        mockServer.verify();
    }

    @Test
    public void request_shouldReturnNotFound_whenValidApiKeyIsProvided() throws JsonProcessingException {
        // Given
        final ObjectMapper objectMapper = new ObjectMapper();
        final ApiKey apiKey = new ApiKey("Coucou");
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.put("Authorization", Arrays.asList(apiKey.value()));
        final String authServiceRequestContent = objectMapper.writeValueAsString(apiKey);
        mockServer.expect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.anything())
                .andExpect(content().string(authServiceRequestContent))
                .andRespond(withStatus(HttpStatus.OK));

        // When
        assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(
                () -> restTemplate.exchange(
                        serverUrl + "/whatever",
                        HttpMethod.GET,
                        new HttpEntity<>(requestHeaders),
                        Void.class
                )
        );

        // Then
        mockServer.verify();
    }

    @Test
    public void request_shouldReturnForbidden_whenUnknownApiKeyIsProvided() throws JsonProcessingException {
        // Given
        final ObjectMapper objectMapper = new ObjectMapper();
        final ApiKey apiKey = new ApiKey("Coucou");
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.put("Authorization", Arrays.asList(apiKey.value()));
        final String authServiceRequestContent = objectMapper.writeValueAsString(apiKey);
        mockServer.expect(method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.anything())
                .andExpect(content().string(authServiceRequestContent))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        // When
        assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(
                () -> restTemplate.exchange(
                        serverUrl + "/whatever",
                        HttpMethod.GET,
                        new HttpEntity<>(requestHeaders),
                        Void.class
                )
        ).withMessage("403 Forbidden");

        // Then
        mockServer.verify();
    }

}
