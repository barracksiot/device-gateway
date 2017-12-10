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
import io.barracks.devicegateway.client.exception.ComponentServiceClientException;
import io.barracks.devicegateway.model.Version;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.OutputStream;

@Component
@Slf4j
public class ComponentServiceClient {

    static final Endpoint GET_VERSION_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages/{reference}/versions/{id}");
    static final Endpoint GET_VERSION_FILE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/owners/{userId}/packages/{packageRef}/versions/{versionId}/file");

    private final String componentServiceBaseUrl;

    private RestTemplate restTemplate;

    @Autowired
    public ComponentServiceClient(
            @Value("${io.barracks.componentservice.base_url}") String componentServiceBaseUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.componentServiceBaseUrl = componentServiceBaseUrl;
        this.restTemplate = restTemplateBuilder.build();
    }

    public long getVersionFile(String userId, String packageRef, String versionId, OutputStream outputStream) {
        try {
            return restTemplate.execute(
                    GET_VERSION_FILE_ENDPOINT.withBase(componentServiceBaseUrl).getURI(userId, packageRef, versionId),
                    GET_VERSION_FILE_ENDPOINT.getMethod(),
                    request -> request.getHeaders().set(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE),
                    new LargeFileForwardingExtractor(restTemplate.getMessageConverters(), outputStream)
            );
        } catch (HttpStatusCodeException e) {
            throw new ComponentServiceClientException(e);
        }
    }

    public Version getVersion(String userId, String reference, String version) {
        try {
            return restTemplate.exchange(
                    GET_VERSION_ENDPOINT.withBase(componentServiceBaseUrl).getRequestEntity(userId, reference, version),
                    Version.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new ComponentServiceClientException(e);
        }
    }
}

