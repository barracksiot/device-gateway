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

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import io.barracks.devicegateway.exception.DeviceGatewayException;
import io.barracks.devicegateway.manager.DeviceUpdateManager;
import io.barracks.devicegateway.model.DetailedUpdate;
import io.barracks.devicegateway.model.DeviceInfo;
import io.barracks.devicegateway.model.Update;
import io.barracks.devicegateway.model.User;
import io.barracks.devicegateway.rest.entity.DevicePackageInfo;
import io.barracks.devicegateway.rest.entity.DeviceRequestEntity;
import io.barracks.devicegateway.rest.entity.DeviceUpdate;
import io.barracks.devicegateway.security.UserAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/update")
public class UpdateResource {

    private final DeviceUpdateManager deviceUpdateManager;

    private final CounterService counter;

    @Autowired
    public UpdateResource(DeviceUpdateManager deviceUpdateManager, CounterService counter) {
        this.deviceUpdateManager = deviceUpdateManager;
        this.counter = counter;
    }

    @ResponseBody
    @RequestMapping(value = "/check", method = RequestMethod.POST)
    public DeviceUpdate checkForUpdate(HttpServletRequest request, @Valid @RequestBody DeviceRequestEntity deviceRequest, Principal principal) {
        final String deviceIP = getDeviceIp(request);
        final String userAgent = getUserAgent(request);
        final User user = ((UserAuthentication) principal).getDetails();
        incrementPingMetric(user);
        final DeviceInfo deviceInfo = DeviceInfo.builder()
                .userId(user.getId())
                .unitId(deviceRequest.getUnitId())
                .versionId(deviceRequest.getVersionId())
                .additionalProperties(deviceRequest.getCustomClientData())
                .deviceIP(deviceIP)
                .userAgent(userAgent)
                .build();
        final DetailedUpdate update = deviceUpdateManager.checkForUpdate(deviceInfo);
        final String url = buildDownloadUrl(request, update.getUpdate()).toString();
        return convertToDeviceUpdate(update, url);
    }

    private DeviceUpdate convertToDeviceUpdate(DetailedUpdate update, String url) {
        return DeviceUpdate.builder()
                .versionId(update.getPackageInfo().getVersionId())
                .packageInfo(DevicePackageInfo.builder()
                        .url(url)
                        .md5(update.getPackageInfo().getMd5())
                        .size(update.getPackageInfo().getSize())
                        .build()
                )
                .customUpdateData(update.getUpdate().getAdditionalProperties())
                .build();
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("user-agent");
    }

    private String getDeviceIp(HttpServletRequest request) {
        final String ipData = request.getHeader("X-Forwarded-For");
        if(!Strings.isNullOrEmpty(ipData)) {
            final String ips[] = ipData.split(",");
            return ips[0];
        } else {
            return request.getRemoteAddr();
        }
    }

    private void incrementPingMetric(User user) {
        counter.increment("ping.v1." + user.getEmail().replace("@", "_at_").replace(".", "_"));
    }

    @RequestMapping(value = "/download/{uuid}", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadUpdate(@PathVariable("uuid") String updateId, Principal principal, HttpServletResponse response) {
        try {
            long length = deviceUpdateManager.downloadUpdate(principal.getName(), updateId, response.getOutputStream());
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength((int) length);
            response.flushBuffer();
        } catch (IOException e) {
            throw new DeviceGatewayException("Could not get output stream from " + response, e);
        }
    }

    URL buildDownloadUrl(HttpServletRequest request, Update update) {
        try {
            final String forwardedProtoHeader = request.getHeader("X-Forwarded-Proto");
            final String forwardedPrefixHeader = request.getHeader("X-Forwarded-Prefix");
            final String hostHeader = MoreObjects.firstNonNull(request.getHeader("Host"), "");
            final String[] splitHostHeader = hostHeader.split(":");
            final String host = splitHostHeader[0];
            final Integer port = splitHostHeader.length > 1 ? Integer.valueOf(splitHostHeader[1]) : null;
            final String protocol = request.isSecure() ? "https" : "http";
            final String updateFilePath = "/update/download/" + update.getUuid();
            if (port != null) {
                return new URL(MoreObjects.firstNonNull(forwardedProtoHeader, protocol), host, port, MoreObjects.firstNonNull(forwardedPrefixHeader, "") + updateFilePath);
            }
            return new URL(MoreObjects.firstNonNull(forwardedProtoHeader, protocol), host, MoreObjects.firstNonNull(forwardedPrefixHeader, "") + updateFilePath);
        } catch (MalformedURLException e) {
            throw new DeviceGatewayException("Failed to create download URL for " + request, e);
        }
    }
}
