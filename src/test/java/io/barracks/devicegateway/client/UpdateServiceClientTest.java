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
import io.barracks.devicegateway.client.exception.UpdateServiceClientException;
import io.barracks.devicegateway.model.Update;
import net.minidev.json.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static io.barracks.devicegateway.client.UpdateServiceClient.LATEST_UPDATE_ENDPOINT;
import static io.barracks.devicegateway.client.UpdateServiceClient.UPDATE_BY_ID_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RunWith(SpringRunner.class)
@RestClientTest(UpdateServiceClient.class)
public class UpdateServiceClientTest {

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private UpdateServiceClient updateServiceClient;

    @Autowired
    private ObjectMapper mapper;

    @Value("classpath:io/barracks/devicegateway/client/update-info.json")
    private Resource updateInfo;

    @Value("${io.barracks.updateservice.base_url}")
    private String baseUrl;

    @Test
    public void getLatestPublishedUpdateByUserIdAndSegmentId_whenUpdateServiceReturnNoContent_shouldReturnEmpty() {
        // Given
        final Endpoint endpoint = LATEST_UPDATE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, segmentId)))
                .andRespond(withNoContent());

        // When
        Optional<Update> result = updateServiceClient.getLatestPublishedUpdateByUserIdAndSegmentId(userId, segmentId);

        // Then
        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    public void getLatestPublishedUpdateByUserIdAndSegmentId_whenUpdateServiceReturnError500_shouldThrownAnException() {
        // Given
        final Endpoint endpoint = LATEST_UPDATE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, segmentId)))
                .andRespond(withServerError());

        // When / Then
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.getLatestPublishedUpdateByUserIdAndSegmentId(userId, segmentId));
        mockServer.verify();
    }

    @Test
    public void getLatestPublishedUpdateByUserIdAndSegmentId_whenUpdateServiceReturnAnUpdate_shouldReturnIt() throws IOException, ParseException {
        // Given
        final Endpoint endpoint = LATEST_UPDATE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        final Update expected = mapper.readValue(updateInfo.getInputStream(), Update.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, segmentId)))
                .andRespond(
                        withSuccess()
                                .body(updateInfo)
                                .contentType(MediaType.APPLICATION_JSON)
                );

        // When
        final Optional<Update> update = updateServiceClient.getLatestPublishedUpdateByUserIdAndSegmentId(userId, segmentId);

        // Then
        mockServer.verify();
        assertThat(update).isPresent().contains(expected);
    }

    @Test
    public void getLatestPublishedUpdateByUserIdAndSegmentId_whenServiceReturn404_shouldThrowHttp404() {
        // Given
        final Endpoint endpoint = LATEST_UPDATE_ENDPOINT;
        final String userId = UUID.randomUUID().toString();
        final String segmentId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(userId, segmentId)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When / Then
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.getLatestPublishedUpdateByUserIdAndSegmentId(userId, segmentId));
        mockServer.verify();
    }

    @Test
    public void getUpdateByUuidAndUserId_whenServiceThrowsAnException_ShouldThrowAnExceptionToo() {
        // Given
        final Endpoint endpoint = UPDATE_BY_ID_ENDPOINT;
        final String uuid = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(uuid, userId)))
                .andRespond(withServerError());

        // When
        assertThatExceptionOfType(UpdateServiceClientException.class)
                .isThrownBy(() -> updateServiceClient.getUpdateByUuidAndUserId(uuid, userId));
        mockServer.verify();
    }

    @Test
    public void getUpdateByUuidAndUserId_whenServiceReturnAnUpdate_shouldReturnItToo() throws Exception {
        // Given
        final Endpoint endpoint = UPDATE_BY_ID_ENDPOINT;
        final String uuid = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final Update expected = mapper.readValue(updateInfo.getInputStream(), Update.class);
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(uuid, userId)))
                .andRespond(
                        withSuccess()
                                .body(updateInfo)
                                .contentType(MediaType.APPLICATION_JSON)
                );

        // When
        final Optional<Update> result = updateServiceClient.getUpdateByUuidAndUserId(uuid, userId);

        // Then
        mockServer.verify();
        assertThat(result).isPresent().contains(expected);
    }

    @Test
    public void getUpdateByUuidAndUserId_whenUpdateServiceReturnNoContent_shouldReturnEmpty() {
        // Given
        final Endpoint endpoint = UPDATE_BY_ID_ENDPOINT;
        final String uuid = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        mockServer.expect(method(endpoint.getMethod()))
                .andExpect(requestTo(endpoint.withBase(baseUrl).getURI(uuid, userId)))
                .andRespond(withNoContent());

        // When
        Optional<Update> result = updateServiceClient.getUpdateByUuidAndUserId(uuid, userId);

        // Then
        assertThat(result).isEmpty();
        mockServer.verify();
    }
}