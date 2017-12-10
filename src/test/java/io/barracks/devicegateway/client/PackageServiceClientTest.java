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
import io.barracks.devicegateway.client.exception.PackageServiceClientException;
import io.barracks.devicegateway.model.PackageInfo;
import net.minidev.json.parser.ParseException;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static io.barracks.devicegateway.utils.PackageInfoUtils.getPackageInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@RestClientTest(PackageServiceClient.class)
public class PackageServiceClientTest {

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private PackageServiceClient packageServiceClient;

    @Autowired
    private ObjectMapper mapper;

    @Value("classpath:io/barracks/devicegateway/client/package-info.json")
    private Resource packageInfo;

    @Test
    public void getPackageStream_whenPackageServiceFails_shouldThrowException() {
        // Given
        final OutputStream outputStream = new ByteArrayOutputStream();
        final PackageInfo packageInfo = getPackageInfo();
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(header("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        // When Then
        assertThatExceptionOfType(PackageServiceClientException.class).isThrownBy(() -> packageServiceClient.loadPackageStream(packageInfo, outputStream));
        mockServer.verify();
    }

    @Test
    public void getPackageStream_whenPackageNotFound_shouldReturnEmpty() throws IOException {
        // Given
        final OutputStream outputStream = new ByteArrayOutputStream();
        final PackageInfo packageInfo = getPackageInfo();
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(header("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When / Then
        assertThatExceptionOfType(PackageServiceClientException.class).isThrownBy(() -> packageServiceClient.loadPackageStream(packageInfo, outputStream));
        mockServer.verify();
    }

    @Test
    public void getPackageStream_whenPackageAvailable_shouldReturnInputStream() throws IOException {
        // Given
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final byte content[] = new byte[]{1, 2, 3, 4, 5, 6};
        final PackageInfo packageInfo = getPackageInfo();
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(header("Accept", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andRespond(withStatus(HttpStatus.OK).body(content));
        // When Then
        Long result = packageServiceClient.loadPackageStream(packageInfo, outputStream);
        mockServer.verify();
        assertThat(outputStream.toByteArray()).isEqualTo(content);
        assertThat(result).isEqualTo(content.length);
    }

    @Test
    public void getPackageInfo_whenPackageServiceFails_shouldThrowException() {
        // Given
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getHeaders().getAccept().stream().filter(MediaType.APPLICATION_JSON::isCompatibleWith).count()).isNotZero())
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // When Then
        assertThatExceptionOfType(PackageServiceClientException.class).isThrownBy(() -> packageServiceClient.getPackageInfo(UUID.randomUUID().toString()));
        mockServer.verify();

    }

    @Test
    public void getPackageInfo_whenPackageNotFound_shouldReturnEmpty() {
        // Given
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getHeaders().getAccept().stream().filter(MediaType.APPLICATION_JSON::isCompatibleWith).count()).isNotZero())
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        // When / Then
        assertThatExceptionOfType(PackageServiceClientException.class).isThrownBy(() -> packageServiceClient.getPackageInfo(UUID.randomUUID().toString()));
        mockServer.verify();
    }

    @Test
    public void getPackageInfo_whenPackageAvailable_shouldReturnInfo() throws IOException, ParseException {
        // Given
        final PackageInfo expected = mapper.readValue(packageInfo.getInputStream(), PackageInfo.class);
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(request -> assertThat(request.getHeaders().getAccept().stream().filter(MediaType.APPLICATION_JSON::isCompatibleWith).count()).isNotZero())
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .body(packageInfo)
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                );

        // When
        final PackageInfo result = packageServiceClient.getPackageInfo(UUID.randomUUID().toString());

        // Then
        mockServer.verify();
        assertThat(result).isNotNull()
                .isEqualTo(expected);
    }
}
