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
import io.barracks.devicegateway.client.exception.DeviceServiceClientException;
import io.barracks.devicegateway.model.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class DeviceServiceClient {

    private static final Endpoint CREATE_DEVICE_ENDPOINT = Endpoint.from(HttpMethod.POST, "/devices");

    private final String deviceServiceBaseUrl;

    private RestTemplate restTemplate;

    public DeviceServiceClient(
            @Value("${io.barracks.deviceservice.base_url}") String deviceServiceUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.deviceServiceBaseUrl = deviceServiceUrl;
    }

    public DeviceInfo createDeviceInfo(DeviceInfo deviceInfo) {
        try {
            log.debug("Creating device '{}'", deviceInfo);
            final ResponseEntity<DeviceInfo> responseEntity = restTemplate.exchange(
                    CREATE_DEVICE_ENDPOINT.withBase(deviceServiceBaseUrl).body(deviceInfo).getRequestEntity(),
                    DeviceInfo.class
            );
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            log.debug("Creation failed with '{}'", e.getResponseBodyAsString());
            throw new DeviceServiceClientException(e, deviceInfo);
        }
    }

}
