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

package io.barracks.devicegateway.manager;

import io.barracks.devicegateway.Application;
import io.barracks.devicegateway.client.*;
import io.barracks.devicegateway.client.exception.DeviceServiceClientException;
import io.barracks.devicegateway.client.exception.PackageServiceClientException;
import io.barracks.devicegateway.client.exception.UpdateServiceClientException;
import io.barracks.devicegateway.exception.DeviceGatewayException;
import io.barracks.devicegateway.exception.InvalidUpdateStatusException;
import io.barracks.devicegateway.exception.NotPackageOwnerException;
import io.barracks.devicegateway.exception.PackageStreamException;
import io.barracks.devicegateway.exception.NoUpdateAvailableException;
import io.barracks.devicegateway.model.*;
import org.assertj.core.util.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Optional;
import java.util.UUID;

import static io.barracks.devicegateway.utils.DeviceInfoUtils.getDeviceInfo;
import static io.barracks.devicegateway.utils.PackageInfoUtils.getPackageInfo;
import static io.barracks.devicegateway.utils.UpdateUtils.getUpdate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest(classes = Application.class)
public class DeviceUpdateManagerTest {

    @Mock
    private DeviceServiceClient deviceServiceClient;

    @Mock
    private PackageServiceClient packageServiceClient;

    @Mock
    private UpdateServiceClient updateServiceClient;

    @Mock
    private QueuingServiceClient queuingServiceClient;

    @Mock
    private ComponentServiceClient componentServiceClient;

    @InjectMocks
    @Spy
    private DeviceUpdateManager deviceUpdateManager;

    @Test
    public void createDeviceInfo_whenDeviceServiceClientCreatesSucceeds_shouldReturnDeviceInfo() {
        // Given
        final DeviceInfo clientResponse = getDeviceInfo();
        final DeviceInfo deviceInfo = getDeviceInfo();
        when(deviceServiceClient.createDeviceInfo(deviceInfo)).thenReturn(clientResponse);

        // When
        final DeviceInfo result = deviceUpdateManager.createDeviceInfo(deviceInfo);

        // Then
        verify(deviceServiceClient).createDeviceInfo(deviceInfo);
        verify(deviceUpdateManager).createDeviceInfo(deviceInfo);
        verifyNoMoreInteractions(deviceUpdateManager);
        assertThat(result).isEqualTo(clientResponse);
    }

    @Test
    public void createDeviceInfo_whenDeviceServiceClientCreateThrowsAnException_shouldThrowAnExceptionToo() {
        // Given
        final DeviceInfo deviceInfo = getDeviceInfo();
        final DeviceInfo expectedToBeCreated = getDeviceInfo();
        final DeviceServiceClientException clientException = new DeviceServiceClientException(
                new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR),
                expectedToBeCreated
        );
        when(deviceServiceClient.createDeviceInfo(deviceInfo)).thenThrow(clientException);

