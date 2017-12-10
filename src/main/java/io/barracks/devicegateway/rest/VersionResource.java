
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

import com.google.common.base.Strings;
import io.barracks.commons.util.Endpoint;
import io.barracks.devicegateway.exception.DeviceGatewayException;
import io.barracks.devicegateway.manager.DeviceManager;
import io.barracks.devicegateway.manager.DeviceUpdateManager;
import io.barracks.devicegateway.model.DeviceRequest;
import io.barracks.devicegateway.model.ResolvedVersions;
import io.barracks.devicegateway.model.User;
import io.barracks.devicegateway.model.Version;
import io.barracks.devicegateway.security.UserAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class VersionResource {

    private static final String DOWNLOAD_URI_MAPPING = "/packages/{packageRef}/versions/{versionId}/file";
    static final Endpoint DOWNLOAD_ENDPOINT = Endpoint.from(HttpMethod.GET, DOWNLOAD_URI_MAPPING);

    private final DeviceUpdateManager deviceUpdateManager;
    private final DeviceManager deviceManager;
    private final CounterService counter;

    @Autowired
    public VersionResource(DeviceUpdateManager deviceUpdateManager, DeviceManager deviceManager, CounterService counter) {
        this.deviceUpdateManager = deviceUpdateManager;
        this.deviceManager = deviceManager;
        this.counter = counter;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/resolve")
    public ResolvedVersions resolveVersions(HttpServletRequest request, @RequestBody @Valid DeviceRequest deviceRequest, Principal principal) {
        final User user = ((UserAuthentication) principal).getDetails();
        incrementPingMetric(user);
        final String baseUrl = getBaseUrl(request);
        final DeviceRequest authenticatedRequest = deviceRequest.toBuilder()
                .userId(principal.getName())
                .userAgent(getUserAgent(request))
                .ipAddress(getIpAddress(request))
                .build();
        final ResolvedVersions resolvedVersions = deviceManager.resolveVersions(authenticatedRequest);
        return ResolvedVersions.builder()
                .unavailable(resolvedVersions.getUnavailable())
                .unchanged(resolvedVersions.getUnchanged())
                .available(
                        resolvedVersions.getAvailable().stream()
                                .map(version -> addVersionUrl(baseUrl, version))
                                .collect(Collectors.toList())
                )
                .changed(
                        resolvedVersions.getChanged().stream()
                                .map(version -> addVersionUrl(baseUrl, version))
                                .collect(Collectors.toList())
                )
                .build();

    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("user-agent");
    }

    private String getIpAddress(HttpServletRequest request) {
        final String ipData = request.getHeader("X-Forwarded-For");
        if(!Strings.isNullOrEmpty(ipData)) {
            final String ips[] = ipData.split(",");
            return ips[0];
        } else {
            return request.getRemoteAddr();
        }
    }

    private Version addVersionUrl(String baseUrl, Version version) {
        return version.toBuilder()
                .url(DOWNLOAD_ENDPOINT.withBase(baseUrl).getURI(version.getReference(), version.getVersion()).toString())
                .build();
    }

    String getBaseUrl(HttpServletRequest request) {
        try {
            final String protocol = Optional.ofNullable(request.getHeader("X-Forwarded-Proto")).orElse(request.isSecure() ? "https" : "http");
            final String hostHeader = Optional.ofNullable(request.getHeader("Host")).orElse("");
            final String[] splitHostHeader = hostHeader.split(":");
            final String host = splitHostHeader[0];
            final Integer port = splitHostHeader.length > 1 ? Integer.valueOf(splitHostHeader[1]) : null;
            final String pathPrefix = Optional.ofNullable(request.getHeader("X-Forwarded-Prefix")).orElse("");
            if (port != null) {
                return new URL(protocol, host, port, pathPrefix).toExternalForm();
            }
            return new URL(protocol, host, pathPrefix).toExternalForm();
        } catch (MalformedURLException e) {
            throw new DeviceGatewayException("Failed to create download URL for " + request, e);
        }
    }

    @RequestMapping(value = DOWNLOAD_URI_MAPPING, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void downloadVersion(@PathVariable("packageRef") String packageRef, @PathVariable("versionId") String versionId, Principal principal, HttpServletResponse response) {
        try {
            deviceUpdateManager.downloadVersion(principal.getName(), packageRef, versionId, response.getOutputStream());
            response.setStatus(HttpServletResponse.SC_OK);
            response.flushBuffer();
        } catch (IOException e) {
            throw new DeviceGatewayException("Could not get output stream from " + response, e);
        }
    }

    private void incrementPingMetric(User user) {
        counter.increment("ping.v2." + user.getEmail().replace("@", "_at_").replace(".", "_"));
    }

}
