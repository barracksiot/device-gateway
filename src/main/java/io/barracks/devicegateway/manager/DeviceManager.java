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
import io.barracks.devicegateway.model.DeviceEvent;
import io.barracks.devicegateway.model.*;
import io.barracks.devicegateway.model.Package;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeviceManager {

    private final DeploymentServiceClient deploymentServiceClient;
    private final ComponentServiceClient componentServiceClient;
    private final QueuingServiceClientV2 queuingServiceClientV2;

    public DeviceManager(
            DeploymentServiceClient deploymentServiceClient,
            ComponentServiceClient componentServiceClient,
            QueuingServiceClientV2 queuingServiceClientV2
    ) {
        this.deploymentServiceClient = deploymentServiceClient;
        this.componentServiceClient = componentServiceClient;
        this.queuingServiceClientV2 = queuingServiceClientV2;
    }

    public ResolvedVersions resolveVersions(DeviceRequest request) {
        final ResolvedPackages resolvedPackages = deploymentServiceClient.resolvePackages(request);
        final ResolvedVersions resolvedVersions = buildResolvedVersions(request, resolvedPackages);
        final DeviceEvent deviceEvent = DeviceEvent.builder().request(request).response(resolvedVersions).build();
        queuingServiceClientV2.postDeviceRequest(deviceEvent);
        return resolvedVersions;
    }

    ResolvedVersions buildResolvedVersions(DeviceRequest request, ResolvedPackages resolvedPackages) {
        final Map<String, Package> requestPackages = request.getPackages().stream()
                .collect(
                        Collectors.toMap(
                                Package::getReference,
                                pkg -> pkg
                        )
                );
        return ResolvedVersions.builder()
                .available(getAvailableVersions(request.getUserId(), requestPackages, resolvedPackages))
                .unavailable(getUnavailableVersions(requestPackages, resolvedPackages))
                .changed(getChangedVersions(request.getUserId(), requestPackages, resolvedPackages))
                .unchanged(getUnchangedVersions(requestPackages, resolvedPackages))
                .build();
    }

    private List<Version> getAvailableVersions(String userId, Map<String, Package> requestPackages, ResolvedPackages resolvedPackages) {
        return resolvedPackages.getPresent()
                .stream()
                .filter(pkg -> !Optional.ofNullable(requestPackages.get(pkg.getReference())).isPresent())
                .filter(pkg -> pkg.getVersion().isPresent())
                .map(pkg -> getVersion(userId, pkg.getReference(), pkg.getVersion().get()))
                .collect(Collectors.toList());
    }

    private List<Version> getUnavailableVersions(Map<String, Package> requestPackages, ResolvedPackages resolvedPackages) {
        return resolvedPackages.getAbsent()
                .stream()
                .filter(pkg -> Optional.ofNullable(requestPackages.get(pkg.getReference())).isPresent())
                .map(Version::fromPackage)
                .collect(Collectors.toList());
    }

    private List<Version> getChangedVersions(String userId, Map<String, Package> requestPackages, ResolvedPackages resolvedPackages) {
        return resolvedPackages.getPresent()
                .stream()
                .filter(pkg -> Optional.ofNullable(requestPackages.get(pkg.getReference()))
                        .filter(devicePkg -> pkg.getVersion().isPresent() && !devicePkg.getVersion().equals(pkg.getVersion()))
                        .isPresent()
                )
                .map(pkg -> getVersion(userId, pkg.getReference(), pkg.getVersion().get()))
                .collect(Collectors.toList());

    }

    private List<Version> getUnchangedVersions(Map<String, Package> requestPackages, ResolvedPackages resolvedPackages) {
        return resolvedPackages.getPresent()
                .stream()
                .filter(pkg -> Optional.ofNullable(requestPackages.get(pkg.getReference()))
                        .filter(devicePackage -> !pkg.getVersion().isPresent() || pkg.getVersion().equals(devicePackage.getVersion()))
                        .isPresent()
                )
                .map(Version::fromPackage)
                .collect(Collectors.toList());
    }

    Version getVersion(String userId, String reference, String version) {
        return componentServiceClient.getVersion(userId, reference, version);
    }
}
