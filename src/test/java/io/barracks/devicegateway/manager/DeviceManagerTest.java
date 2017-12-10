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

import io.barracks.devicegateway.client.ComponentServiceClient;
import io.barracks.devicegateway.client.DeploymentServiceClient;
import io.barracks.devicegateway.client.QueuingServiceClientV2;
import io.barracks.devicegateway.model.*;
import io.barracks.devicegateway.model.Package;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.annotation.AsyncResult;

import java.util.UUID;

import static io.barracks.devicegateway.utils.DeviceRequestUtils.getDeviceRequest;
import static io.barracks.devicegateway.utils.PackageUtils.getPackage;
import static io.barracks.devicegateway.utils.ResolvedPackagesUtils.getResolvedPackages;
import static io.barracks.devicegateway.utils.ResolvedVersionsUtils.getResolvedVersions;
import static io.barracks.devicegateway.utils.VersionUtils.getVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceManagerTest {

    @Mock
    private DeploymentServiceClient deploymentServiceClient;

    @Mock
    private ComponentServiceClient componentServiceClient;

    @Mock
    private QueuingServiceClientV2 queuingServiceClientV2;

    @InjectMocks
    @Spy
    private DeviceManager manager;

    @Test
    public void getVersion_shouldCallClient_andReturnResult() {
        // Given
        final String userId = UUID.randomUUID().toString();
        final String reference = UUID.randomUUID().toString();
        final String version = UUID.randomUUID().toString();
        final Version expected = getVersion();
        doReturn(expected).when(componentServiceClient).getVersion(userId, reference, version);

        // When
        final Version result = manager.getVersion(userId, reference, version);

        // Then
        verify(componentServiceClient).getVersion(userId, reference, version);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void resolveVersions_shouldReturnResolvedVersionsAndPostDeviceEvent() {
        // Given
        final DeviceRequest deviceRequest = getDeviceRequest();
        final ResolvedVersions resolvedVersions = getResolvedVersions();
        final ResolvedPackages resolvedPackages = getResolvedPackages();
        final DeviceEvent deviceEvent = DeviceEvent.builder()
                .request(deviceRequest)
                .response(resolvedVersions)
                .build();
        doNothing().when(queuingServiceClientV2).postDeviceRequest(deviceEvent);
        doReturn(resolvedPackages).when(deploymentServiceClient).resolvePackages(deviceRequest);
        doReturn(resolvedVersions).when(manager).buildResolvedVersions(deviceRequest, resolvedPackages);

        // When
        final ResolvedVersions result = manager.resolveVersions(deviceRequest);

        // Then
        verify(queuingServiceClientV2).postDeviceRequest(deviceEvent);
        verify(manager).resolveVersions(deviceRequest);
        verify(deploymentServiceClient).resolvePackages(deviceRequest);
        verify(manager).buildResolvedVersions(deviceRequest, resolvedPackages);
        verifyNoMoreInteractions(manager);
        assertThat(result).isEqualTo(resolvedVersions);
    }

    @Test
    public void buildResolvedVersions_whenDeviceHasVersion_andVersionIsAbsent_shouldReturnUnavailable() {
        // Given
        final Package aPackage = getPackage();
        final Version expected = Version.fromPackage(aPackage);
        final DeviceRequest request = getDeviceRequest().toBuilder()
                .clearPackages()
                .addPackage(aPackage)
                .build();
        final ResolvedPackages resolvedPackages = ResolvedPackages.builder()
                .addAbsent(aPackage)
                .build();

        // When
        final ResolvedVersions response = manager.buildResolvedVersions(request, resolvedPackages);

        // Then
        assertThat(response.getAvailable()).isEmpty();
        assertThat(response.getUnavailable()).containsExactly(expected);
        assertThat(response.getChanged()).isEmpty();
        assertThat(response.getUnchanged()).isEmpty();
    }

    @Test
    public void buildResolvedVersions_whenDeviceHasNotVersion_andVersionIsPresentWithId_shouldReturnAvailable() {
        // Given
        final Package aPackage = getPackage();
        final Version expected = Version.fromPackage(aPackage);
        final DeviceRequest request = getDeviceRequest().toBuilder()
                .clearPackages()
                .build();
        final ResolvedPackages resolvedPackages = ResolvedPackages.builder()
                .addPresent(aPackage)
                .build();
        doReturn(expected).when(manager).getVersion(request.getUserId(), expected.getReference(), expected.getVersion());

        // When
        final ResolvedVersions response = manager.buildResolvedVersions(request, resolvedPackages);

        // Then
        verify(manager).getVersion(request.getUserId(), expected.getReference(), expected.getVersion());
        assertThat(response.getAvailable()).containsExactly(expected);
        assertThat(response.getUnavailable()).isEmpty();
        assertThat(response.getChanged()).isEmpty();
        assertThat(response.getUnchanged()).isEmpty();
    }

    @Test
    public void buildResolvedVersions_whenDeviceHasVersion_andVersionIsPresentWithoutId_shouldReturnUnchanged() {
        // Given
        final Package aPackage = getPackage();
        final Package withoutId = aPackage.toBuilder().version(null).build();
        final Version expected = Version.fromPackage(withoutId);
        final DeviceRequest request = getDeviceRequest().toBuilder()
                .clearPackages()
                .addPackage(aPackage)
                .build();
        final ResolvedPackages resolvedPackages = ResolvedPackages.builder()
                .addPresent(withoutId)
                .build();

        // When
        final ResolvedVersions response = manager.buildResolvedVersions(request, resolvedPackages);

        // Then
        assertThat(response.getAvailable()).isEmpty();
        assertThat(response.getUnavailable()).isEmpty();
        assertThat(response.getChanged()).isEmpty();
        assertThat(response.getUnchanged()).containsExactly(expected);
    }

    @Test
    public void buildResolvedVersions_whenDeviceHasVersion_andVersionIsPresentWithSameId_shouldReturnUnchanged() {
        // Given
        final Package aPackage = getPackage();
        final Version expected = Version.fromPackage(aPackage);
        final DeviceRequest request = getDeviceRequest().toBuilder()
                .clearPackages()
                .addPackage(aPackage)
                .build();
        final ResolvedPackages resolvedPackages = ResolvedPackages.builder()
                .addPresent(aPackage)
                .build();

        // When
        final ResolvedVersions response = manager.buildResolvedVersions(request, resolvedPackages);

        // Then
        assertThat(response.getAvailable()).isEmpty();
        assertThat(response.getUnavailable()).isEmpty();
        assertThat(response.getChanged()).isEmpty();
        assertThat(response.getUnchanged()).containsExactly(expected);
    }

    //
//    @Test
    public void buildResolvedVersions_whenDeviceHasVersion_andVersionIsPresentWithDifferentId_shouldReturnChanged() {
        // Given
        final Package aPackage = getPackage();
        final Version expected = Version.fromPackage(aPackage);
        final DeviceRequest request = getDeviceRequest().toBuilder()
                .clearPackages()
                .addPackage(aPackage.toBuilder().version(UUID.randomUUID().toString()).build())
                .build();
        final ResolvedPackages resolvedPackages = ResolvedPackages.builder()
                .addPresent(aPackage)
                .build();
        doReturn(expected).when(manager).getVersion(request.getUserId(), expected.getReference(), expected.getVersion());

        // When
        final ResolvedVersions response = manager.buildResolvedVersions(request, resolvedPackages);

        // Then
        verify(manager).getVersion(request.getUserId(), expected.getReference(), expected.getVersion());
        assertThat(response.getAvailable()).isEmpty();
        assertThat(response.getUnavailable()).isEmpty();
        assertThat(response.getChanged()).containsExactly(expected);
        assertThat(response.getUnchanged()).isEmpty();
    }

    @Test
    public void buildResolvedVersions_whenDeviceHasVersion_andPackageDoesNotExist_shouldIgnorePackageFromResponse() {
        // Given
        final Package aPackage = getPackage();
        final DeviceRequest request = getDeviceRequest().toBuilder()
                .clearPackages()
                .addPackage(aPackage)
                .build();
        final ResolvedPackages resolvedPackages = ResolvedPackages.builder().build();

        // When
        final ResolvedVersions response = manager.buildResolvedVersions(request, resolvedPackages);

        // Then
        assertThat(response.getAvailable()).isEmpty();
        assertThat(response.getUnavailable()).isEmpty();
        assertThat(response.getChanged()).isEmpty();
        assertThat(response.getUnchanged()).isEmpty();
    }

    @Test
    public void buildResolvedVersions_whenDeviceHasNoVersion_andNoPackageIsPresent_shouldIgnorePackageFromResponse() {
        // Given
        final DeviceRequest request = getDeviceRequest().toBuilder()
                .clearPackages()
                .build();
        final ResolvedPackages resolvedPackages = ResolvedPackages.builder().build();

        // When
        final ResolvedVersions response = manager.buildResolvedVersions(request, resolvedPackages);

        // Then
        assertThat(response.getAvailable()).isEmpty();
        assertThat(response.getUnavailable()).isEmpty();
        assertThat(response.getChanged()).isEmpty();
        assertThat(response.getUnchanged()).isEmpty();
    }

    @Test
    public void buildResolvedVersions_whenDeviceHasNoVersion_andVersionIsPresentWithoutId_shouldIgnorePackageFromResponse() {
        // Given
        final DeviceRequest request = getDeviceRequest().toBuilder()
                .clearPackages()
                .build();
        final ResolvedPackages resolvedPackages = ResolvedPackages.builder()
                .addPresent(getPackage().toBuilder().version(null).build())
                .build();

        // When
        final ResolvedVersions response = manager.buildResolvedVersions(request, resolvedPackages);

        // Then
        assertThat(response.getAvailable()).isEmpty();
        assertThat(response.getUnavailable()).isEmpty();
        assertThat(response.getChanged()).isEmpty();
        assertThat(response.getUnchanged()).isEmpty();
    }

    @Test
    public void buildResolvedVersions_whenDeviceHasNoVersion_andVersionIsAbsent_shouldIgnorePackageFromResponse() {
        // Given
        final DeviceRequest request = getDeviceRequest().toBuilder()
                .clearPackages()
                .build();
        final ResolvedPackages resolvedPackages = ResolvedPackages.builder()
                .addAbsent(getPackage().toBuilder().version(null).build())
                .build();

        // When
        final ResolvedVersions response = manager.buildResolvedVersions(request, resolvedPackages);

        // Then
        assertThat(response.getAvailable()).isEmpty();
        assertThat(response.getUnavailable()).isEmpty();
        assertThat(response.getChanged()).isEmpty();
        assertThat(response.getUnchanged()).isEmpty();
    }
}