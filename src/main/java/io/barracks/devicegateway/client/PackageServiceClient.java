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

import io.barracks.commons.util.Endpoint;
import io.barracks.devicegateway.client.exception.PackageServiceClientException;
import io.barracks.devicegateway.model.PackageInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class PackageServiceClient {
    private static final Endpoint DOWNLOAD_ENDPOINT = Endpoint.from(HttpMethod.GET, "/packages/{packageId}/file");
    private static final Endpoint GET_PACKAGE_INFO_ENDPOINT = Endpoint.from(HttpMethod.GET, "/packages/{packageId}");

    private RestTemplate restTemplate;

    private String packageServiceBaseUrl;

    public PackageServiceClient(
            @Value("${io.barracks.packageservice.base_url}") String packageServiceBaseUrl,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.packageServiceBaseUrl = packageServiceBaseUrl;
        this.restTemplate = restTemplateBuilder.build();
    }

    public long loadPackageStream(PackageInfo packageInfo, OutputStream outputStream) throws IOException {
        try {
            return restTemplate.execute(
                    DOWNLOAD_ENDPOINT.withBase(packageServiceBaseUrl).getURI(packageInfo.getId()),
                    DOWNLOAD_ENDPOINT.getMethod(),
                    request -> request.getHeaders().set(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE),
                    new LargeFileForwardingExtractor(restTemplate.getMessageConverters(), outputStream)
            );
        } catch (HttpStatusCodeException e) {
            throw new PackageServiceClientException(e);
        }
    }

    public PackageInfo getPackageInfo(String id) {
        try {
            return restTemplate.exchange(
                    GET_PACKAGE_INFO_ENDPOINT.withBase(packageServiceBaseUrl).getRequestEntity(id),
                    PackageInfo.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new PackageServiceClientException(e);
        }
    }
}
