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

package io.barracks.devicegateway.rest;

import com.sun.security.auth.UserPrincipal;
import io.barracks.devicegateway.exception.DeviceGatewayException;
import io.barracks.devicegateway.manager.DeviceManager;
import io.barracks.devicegateway.manager.DeviceUpdateManager;
import io.barracks.devicegateway.model.DeviceRequest;
import io.barracks.devicegateway.model.ResolvedVersions;
import io.barracks.devicegateway.utils.DeviceRequestUtils;
import io.barracks.devicegateway.utils.RandomPrincipal;
import io.barracks.devicegateway.utils.ResolvedVersionsUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Principal;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VersionResourceTest {

    private final Principal principal = new RandomPrincipal();

    @Mock
    private DeviceUpdateManager deviceUpdateManager;

    @Mock
    private DeviceManager deviceManager;

    @Mock
    private CounterService counterService;

    @InjectMocks
    @Spy
    private VersionResource versionResource;

    @Test
    public void resolveVersion_shouldUpdateRequestWithUserIdAndUpdateAvailableAndChangedVersionsWithURL_andReturnResponse() {
        // Given
        final String baseUrl = "https://not.barracks.io";
        final HttpServletRequest requestMock = mock(HttpServletRequest.class);
        final String ipData = "1234.123.12.34";
        final String userAgent = UUID.randomUUID().toString();
        doReturn(ipData).when(requestMock).getHeader("X-Forwarded-For");
        doReturn(userAgent).when(requestMock).getHeader("user-agent");
        doReturn(baseUrl).when(versionResource).getBaseUrl(any());
        final DeviceRequest request = DeviceRequestUtils.getDeviceRequest().toBuilder()
                .userId(null)
                .build();
        final DeviceRequest authenticatedRequest = request.toBuilder()
                .userId(principal.getName())
                .ipAddress(ipData)
                .userAgent(userAgent)
                .build();
        final ResolvedVersions managerResponse = ResolvedVersionsUtils.getResolvedVersions();
        doReturn(managerResponse).when(deviceManager).resolveVersions(authenticatedRequest);
        final ResolvedVersions expected = ResolvedVersions.builder()
                .unavailable(managerResponse.getUnavailable())
                .unchanged(managerResponse.getUnchanged())
                .available(
                        managerResponse.getAvailable().stream()
                                .map(version -> version.toBuilder().url(VersionResource.DOWNLOAD_ENDPOINT.withBase(baseUrl).getURI(version.getReference(), version.getVersion()).toString()).build())
                                .collect(Collectors.toList())
                )
                .changed(
                        managerResponse.getChanged().stream()
                                .map(version -> version.toBuilder().url(VersionResource.DOWNLOAD_ENDPOINT.withBase(baseUrl).getURI(version.getReference(), version.getVersion()).toString()).build())
                                .collect(Collectors.toList())
                )
                .build();

        // When
        final ResolvedVersions response = versionResource.resolveVersions(requestMock, request, principal);

        // Then
        verify(requestMock).getHeader("user-agent");
        verify(requestMock).getHeader("X-Forwarded-For");
        verify(versionResource).getBaseUrl(any());
        verify(deviceManager).resolveVersions(authenticatedRequest);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    public void getBaseUrl_whenNotBehindProxyAndIsInsecure() throws MalformedURLException {
        // Given
        final HttpServletRequest requestMock = mock(HttpServletRequest.class);
        final String host = "localhost";
        when(requestMock.getHeader("Host")).thenReturn(host);
        when(requestMock.isSecure()).thenReturn(false);

        // When
        final String result = versionResource.getBaseUrl(requestMock);

        // Then
        assertThat(result)
                .isEqualTo("http://" + host);
    }

    @Test
    public void getBaseUrl_whenNotBehindProxyAndIsSecure() throws MalformedURLException {
        // Given
        final HttpServletRequest requestMock = mock(HttpServletRequest.class);
        final String host = "localhost";
        when(requestMock.getHeader("Host")).thenReturn(host);
        when(requestMock.isSecure()).thenReturn(true);

        // When
        final String result = versionResource.getBaseUrl(requestMock);

        // Then
        assertThat(result)
                .isEqualTo("https://" + host);
    }

    @Test
    public void getBaseUrl_whenBehindProxy() throws MalformedURLException {
        // Given
        final HttpServletRequest requestMock = mock(HttpServletRequest.class);
        final String protocol = "https";
        final String host = "localhost:8080";
        final String prefix = "/api/device";
        when(requestMock.isSecure()).thenReturn(true);
        when(requestMock.getHeader("Host")).thenReturn(host);
        when(requestMock.getHeader("X-Forwarded-Proto")).thenReturn(protocol);
        when(requestMock.getHeader("X-Forwarded-Prefix")).thenReturn(prefix);

        // When
        final String result = versionResource.getBaseUrl(requestMock);

        // Then
        assertThat(result)
                .isEqualTo("https://" + host + prefix);
    }

    @Test
    public void downloadVersion_whenOutputStreamCallFails_shouldThrowException() throws IOException {
        // Given
        final String packageRef = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final UserPrincipal principal = new UserPrincipal(UUID.randomUUID().toString());
        final HttpServletResponse response = mock(HttpServletResponse.class);

        doThrow(IOException.class).when(response).getOutputStream();

        // Then / When
        assertThatExceptionOfType(DeviceGatewayException.class)
                .isThrownBy(() -> versionResource.downloadVersion(packageRef, versionId, principal, response));

    }

    @Test
    public void downloadVersion_whenSuccessful_shouldReturnFile() throws IOException {
        // Given
        final String packageRef = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final UserPrincipal principal = new UserPrincipal(UUID.randomUUID().toString());
        final HttpServletResponse response = new MockHttpServletResponse();

        doReturn(25L).when(deviceUpdateManager).downloadVersion(principal.getName(), packageRef, versionId, response.getOutputStream());
        // When
        versionResource.downloadVersion(packageRef, versionId, principal, response);

        // Then
        verify(deviceUpdateManager).downloadVersion(principal.getName(), packageRef, versionId, response.getOutputStream());
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }
}
