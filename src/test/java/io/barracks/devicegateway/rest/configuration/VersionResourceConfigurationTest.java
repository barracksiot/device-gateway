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

package io.barracks.devicegateway.rest.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.barracks.commons.util.Endpoint;
import io.barracks.devicegateway.model.DeviceRequest;
import io.barracks.devicegateway.model.ResolvedVersions;
import io.barracks.devicegateway.model.Version;
import io.barracks.devicegateway.rest.BarracksResourceTest;
import io.barracks.devicegateway.rest.VersionResource;
import io.barracks.devicegateway.utils.RandomPrincipal;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.FileCopyUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.UUID;

import static io.barracks.devicegateway.utils.ResolvedVersionsUtils.getResolvedVersions;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = VersionResource.class, outputDir = "build/generated-snippets/version")
public class VersionResourceConfigurationTest {
    private static final Endpoint RESOLVE_VERSIONS_ENDPOINT = Endpoint.from(HttpMethod.POST, "/resolve");

    private static final String DOWNLOAD_URI_MAPPING = "/packages/{packageRef}/versions/{versionId}/file";
    private static final Endpoint DOWNLOAD_ENDPOINT = Endpoint.from(HttpMethod.GET, DOWNLOAD_URI_MAPPING);
    private static final String baseUrl = "https://not.barracks.io";

    @Value("classpath:io/barracks/devicegateway/rest/configuration/version.bin")
    private Resource resource;

    @Value("classpath:io/barracks/devicegateway/device-package-request.json")
    private Resource deviceRequest;

    @Value("classpath:io/barracks/devicegateway/rest/configuration/device-package-request-documentation.json")
    private Resource deviceRequestDoc;

    @MockBean
    private VersionResource versionResource;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;
    private RandomPrincipal principal;

    @Before
    public void setUp() throws Exception {
        this.principal = new RandomPrincipal();
    }

    @Test
    public void documentDownloadVersion() throws Exception {
        final Endpoint endpoint = DOWNLOAD_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        final String packageRef = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();

        doAnswer(invocation -> {
                    IOUtils.copyLarge(resource.getInputStream(), ((HttpServletResponse) invocation.getArguments()[3]).getOutputStream());
                    return null;
                }
        ).when(versionResource).downloadVersion(eq(packageRef), eq(versionId), eq(principal), isA(HttpServletResponse.class));
        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.getPath(), packageRef, versionId)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                        .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                        .principal(principal)
        );

        verify(versionResource).downloadVersion(eq(packageRef), eq(versionId), eq(principal), isA(HttpServletResponse.class));
        result.andExpect(status().isOk())
                .andDo(document(
                        "download",
                        pathParameters(
                                parameterWithName("packageRef").description("The package reference associated to the version"),
                                parameterWithName("versionId").description("The version identifier")
                        )
                ));
    }

    @Test
    public void downloadVersion_WhenRequestMethodIsPost_shouldReturn405Code() throws Exception {
        // Given
        final String packageRef = UUID.randomUUID().toString();
        final String versionId = UUID.randomUUID().toString();

        // Then When
        mvc.perform(
                MockMvcRequestBuilders.post(DOWNLOAD_URI_MAPPING, packageRef, versionId)
        ).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void documentResolveVersions() throws Exception {
        // Given
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final Endpoint endpoint = RESOLVE_VERSIONS_ENDPOINT;
        final DeviceRequest request = objectMapper.readValue(deviceRequestDoc.getInputStream(), DeviceRequest.class);
        final ResolvedVersions response = ResolvedVersions.builder()
                .addAvailable(Version.builder()
                        .reference("io.barracks.newApplication")
                        .version("1.0.0")
                        .filename("application.bin")
                        .md5("deadbeef")
                        .size(42L)
                        .url("https://app.barracks.io/download/io.barracks.newApplication/version/1.0.0")
                        .addMetadata("reason", new HashMap<String, String>() {{
                            put("en", "This is the new application for your device.");
                            put("fr", "Ceci est la nouvelle application pour votre périphérique.");
                        }})
                        .build()
                )
                .addChanged(Version.builder()
                        .reference("io.barracks.firmware")
                        .version("0.0.2")
                        .filename("firmware.bin")
                        .md5("badc0ffee")
                        .size(42L)
                        .url("https://app.barracks.io/download/io.barracks.firmware/version/0.0.2")
                        .addMetadata("critical", true)
                        .build()
                )
                .addUnchanged(Version.builder().reference("io.barracks.bootloader").build())
                .addUnavailable(Version.builder().reference("io.barracks.oldApplication").build())
                .build();
        doReturn(response).when(versionResource).resolveVersions(any(), eq(request), eq(principal));

        // When
        final ResultActions result = mvc.perform(RestDocumentationRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI())
                .principal(principal)
                .content(FileCopyUtils.copyToByteArray(deviceRequestDoc.getInputStream()))
                .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(versionResource).resolveVersions(any(), eq(request), eq(principal));
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)))
                .andDo(
                        document(
                                "resolve",
                                requestFields(
                                        fieldWithPath("unitId").description("The device's unique identifier."),
                                        fieldWithPath("packages[].reference").description("The reference to a software package on the device."),
                                        fieldWithPath("packages[].version").description("The version of the software package running on the device."),
                                        fieldWithPath("customClientData").description("Additional information regarding the device.")
                                ),
                                responseFields(
                                        fieldWithPath("changed").description("The updates available for software already running on the device."),
                                        fieldWithPath("changed[].reference").description("The software reference"),
                                        fieldWithPath("changed[].version").description("The software version."),
                                        fieldWithPath("changed[].filename").description("The name of the file."),
                                        fieldWithPath("changed[].md5").description("The md5 hash of the file."),
                                        fieldWithPath("changed[].size").description("The size of the download."),
                                        fieldWithPath("changed[].url").description("The URL to download the file."),
                                        fieldWithPath("changed[].customUpdateData").description("Custom information regarding the update."),
                                        fieldWithPath("available").description("The newly available software not already running on the device."),
                                        fieldWithPath("unchanged").description("Unchanged software already running on the device."),
                                        fieldWithPath("unavailable").description("Software not available anymore for the device")
                                )
                        )
                );
    }

    @Test
    public void postDeviceRequest_shouldReturnDeviceResponse() throws Exception {
        // Given
        final Endpoint endpoint = RESOLVE_VERSIONS_ENDPOINT;
        final DeviceRequest request = objectMapper.readValue(deviceRequest.getInputStream(), DeviceRequest.class);
        final ResolvedVersions response = getResolvedVersions();
        doReturn(response).when(versionResource).resolveVersions(any(), eq(request), eq(principal));

        // When
        final ResultActions result = mvc.perform(MockMvcRequestBuilders.request(endpoint.getMethod(), endpoint.withBase(baseUrl).getURI())
                .principal(principal)
                .content(FileCopyUtils.copyToByteArray(deviceRequest.getInputStream()))
                .contentType(MediaType.APPLICATION_JSON)
        );

        // Then
        verify(versionResource).resolveVersions(any(), eq(request), eq(principal));
        result.andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));
    }
}
