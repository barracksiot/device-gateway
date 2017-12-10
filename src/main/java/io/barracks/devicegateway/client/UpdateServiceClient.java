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

import io.barracks.commons.util.Endpoint;
import io.barracks.devicegateway.client.exception.UpdateServiceClientException;
import io.barracks.devicegateway.model.Update;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
public class UpdateServiceClient {

    static final Endpoint LATEST_UPDATE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/updates/latest", "userId={userId}&segmentId={segmentId}");
    static final Endpoint UPDATE_BY_ID_ENDPOINT = Endpoint.from(HttpMethod.GET, "/updates/{uuid}", "userId={userId}");

    private String updateServiceBaseUrl;

    private RestTemplate restTemplate;

    @Autowired
    public UpdateServiceClient(
            @Value("${io.barracks.updateservice.base_url}") String updateServiceBaseUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.updateServiceBaseUrl = updateServiceBaseUrl;
        this.restTemplate = restTemplateBuilder.build();
    }

    public Optional<Update> getLatestPublishedUpdateByUserIdAndSegmentId(String userId, String segmentId) {
        try {
            final ResponseEntity<Update> responseEntity = restTemplate.exchange(
                    LATEST_UPDATE_ENDPOINT.withBase(updateServiceBaseUrl).getRequestEntity(userId, segmentId),
                    Update.class
            );
            return Optional.ofNullable(responseEntity.getBody());
        } catch (HttpStatusCodeException e) {
            throw new UpdateServiceClientException(e);
        }
    }

    public Optional<Update> getUpdateByUuidAndUserId(String updateId, String userId) {
        try {
            final ResponseEntity<Update> responseEntity = restTemplate.exchange(
                    UPDATE_BY_ID_ENDPOINT.withBase(updateServiceBaseUrl).getRequestEntity(updateId, userId),
                    Update.class
            );
            return Optional.ofNullable(responseEntity.getBody());
        } catch (HttpStatusCodeException e) {
            throw new UpdateServiceClientException(e);
        }
    }
}
