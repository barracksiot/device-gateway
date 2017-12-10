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
import io.barracks.devicegateway.client.exception.DeploymentServiceClientException;
import io.barracks.devicegateway.model.DeviceRequest;
import io.barracks.devicegateway.model.ResolvedPackages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import static io.barracks.devicegateway.client.DeploymentServiceClient.RESOLVE_PACKAGES_ENDPOINT;
import static io.barracks.devicegateway.utils.DeviceRequestUtils.getDeviceRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(DeploymentServiceClient.class)
public class DeploymentServiceClientTest {
    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private DeploymentServiceClient client;

    @Autowired
    private ObjectMapper mapper;

    @Value("${io.barracks.deploymentservice.base_url}")
    private String baseUrl;

    @Value("classpath:io/barracks/devicegateway/resolved-packages.json")
    private Resource resolved;

    @Test
    public void resolvePackages_whenRequestSucceeds_shouldReturnResolvedPackages() throws Exception {
        // Given
        final Endpoint endpoint = RESOLVE_PACKAGES_ENDPOINT;
        final DeviceRequest request = getDeviceRequest();
        final ResolvedPackages expected = mapper.readValue(resolved.getInputStream(), ResolvedPackages.class);
        mockServer.expect(requestTo(endpoint.withBase(baseUrl).getURI()))
                .andExpect(method(endpoint.getMethod()))
                .andExpect(content().string(mapper.writeValueAsString(request)))
                .andRespond(withSuccess(resolved, MediaType.APPLICATION_JSON));

        // When
        final ResolvedPackages resolvedPackages = client.resolvePackages(request);

        // Then
        mockServer.verify();
        assertThat(resolvedPackages).isEqualTo(expected);
    }

    @Test
    public void resolvePackages_whenRequestFails_shouldThrowException() throws Exception {
        // Given
        final Endpoint endpoint = RESOLVE_PACKAGES_ENDPOINT;
        final DeviceRequest request = getDeviceRequest();
        mockServer.expect(requestTo(endpoint.withBase(baseUrl).getURI()))
                .andExpect(method(endpoint.getMethod()))
                .andExpect(content().string(mapper.writeValueAsString(request)))
                .andRespond(withBadRequest());

        // Then When
        assertThatExceptionOfType(DeploymentServiceClientException.class)
                .isThrownBy(() -> client.resolvePackages(request));
        mockServer.verify();
    }

}
