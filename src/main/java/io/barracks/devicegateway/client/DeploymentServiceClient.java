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
import io.barracks.devicegateway.client.exception.DeploymentServiceClientException;
import io.barracks.devicegateway.model.DeviceRequest;
import io.barracks.devicegateway.model.ResolvedPackages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class DeploymentServiceClient {
    static final Endpoint RESOLVE_PACKAGES_ENDPOINT = Endpoint.from(HttpMethod.POST, "/packages/resolve");

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public DeploymentServiceClient(
            @Value("${io.barracks.deploymentservice.base_url}") String baseUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.baseUrl = baseUrl;
    }

    public ResolvedPackages resolvePackages(DeviceRequest request) {
        try {
            return restTemplate.exchange(
                    RESOLVE_PACKAGES_ENDPOINT.withBase(baseUrl).body(request).getRequestEntity(),
                    ResolvedPackages.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new DeploymentServiceClientException(e);
        }
    }

}
