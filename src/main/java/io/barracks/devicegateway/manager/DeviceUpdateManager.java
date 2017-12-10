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

import io.barracks.commons.exceptions.BarracksServiceClientException;
import io.barracks.devicegateway.client.*;
import io.barracks.devicegateway.exception.DeviceGatewayException;
import io.barracks.devicegateway.exception.InvalidUpdateStatusException;
import io.barracks.devicegateway.exception.NotPackageOwnerException;
import io.barracks.devicegateway.exception.PackageStreamException;
import io.barracks.devicegateway.exception.NoUpdateAvailableException;
import io.barracks.devicegateway.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;

@Service
@Slf4j
public class DeviceUpdateManager {

    private final DeviceServiceClient deviceServiceClient;
    private final UpdateServiceClient updateServiceClient;
    private final PackageServiceClient packageServiceClient;
    private final QueuingServiceClient queuingServiceClient;
    private final ComponentServiceClient componentServiceClient;

    @Autowired
    public DeviceUpdateManager(DeviceServiceClient deviceServiceClient, PackageServiceClient packageServiceClient, QueuingServiceClient queuingServiceClient, UpdateServiceClient updateServiceClient, ComponentServiceClient componentServiceClient) {
        this.deviceServiceClient = deviceServiceClient;
        this.packageServiceClient = packageServiceClient;
        this.queuingServiceClient = queuingServiceClient;
        this.updateServiceClient = updateServiceClient;
        this.componentServiceClient = componentServiceClient;
    }

    public DetailedUpdate checkForUpdate(DeviceInfo deviceInfo) {
        queuingServiceClient.postDeviceInfo(deviceInfo);
        final DeviceInfo savedDeviceInfo = createDeviceInfo(deviceInfo);
        return getUpdateForDevice(savedDeviceInfo);
    }

    DeviceInfo createDeviceInfo(DeviceInfo deviceInfo) {
        try {
            return deviceServiceClient.createDeviceInfo(deviceInfo);
        } catch (BarracksServiceClientException e) {
            throw new DeviceGatewayException("DeviceInfo creation failed.", e);
        }
    }

    DetailedUpdate getUpdateForDevice(DeviceInfo deviceInfo) {
        return updateServiceClient.getLatestPublishedUpdateByUserIdAndSegmentId(deviceInfo.getUserId(), deviceInfo.getSegmentId())
                .map(update -> {
                    final PackageInfo packageInfo = getPackageInfoByPackageIdAndUserId(update.getPackageId(), deviceInfo.getUserId());
                    return DetailedUpdate.builder()
                            .update(update)
                            .packageInfo(packageInfo)
                            .build();
                })
                .filter(detailedUpdate -> !detailedUpdate.getPackageInfo().getVersionId().equals(deviceInfo.getVersionId()))
                .orElseThrow(NoUpdateAvailableException::new);
    }

    public long downloadUpdate(String userId, String updateId, OutputStream outputStream) {
        final PackageInfo packageInfo = getPackageInfoByUpdateIdAndUserId(updateId, userId);
        final long copied = loadPackageStream(packageInfo, outputStream);

        if (copied != packageInfo.getSize()) {
            log.error("Expected '" + packageInfo.getSize() + "' but copied '" + copied + "'");
        }
        return packageInfo.getSize();

    }

    public long downloadVersion(String userId, String packageRef, String versionId, OutputStream outputStream) {
        return componentServiceClient.getVersionFile(userId, packageRef, versionId, outputStream);
    }

    long loadPackageStream(PackageInfo packageInfo, OutputStream outputStream) {
        try {
            return packageServiceClient.loadPackageStream(packageInfo, outputStream);
        } catch (IOException e) {
            throw new PackageStreamException(packageInfo, e);
        }
    }

    PackageInfo getPackageInfoByPackageIdAndUserId(String packageId, String userId) {
        PackageInfo packageInfo = packageServiceClient.getPackageInfo(packageId);
        if (packageInfo.getUserId().equals(userId)) {
            return packageInfo;
        } else {
            throw new NotPackageOwnerException(packageId, userId);
        }
    }

    PackageInfo getPackageInfoByUpdateIdAndUserId(String updateId, String userId) {
        return updateServiceClient.getUpdateByUuidAndUserId(updateId, userId)
                .filter(update -> update.getStatus() == UpdateStatus.PUBLISHED)
                .map(update -> getPackageInfoByPackageIdAndUserId(update.getPackageId(), userId))
                .orElseThrow(InvalidUpdateStatusException::new);
    }

}

