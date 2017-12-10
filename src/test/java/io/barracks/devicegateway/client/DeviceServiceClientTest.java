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
import io.barracks.devicegateway.client.exception.DeviceServiceClientException;
import io.barracks.devicegateway.model.DeviceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@RestClientTest(DeviceServiceClient.class)
public class DeviceServiceClientTest {

    private static final String BARRACKS_URL = "http://not.barracks.io";

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private DeviceServiceClient deviceServiceClient;

    @Autowired
    private ObjectMapper mapper;

    @Value("classpath:io/barracks/devicegateway/client/device-info.json")
    private Resource deviceInfo;

    @Test
    public void createDeviceInfo_whenCreationSucceed_shouldReturnAnUpdatedDeviceInfo() throws Exception {
        // Given
        final TimeZone timeZone = TimeZone.getTimeZone("UTC");
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        dateFormat.setTimeZone(timeZone);
        final DeviceInfo expected = mapper.readValue(deviceInfo.getInputStream(), DeviceInfo.class);
        final DeviceInfo query = DeviceInfo.builder()
                .userId(UUID.randomUUID().toString())
                .unitId(UUID.randomUUID().toString())
                .versionId(UUID.randomUUID().toString())
                .build();

        mockServer.expect(method(HttpMethod.POST))
                .andExpect(jsonPath("userId").value(query.getUserId()))
                .andExpect(jsonPath("unitId").value(query.getUnitId()))
                .andExpect(jsonPath("versionId").value(query.getVersionId()))
                .andRespond(
                        withStatus(HttpStatus.CREATED)
                                .body(deviceInfo)
                                .contentType(MediaType.parseMediaType("application/hal+json"))
                );

        // When
        final DeviceInfo result = deviceServiceClient.createDeviceInfo(query);

        // Then
        mockServer.verify();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void createDeviceInfo_whenCreationFail_shouldThrowAnException() {
        // Given
        final DeviceInfo deviceInfo = DeviceInfo.builder().unitId("UnitId").versionId("VersionId").build();
        final HttpStatus status = HttpStatus.BAD_REQUEST;
        mockServer.expect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(status)
                );

        // Then When
        assertThatExceptionOfType(DeviceServiceClientException.class)
                .isThrownBy(() -> deviceServiceClient.createDeviceInfo(deviceInfo))
                .withCauseInstanceOf(HttpClientErrorException.class)
                .matches((e) -> e.getDeviceInfo().equals(deviceInfo))
                .matches((e) -> e.getCause().getStatusCode().equals(status));
    }
}
