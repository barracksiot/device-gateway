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

package io.barracks.devicegateway.model.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.devicegateway.model.DeviceRequest;
import io.barracks.devicegateway.model.Package;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.stream.IntStream;

import static io.barracks.devicegateway.utils.DeviceRequestUtils.getDeviceRequest;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class DeviceRequestTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JacksonTester<DeviceRequest> json;

    @Value("classpath:io/barracks/devicegateway/device-package-request.json")
    private Resource request;

    @Test
    public void deserialize_shouldMapCustomClientDataAndIgnoreUserId() throws Exception {
        // Given
        final DeviceRequest expected = DeviceRequest.builder()
                .userId("user")
                .unitId("OhOhOh")
                .addCustomClientData("boolean", Boolean.TRUE)
                .addCustomClientData("number", Math.PI)
                .addCustomClientData("string", "deadbeef")
                .addPackage(Package.builder()
                        .reference("abc.def")
                        .version("0.0.1")
                        .build()
                )
                .build();

        // When
        final DeviceRequest result = objectMapper.readValue(request.getInputStream(), DeviceRequest.class);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void serialize_shouldIncludeUserIdAndIncludeAdditionalProperties() throws Exception {
        // Given
        final DeviceRequest request = getDeviceRequest();

        // When
        JsonContent<DeviceRequest> result = json.write(request);

        // Then
        assertThat(result).extractingJsonPathStringValue("@.userId").isEqualTo(request.getUserId());
        assertThat(result).extractingJsonPathStringValue("@.unitId").isEqualTo(request.getUnitId());
        request.getCustomClientData().forEach(
                (key, value) -> assertThat(result).extractingJsonPathValue("@.customClientData." + key).isEqualTo(value)
        );
        IntStream.range(0, request.getPackages().size()).forEach(
                idx -> {
                    final Package aPackage = request.getPackages().get(idx);
                    assertThat(result).extractingJsonPathValue("@.packages[" + idx + "].reference").isEqualTo(aPackage.getReference());
                    assertThat(result).extractingJsonPathValue("@.packages[" + idx + "].version").isEqualTo(aPackage.getVersion().get());
                }
        );
    }
}
