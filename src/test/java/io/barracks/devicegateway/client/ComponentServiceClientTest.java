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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.commons.util.Endpoint;
import io.barracks.devicegateway.client.exception.ComponentServiceClientException;
import io.barracks.devicegateway.model.Version;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static io.barracks.devicegateway.client.ComponentServiceClient.GET_VERSION_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(ComponentServiceClient.class)
public class ComponentServiceClientTest {

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private ComponentServiceClient componentServiceClient;

    @Autowired
    private ObjectMapper mapper;

    @Value("classpath:io/barracks/devicegateway/client/version.bin")
    private Resource versionFile;

    @Value("classpath:io/barracks/devicegateway/client/version.json")
    private Resource version;

    @Value("${io.barracks.componentservice.base_url}")
    private String baseUrl;

    @Test
    public void getVersionFile_whenComponentServiceFails_shouldThrowException() {
        // Given
        final String componentRef = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final OutputStream outputStream = new ByteArrayOutputStream();
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(header("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // When Then
        assertThatExceptionOfType(ComponentServiceClientException.class)
                .isThrownBy(() -> componentServiceClient.getVersionFile(componentRef, versionId, userId, outputStream));
        mockServer.verify();
    }

    @Test
    public void getVersionFile_whenVersionNotFound_shouldThrowException() {
        final String componentRef = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final OutputStream outputStream = new ByteArrayOutputStream();
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(header("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When Then
        assertThatExceptionOfType(ComponentServiceClientException.class)
                .isThrownBy(() -> componentServiceClient.getVersionFile(componentRef, versionId, userId, outputStream));
        mockServer.verify();
    }

    @Test
    public void getVersionFile_whenVersionFound_shouldLoadVersionFile() throws IOException {
        // Given
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String userId = UUID.randomUUID().toString();
        final String reference = UUID.randomUUID().toString();
        final String version = UUID.randomUUID().toString();
        final byte content[] = new byte[]{1, 2, 3, 4, 5, 6};

        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        mockServer.expect(method(HttpMethod.GET))
                .andExpect(header("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .headers(headers)
                                .body(content)
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                );

        // When
        final long result = componentServiceClient.getVersionFile(version, userId, reference, outputStream);

        // Then
        mockServer.verify();
        assertThat(outputStream.toByteArray()).isEqualTo(content);
        assertThat(result).isEqualTo(content.length);
    }

    @Test
    public void getVersion_whenVersionExists_shouldReturnVersion() throws Exception {
        // Given
        final Endpoint endpoint = GET_VERSION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String reference = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final Version expected = mapper.readValue(version.getInputStream(), Version.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, reference, versionId)))
                .andRespond(withSuccess(version, MediaType.APPLICATION_JSON));

        // When
        final Version result = componentServiceClient.getVersion(userId, reference, versionId);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getVersion_whenError_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = GET_VERSION_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String reference = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, reference, versionId)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // Then / When
        assertThatExceptionOfType(ComponentServiceClientException.class)
                .isThrownBy(() -> componentServiceClient.getVersion(userId, reference, versionId));
        mockServer.verify();
    }
}
