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
import io.barracks.devicegateway.manager.DeviceUpdateManager;
import io.barracks.devicegateway.model.DetailedUpdate;
import io.barracks.devicegateway.model.DeviceInfo;
import io.barracks.devicegateway.model.Update;
import io.barracks.devicegateway.model.User;
import io.barracks.devicegateway.rest.entity.DevicePackageInfo;
import io.barracks.devicegateway.rest.entity.DeviceRequestEntity;
import io.barracks.devicegateway.rest.entity.DeviceUpdate;
import io.barracks.devicegateway.utils.RandomPrincipal;
import org.junit.Before;
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
import java.net.URL;
import java.util.UUID;

import static io.barracks.devicegateway.utils.DetailedUpdateUtils.getDetailedUpdate;
import static io.barracks.devicegateway.utils.DeviceRequestEntityUtils.getDeviceRequestEntity;
import static io.barracks.devicegateway.utils.UpdateUtils.getUpdate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UpdateResourceTest {
    @Mock
    private CounterService counter;

    @Mock
    private DeviceUpdateManager deviceUpdateManager;

    @InjectMocks
    @Spy
    private UpdateResource updateResource;

    private RandomPrincipal principal;

    @Before
    public void setUp() throws Exception {
        this.principal = new RandomPrincipal();
    }

    @Test
    public void checkForUpdate_whenAnUpdateIsAvailableAndHeaderPresent_shouldReturnDeviceUpdate() throws Exception {
        // Given
        final User user = principal.getDetails();
        final String userAgent = UUID.randomUUID().toString();
        final URL url = new URL("https", UUID.randomUUID().toString(), "/");
        final HttpServletRequest requestMock = mock(HttpServletRequest.class);
        final String ipData = "1925.168.52.79, 1925.168.54.7, 1925.168.3.121";
        final String deviceIP = "1925.168.52.79";
        final DeviceRequestEntity request = getDeviceRequestEntity().toBuilder().deviceIP(deviceIP).build();
        final DeviceInfo deviceInfo = DeviceInfo.builder()
                .deviceIP(request.getDeviceIP())
                .userAgent(userAgent)
                .versionId(request.getVersionId())
                .additionalProperties(request.getCustomClientData())
                .unitId(request.getUnitId())
                .userId(user.getId())
                .build();
        final DetailedUpdate detailedUpdate = getDetailedUpdate();
        final DeviceUpdate expected = DeviceUpdate.builder()
                .versionId(detailedUpdate.getPackageInfo().getVersionId())
                .packageInfo(DevicePackageInfo.builder()
                        .md5(detailedUpdate.getPackageInfo().getMd5())
                        .url(url.toString())
                        .size(detailedUpdate.getPackageInfo().getSize())
                        .build()
                )
                .customUpdateData(detailedUpdate.getUpdate().getAdditionalProperties())
                .build();

        doReturn(ipData).when(requestMock).getHeader("X-Forwarded-For");
        doReturn(userAgent).when(requestMock).getHeader("user-agent");
        doReturn(detailedUpdate).when(deviceUpdateManager).checkForUpdate(deviceInfo);
        doReturn(url).when(updateResource).buildDownloadUrl(requestMock, detailedUpdate.getUpdate());

        // When
        final DeviceUpdate result = updateResource.checkForUpdate(requestMock, request, principal);

        // Then
        verify(requestMock).getHeader("user-agent");
        verify(deviceUpdateManager).checkForUpdate(deviceInfo);
        verify(updateResource).checkForUpdate(requestMock, request, principal);
        verify(updateResource).buildDownloadUrl(requestMock, detailedUpdate.getUpdate());
        verifyNoMoreInteractions(updateResource);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void checkForUpdate_whenAnUpdateIsAvailableAndHeaderAbsent_shouldReturnDeviceUpdate() throws Exception {
        // Given
        final User user = principal.getDetails();
        final String userAgent = UUID.randomUUID().toString();
        final URL url = new URL("https", UUID.randomUUID().toString(), "/");

        final HttpServletRequest requestMock = mock(HttpServletRequest.class);
        final String deviceIP = "1925.168.52.79";
        final DeviceRequestEntity request = getDeviceRequestEntity().toBuilder().deviceIP(deviceIP).build();
        final DeviceInfo deviceInfo = DeviceInfo.builder()
                .userId(user.getId())
                .unitId(request.getUnitId())
                .versionId(request.getVersionId())
                .additionalProperties(request.getCustomClientData())
                .deviceIP(deviceIP)
                .userAgent(userAgent)
                .build();
        final DetailedUpdate detailedUpdate = getDetailedUpdate();
        final DeviceUpdate deviceUpdate = DeviceUpdate.builder()
                .versionId(detailedUpdate.getPackageInfo().getVersionId())
                .packageInfo(DevicePackageInfo.builder()
                        .url(url.toString())
                        .md5(detailedUpdate.getPackageInfo().getMd5())
                        .size(detailedUpdate.getPackageInfo().getSize())
                        .build()
                )
                .customUpdateData(detailedUpdate.getUpdate().getAdditionalProperties())
                .build();
        doReturn(deviceIP).when(requestMock).getRemoteAddr();
        doReturn(userAgent).when(requestMock).getHeader("user-agent");
        doReturn(detailedUpdate).when(deviceUpdateManager).checkForUpdate(deviceInfo);
        doReturn(url).when(updateResource).buildDownloadUrl(requestMock, detailedUpdate.getUpdate());

        // When
        final DeviceUpdate result = updateResource.checkForUpdate(requestMock, request, principal);

        // Then
        verify(requestMock).getHeader("user-agent");
        verify(deviceUpdateManager).checkForUpdate(deviceInfo);
        verify(updateResource).checkForUpdate(requestMock, request, principal);
        verify(updateResource).buildDownloadUrl(requestMock, detailedUpdate.getUpdate());
        verifyNoMoreInteractions(updateResource);
        assertThat(result).isEqualTo(deviceUpdate);
    }


    @Test
    public void downloadUpdate_whenStreamIsAvailable_ShouldReturnFile() throws Exception {
        // Given
        final String updateId = UUID.randomUUID().toString();
        final UserPrincipal principal = new UserPrincipal(UUID.randomUUID().toString());
        final HttpServletResponse response = new MockHttpServletResponse();

        doReturn(123L).when(deviceUpdateManager).downloadUpdate(principal.getName(), updateId, response.getOutputStream());

        // When
        updateResource.downloadUpdate(updateId, principal, response);

        // Then
        verify(deviceUpdateManager).downloadUpdate(principal.getName(), updateId, response.getOutputStream());

        // TODO
        //      verify(response).setContentLength((int)123L);
        //      verify(response).flushBuffer();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    public void downloadUpdate_whenStreamIsNotAvailable_ShouldThrowException() throws Exception {
        // Given
        final String updateId = UUID.randomUUID().toString();
        final UserPrincipal principal = new UserPrincipal(UUID.randomUUID().toString());
        final HttpServletResponse response = mock(HttpServletResponse.class);
        doThrow(IOException.class).when(response).getOutputStream();

        // Then / When
        assertThatExceptionOfType(DeviceGatewayException.class)
                .isThrownBy(() -> updateResource.downloadUpdate(updateId, principal, response));
    }

    @Test
    public void buildDownloadUrl_whenNotBehindProxyAndIsInsecure() throws MalformedURLException {
        // Given
        final Update update = getUpdate();
        final HttpServletRequest requestMock = mock(HttpServletRequest.class);
        final String host = "localhost";
        when(requestMock.getHeader("Host")).thenReturn(host);
        when(requestMock.isSecure()).thenReturn(false);

        // When
        final URL result = updateResource.buildDownloadUrl(requestMock, update);

        // Then
        assertThat(result)
                .isNotNull()
                .hasToString("http://" + host + "/update/download/" + update.getUuid());
    }

    @Test
    public void buildDownloadUrl_whenNotBehindProxyAndIsSecure() throws MalformedURLException {
        // Given
        final Update update = getUpdate();
        final HttpServletRequest requestMock = mock(HttpServletRequest.class);
        final String host = "localhost";
        when(requestMock.getHeader("Host")).thenReturn(host);
        when(requestMock.isSecure()).thenReturn(true);

        // When
        final URL result = updateResource.buildDownloadUrl(requestMock, update);

        // Then
        assertThat(result)
                .isNotNull()
                .hasToString("https://" + host + "/update/download/" + update.getUuid());
    }

    @Test
    public void buildDownloadUrl_whenBehindProxy() throws MalformedURLException {
        // Given
        final Update update = getUpdate();
        final HttpServletRequest requestMock = mock(HttpServletRequest.class);
        final String protocol = "https";
        final String host = "localhost:8080";
        final String prefix = "/api/device";
        when(requestMock.isSecure()).thenReturn(true);
        when(requestMock.getHeader("Host")).thenReturn(host);
        when(requestMock.getHeader("X-Forwarded-Proto")).thenReturn(protocol);
        when(requestMock.getHeader("X-Forwarded-Prefix")).thenReturn(prefix);

        // When
        final URL result = updateResource.buildDownloadUrl(requestMock, update);

        // Then
        assertThat(result)
                .isNotNull()
                .hasToString("https://" + host + prefix + "/update/download/" + update.getUuid());
    }
}