        // Then
        assertThatExceptionOfType(DeviceGatewayException.class)
                .isThrownBy(() -> deviceUpdateManager.createDeviceInfo(deviceInfo))
                .withCause(clientException);
        verify(deviceServiceClient).createDeviceInfo(deviceInfo);
        verify(deviceUpdateManager).createDeviceInfo(deviceInfo);
        verifyNoMoreInteractions(deviceUpdateManager);
    }

    @Test
    public void getPackageStream_shouldReturnTheStreamReturnedByPackageServiceClient() throws Exception {
        // Given
        final OutputStream outputStream = new ByteArrayOutputStream();
        final PackageInfo packageInfo = getPackageInfo();
        when(packageServiceClient.loadPackageStream(packageInfo, outputStream)).thenReturn(packageInfo.getSize());

        // When
        final Long result = deviceUpdateManager.loadPackageStream(packageInfo, outputStream);

        // Then
        verify(packageServiceClient).loadPackageStream(packageInfo, outputStream);
        verify(deviceUpdateManager).loadPackageStream(packageInfo, outputStream);
        verifyNoMoreInteractions(deviceUpdateManager);
        assertThat(result).isEqualTo(packageInfo.getSize());
    }

    @Test
    public void getPackageStream_whenServiceClientThrowAnIOException_shouldThrowAnException() throws Exception {
        // Given
        final OutputStream outputStream = new ByteArrayOutputStream();
        final PackageInfo packageInfo = getPackageInfo();
        when(packageServiceClient.loadPackageStream(packageInfo, outputStream)).thenThrow(new IOException());

        // When / Then
        assertThatExceptionOfType(PackageStreamException.class).
                isThrownBy(() -> deviceUpdateManager.loadPackageStream(packageInfo, outputStream));
        verify(packageServiceClient).loadPackageStream(packageInfo, outputStream);
        verify(deviceUpdateManager).loadPackageStream(packageInfo, outputStream);
        verifyNoMoreInteractions(deviceUpdateManager);
    }

    @Test
    public void getPackageStream_whenItThrowAPackageServiceClientException_shouldThrowTheSameExceptionThanTheClient() throws Exception {
        // Given
        final OutputStream outputStream = new ByteArrayOutputStream();
        final PackageInfo packageInfo = getPackageInfo();
        when(packageServiceClient.loadPackageStream(packageInfo, outputStream))
                .thenThrow(new PackageServiceClientException(new HttpClientErrorException(HttpStatus.NOT_FOUND)));

        // When / Then
        assertThatExceptionOfType(PackageServiceClientException.class).
                isThrownBy(() -> deviceUpdateManager.loadPackageStream(packageInfo, outputStream));
        verify(packageServiceClient).loadPackageStream(packageInfo, outputStream);
        verify(deviceUpdateManager).loadPackageStream(packageInfo, outputStream);
        verifyNoMoreInteractions(deviceUpdateManager);
    }

    @Test
    public void getPackageInfoByPackageIdAndUserId_whenUserIdIsTheOwner_shouldReturnThePackageInfoFromTheClient() throws Exception {
        // Given
        final PackageInfo packageInfo = getPackageInfo();
        when(packageServiceClient.getPackageInfo(packageInfo.getId())).thenReturn(packageInfo);

        // When
        final PackageInfo result = deviceUpdateManager.getPackageInfoByPackageIdAndUserId(packageInfo.getId(), packageInfo.getUserId());

        // Then
        assertThat(result).isNotNull().isEqualTo(packageInfo);
        verify(deviceUpdateManager).getPackageInfoByPackageIdAndUserId(packageInfo.getId(), packageInfo.getUserId());
        verifyNoMoreInteractions(deviceUpdateManager);

    }

    @Test
    public void getPackageInfoByPackageIdAndUserId_whenUserIdIsNotTheOwner_shouldThrowAnException() throws Exception {
        // Given
        final String userId = "456";
        final PackageInfo packageInfo = getPackageInfo();
        when(packageServiceClient.getPackageInfo(packageInfo.getId())).thenReturn(packageInfo);

        // When / Then
        assertThatExceptionOfType(NotPackageOwnerException.class).
                isThrownBy(() -> deviceUpdateManager.getPackageInfoByPackageIdAndUserId(packageInfo.getId(), userId));
        verify(packageServiceClient).getPackageInfo(packageInfo.getId());
        verify(deviceUpdateManager).getPackageInfoByPackageIdAndUserId(packageInfo.getId(), userId);
        verifyNoMoreInteractions(deviceUpdateManager);
    }

    @Test
    public void getPackageInfoByPackageIdAndUserId_whenClientThrowOne_shouldThrowBarracksServiceClientException() throws Exception {
        // Given
        final String packageId = "123";
        final String userId = "456";
        when(packageServiceClient.getPackageInfo(packageId))
                .thenThrow(new PackageServiceClientException(new HttpClientErrorException(HttpStatus.NOT_FOUND)));

        // When / Then
        assertThatExceptionOfType(PackageServiceClientException.class).
                isThrownBy(() -> deviceUpdateManager.getPackageInfoByPackageIdAndUserId(packageId, userId));
        verify(packageServiceClient).getPackageInfo(packageId);
        verify(deviceUpdateManager).getPackageInfoByPackageIdAndUserId(packageId, userId);
        verifyNoMoreInteractions(deviceUpdateManager);
    }

    @Test
    public void getPackageInfoByUpdateIdAndUserId_whenUpdateIsArchived_shouldReturnHttp404() throws Exception {
        // Given
        final String updateId = UUID.randomUUID().toString();
        final PackageInfo packageInfo = getPackageInfo();
        final Update update = Update.builder().
                status(UpdateStatus.ARCHIVED).
                packageId(packageInfo.getId()).
                userId(packageInfo.getUserId())
                .build();
        final Optional<Update> opt = Optional.of(update);
        when(updateServiceClient.getUpdateByUuidAndUserId(updateId, packageInfo.getUserId())).thenReturn(opt);

        // When / Then
        assertThatExceptionOfType(InvalidUpdateStatusException.class).
                isThrownBy(() -> deviceUpdateManager.getPackageInfoByUpdateIdAndUserId(updateId, packageInfo.getUserId()));
        verify(updateServiceClient).getUpdateByUuidAndUserId(updateId, packageInfo.getUserId());
        verify(deviceUpdateManager).getPackageInfoByUpdateIdAndUserId(updateId, packageInfo.getUserId());
        verifyNoMoreInteractions(deviceUpdateManager);
    }

    @Test
    public void getPackageInfoByUpdateIdAndUserId_whenUpdateIsDraft_shouldReturnHttp404() throws Exception {
        // Given
        final String updateId = UUID.randomUUID().toString();
        final PackageInfo packageInfo = getPackageInfo();
        final Update update = Update.builder().
                status(UpdateStatus.DRAFT).
                packageId(packageInfo.getId()).
                userId(packageInfo.getUserId())
                .build();
        final Optional<Update> opt = Optional.of(update);

        when(updateServiceClient.getUpdateByUuidAndUserId(updateId, packageInfo.getUserId())).thenReturn(opt);

        // When / Then
        assertThatExceptionOfType(InvalidUpdateStatusException.class).
                isThrownBy(() -> deviceUpdateManager.getPackageInfoByUpdateIdAndUserId(updateId, packageInfo.getUserId()));
        verify(updateServiceClient).getUpdateByUuidAndUserId(updateId, packageInfo.getUserId());
        verify(deviceUpdateManager).getPackageInfoByUpdateIdAndUserId(updateId, packageInfo.getUserId());
        verifyNoMoreInteractions(deviceUpdateManager);
    }

    @Test
    public void getPackageInfoByUpdateIdAndUserId_whenUserIdIsTheOwner_shouldReturnThePackageInfoFromTheClient() throws Exception {
        // Given
        final PackageInfo packageInfo = getPackageInfo();
        final Update update = Update.builder()
                .packageId(packageInfo.getId())
                .userId(packageInfo.getUserId())
                .status(UpdateStatus.PUBLISHED)
                .build();
        final Optional<Update> opt = Optional.of(update);

        when(updateServiceClient.getUpdateByUuidAndUserId(update.getUuid(), packageInfo.getUserId())).thenReturn(opt);
        when(packageServiceClient.getPackageInfo(packageInfo.getId())).thenReturn(packageInfo);

        // When
        final PackageInfo result = deviceUpdateManager.getPackageInfoByUpdateIdAndUserId(update.getUuid(), packageInfo.getUserId());

        // Then
        assertThat(result).isNotNull().isEqualTo(packageInfo);
        verify(deviceUpdateManager).getPackageInfoByUpdateIdAndUserId(update.getUuid(), packageInfo.getUserId());

    }

    @Test
    public void getPackageInfoByUpdateIdAndUserId_whenUserIdIsNotTheOwnerOfTheUpdate_shouldThrowAnException() throws Exception {
        // Given
        final PackageInfo packageInfo = getPackageInfo();
        final Update update = Update.builder()
                .userId("anotherUser")
                .build();
        when(updateServiceClient.getUpdateByUuidAndUserId(update.getUuid(), packageInfo.getUserId()))
                .thenThrow(new UpdateServiceClientException(new HttpClientErrorException(HttpStatus.NOT_FOUND)));

        // When / Then
        assertThatExceptionOfType(UpdateServiceClientException.class).
                isThrownBy(() -> deviceUpdateManager.getPackageInfoByUpdateIdAndUserId(update.getUuid(), packageInfo.getUserId()));
        verify(updateServiceClient).getUpdateByUuidAndUserId(update.getUuid(), packageInfo.getUserId());
    }

    @Test
    public void getPackageInfoByUpdateIdAndUserId_whenUserIdIsNotTheOwnerOfThePackage_shouldThrowAnException() throws Exception {
        // Given
        final Update update = Update.builder()
                .build();
        when(updateServiceClient.getUpdateByUuidAndUserId(update.getUuid(), update.getUserId()))
                .thenThrow(new UpdateServiceClientException(new HttpClientErrorException(HttpStatus.NOT_FOUND)));

        // When / Then
        assertThatExceptionOfType(UpdateServiceClientException.class).
                isThrownBy(() -> deviceUpdateManager.getPackageInfoByUpdateIdAndUserId(update.getUuid(), update.getUserId()));
        verify(updateServiceClient).getUpdateByUuidAndUserId(update.getUuid(), update.getUserId());
    }

    @Test
    public void getPackageInfoByUpdateIdAndUserId_whenClientThrowOne_shouldThrowBarracksServiceClientException() throws Exception {
        // Given
        final PackageInfo packageInfo = getPackageInfo();
        final Update update = Update.builder()
                .packageId(packageInfo.getId())
                .userId(packageInfo.getUserId())
                .build();
        when(updateServiceClient.getUpdateByUuidAndUserId(update.getUuid(), packageInfo.getUserId()))
                .thenThrow(new UpdateServiceClientException(new HttpClientErrorException(HttpStatus.NOT_FOUND)));

        // When / Then
        assertThatExceptionOfType(UpdateServiceClientException.class).
                isThrownBy(() -> deviceUpdateManager.getPackageInfoByUpdateIdAndUserId(update.getUuid(), packageInfo.getUserId()));
        verify(updateServiceClient).getUpdateByUuidAndUserId(update.getUuid(), packageInfo.getUserId());
    }

    @Test
    public void checkForUpdate_whenUpdateIsAvailable_shouldReturnedDetailedUpdate() throws MalformedURLException {
        // Given
        final DeviceInfo deviceInfo = getDeviceInfo();
        final DeviceInfo savedDeviceInfo = getDeviceInfo();
        final PackageInfo packageInfo = getPackageInfo();
        final Update latestUpdate = Update.builder()
                .packageId(packageInfo.getId())
                .userId(packageInfo.getUserId())
                .additionalProperties(Maps.newHashMap("key", "value"))
                .build();
        final DetailedUpdate expected = DetailedUpdate.builder()
                .update(latestUpdate)
                .packageInfo(packageInfo).build();

        doNothing().when(queuingServiceClient).postDeviceInfo(deviceInfo);
        doReturn(savedDeviceInfo).when(deviceUpdateManager).createDeviceInfo(deviceInfo);
        doReturn(expected).when(deviceUpdateManager).getUpdateForDevice(savedDeviceInfo);

        // When
        final DetailedUpdate opt = deviceUpdateManager.checkForUpdate(deviceInfo);

        // Then
        verify(deviceUpdateManager).checkForUpdate(deviceInfo);
        verify(queuingServiceClient).postDeviceInfo(deviceInfo);
        verify(deviceUpdateManager).createDeviceInfo(deviceInfo);
        verify(deviceUpdateManager).getUpdateForDevice(savedDeviceInfo);
        verifyNoMoreInteractions(deviceUpdateManager);
        assertThat(opt).isEqualTo(expected);
    }

    @Test
    public void checkForUpdate_whenNoNewUpdateAvailable_shouldThrowException() throws MalformedURLException {
        // Given
        final DeviceInfo deviceInfo = getDeviceInfo();
        final DeviceInfo savedDeviceInfo = getDeviceInfo();
        doReturn(savedDeviceInfo).when(deviceUpdateManager).createDeviceInfo(deviceInfo);
        doReturn(Optional.empty()).when(updateServiceClient).getLatestPublishedUpdateByUserIdAndSegmentId(savedDeviceInfo.getUserId(), savedDeviceInfo.getSegmentId());
        doNothing().when(queuingServiceClient).postDeviceInfo(deviceInfo);

        // Then / When
        assertThatExceptionOfType(NoUpdateAvailableException.class).isThrownBy(() -> deviceUpdateManager.checkForUpdate(deviceInfo));
        verify(deviceUpdateManager).createDeviceInfo(deviceInfo);
        verify(deviceUpdateManager).getUpdateForDevice(savedDeviceInfo);
        verify(updateServiceClient).getLatestPublishedUpdateByUserIdAndSegmentId(savedDeviceInfo.getUserId(), savedDeviceInfo.getSegmentId());
        verify(queuingServiceClient).postDeviceInfo(deviceInfo);
        verify(deviceUpdateManager).checkForUpdate(deviceInfo);
        verifyNoMoreInteractions(deviceUpdateManager);
    }

    @Test
    public void downloadUpdate_ifCopyFails_shouldLogErrorAndReturnPackageSize() {
        // Given
        final String updateId = UUID.randomUUID().toString();
        final OutputStream outputStream = new ByteArrayOutputStream();
        final PackageInfo packageInfo = getPackageInfo();

        final long copied = 1337L;

        doReturn(packageInfo).when(deviceUpdateManager).getPackageInfoByUpdateIdAndUserId(updateId, packageInfo.getUserId());
        doReturn(copied).when(deviceUpdateManager).loadPackageStream(packageInfo, outputStream);

        // When
        long result = deviceUpdateManager.downloadUpdate(packageInfo.getUserId(), updateId, outputStream);

        // Then
        verify(deviceUpdateManager).getPackageInfoByUpdateIdAndUserId(updateId, packageInfo.getUserId());
        verify(deviceUpdateManager).loadPackageStream(packageInfo, outputStream);
        verify(deviceUpdateManager).downloadUpdate(packageInfo.getUserId(), updateId, outputStream);
        verifyNoMoreInteractions(deviceUpdateManager);
        assertThat(result).isNotEqualTo(copied);
    }

    @Test
    public void downloadUpdate_ifCopySucceeds_shouldReturnPackageSize() {
        // Given
        final String updateId = UUID.randomUUID().toString();
        final OutputStream outputStream = new ByteArrayOutputStream();
        final PackageInfo packageInfo = getPackageInfo();

        final long copied = packageInfo.getSize();

        doReturn(packageInfo).when(deviceUpdateManager).getPackageInfoByUpdateIdAndUserId(updateId, packageInfo.getUserId());
        doReturn(copied).when(deviceUpdateManager).loadPackageStream(packageInfo, outputStream);

        // When
        long result = deviceUpdateManager.downloadUpdate(packageInfo.getUserId(), updateId, outputStream);

        // Then
        verify(deviceUpdateManager).getPackageInfoByUpdateIdAndUserId(updateId, packageInfo.getUserId());
        verify(deviceUpdateManager).loadPackageStream(packageInfo, outputStream);
        verify(deviceUpdateManager).downloadUpdate(packageInfo.getUserId(), updateId, outputStream);
        verifyNoMoreInteractions(deviceUpdateManager);
        assertThat(result).isEqualTo(copied);
    }

    @Test
    public void downloadVersion_ifSucceeds_shouldReturnVersion() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String packageRef = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();
        final OutputStream outputStream = new ByteArrayOutputStream();
        long expected = 123L;
        doReturn(expected).when(componentServiceClient).getVersionFile(userId, packageRef, versionId, outputStream);

        // When
        long result = deviceUpdateManager.downloadVersion(userId, packageRef, versionId, outputStream);

        // Then
        verify(deviceUpdateManager).downloadVersion(userId, packageRef, versionId, outputStream);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void getUpdateForDevice_shouldReturnDetailedUpdate_whenANewUpdateIsAvailable() {
        // Given
        final DeviceInfo deviceInfo = getDeviceInfo();
        final Update update = getUpdate();
        final DetailedUpdate expectedUpdateForDevice = DetailedUpdate.builder()
                .update(update)
                .packageInfo(getPackageInfo())
                .build();
        doReturn(Optional.of(update)).when(updateServiceClient).getLatestPublishedUpdateByUserIdAndSegmentId(deviceInfo.getUserId(), deviceInfo.getSegmentId());
        doReturn(expectedUpdateForDevice.getPackageInfo()).when(deviceUpdateManager).getPackageInfoByPackageIdAndUserId(update.getPackageId(), deviceInfo.getUserId());

        // When
        final DetailedUpdate updateForDevice = deviceUpdateManager.getUpdateForDevice(deviceInfo);

        // Then
        verify(deviceUpdateManager).getUpdateForDevice(deviceInfo);
        verify(updateServiceClient).getLatestPublishedUpdateByUserIdAndSegmentId(deviceInfo.getUserId(), deviceInfo.getSegmentId());
        verify(deviceUpdateManager).getPackageInfoByPackageIdAndUserId(update.getPackageId(), deviceInfo.getUserId());
        verifyNoMoreInteractions(deviceUpdateManager);
        assertThat(updateForDevice).isEqualTo(expectedUpdateForDevice);
    }

    @Test
    public void getUpdateForDevice_shouldThrowAnException_whenDeviceIsUpToDate() {
        // Given
        final DeviceInfo deviceInfo = getDeviceInfo();
        final Update update = getUpdate();
        final PackageInfo packageInfo = getPackageInfo().toBuilder()
                .versionId(deviceInfo.getVersionId())
                .build();
        doReturn(Optional.of(update)).when(updateServiceClient).getLatestPublishedUpdateByUserIdAndSegmentId(deviceInfo.getUserId(), deviceInfo.getSegmentId());
        doReturn(packageInfo).when(deviceUpdateManager).getPackageInfoByPackageIdAndUserId(update.getPackageId(), deviceInfo.getUserId());

        // When / Then
        assertThatExceptionOfType(NoUpdateAvailableException.class)
                .isThrownBy(() -> deviceUpdateManager.getUpdateForDevice(deviceInfo));

        verify(deviceUpdateManager).getUpdateForDevice(deviceInfo);
        verify(updateServiceClient).getLatestPublishedUpdateByUserIdAndSegmentId(deviceInfo.getUserId(), deviceInfo.getSegmentId());
        verify(deviceUpdateManager).getPackageInfoByPackageIdAndUserId(update.getPackageId(), deviceInfo.getUserId());
        verifyNoMoreInteractions(deviceUpdateManager);
    }
}
