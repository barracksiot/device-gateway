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
import com.google.common.collect.ImmutableMap;
import io.barracks.commons.util.Endpoint;
import io.barracks.devicegateway.rest.BarracksResourceTest;
import io.barracks.devicegateway.rest.UpdateResource;
import io.barracks.devicegateway.rest.entity.DevicePackageInfo;
import io.barracks.devicegateway.rest.entity.DeviceRequestEntity;
import io.barracks.devicegateway.rest.entity.DeviceUpdate;
import io.barracks.devicegateway.utils.RandomPrincipal;
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

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@BarracksResourceTest(controllers = UpdateResource.class, outputDir = "build/generated-snippets/update")
public class UpdateResourceConfigurationTest {

    private static final Endpoint CHECK_UPDATE_ENDPOINT = Endpoint.from(HttpMethod.POST, "/update/check");
    private static final Endpoint DOWNLOAD_UPDATE_ENDPOINT = Endpoint.from(HttpMethod.GET, "/update/download/{uuid}");
    private static final String baseUrl = "https://not.barracks.io";

    @Value("classpath:io/barracks/devicegateway/rest/configuration/device-request.json")
    private Resource request;

    @MockBean
    private UpdateResource updateResource;

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
    public void documentCheckUpdate() throws Exception {
        //  Given
        final Endpoint endpoint = CHECK_UPDATE_ENDPOINT;
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        final DeviceUpdate update = DeviceUpdate.builder()
                .versionId("0.0.2")
                .packageInfo(DevicePackageInfo.builder()
                        .url("https://not.barracks.io/api/device/download/some/update")
                        .md5("deadbeefbadc0ffee")
                        .size(42L)
                        .build())
                .customUpdateData(ImmutableMap.of("critical", true))
                .build();
        final DeviceRequestEntity deviceRequestEntity = objectMapper.readValue(request.getInputStream(), DeviceRequestEntity.class);

        doReturn(update).when(updateResource).checkForUpdate(isA(HttpServletRequest.class), eq(deviceRequestEntity), eq(principal));

        // When
        final ResultActions result = mvc.perform(
                RestDocumentationRequestBuilders.request(
                        endpoint.getMethod(), endpoint.getPath())
                        .principal(principal)
                        .header("X-Forwarded-For", "1924.168.45.4, 1924.168.4.32, 1924.168.27.84")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .content(FileCopyUtils.copyToByteArray(request.getInputStream()))
        );

        // Then
        verify(updateResource).checkForUpdate(isA(HttpServletRequest.class), eq(deviceRequestEntity), eq(principal));
        result.andExpect(status().isOk())
                .andDo(document(
                        "check",
                        requestFields(
                                fieldWithPath("unitId").description("The device's unique identifier"),
                                fieldWithPath("versionId").description("The device's firmware version"),
                                fieldWithPath("customClientData").description("The client's custom data")
                        ),
                        responseFields(
                                fieldWithPath("versionId").description("The version associated to the update"),
                                fieldWithPath("packageInfo").description("Information about the package to download"),
                                fieldWithPath("packageInfo.url").description("URL for downloading the file"),
                                fieldWithPath("packageInfo.md5").description("MD5 hash of the file"),
                                fieldWithPath("packageInfo.size").description("Size (in bytes) of the file"),
                                fieldWithPath("customUpdateData").description("Custom data associated to the update")
                        )
                ));
    }

    @Test
    public void checkForUpdate_whenRequestMethodIsGet_shouldReturn405Code() throws Exception {
        mvc.perform(
                MockMvcRequestBuilders.get("/update/check")
        ).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void checkForUpdate_whenUnitIdIsMissing_shouldReturn400Code() throws Exception {
        // Given
        final DeviceRequestEntity deviceRequestEntity = DeviceRequestEntity.builder().versionId("VersionId").build();
        final String requestContent = objectMapper.writeValueAsString(deviceRequestEntity);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/update/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent)
        );

        // Then
        result.andExpect(status().is4xxClientError());
    }

    @Test
    public void checkForUpdate_whenVersionIdIsMissing_shouldReturn400Code() throws Exception {
        // Given
        final DeviceRequestEntity deviceRequestEntity = DeviceRequestEntity.builder().unitId("UnitId").build();
        final String requestContent = objectMapper.writeValueAsString(deviceRequestEntity);

        // When
        final ResultActions result = mvc.perform(
                MockMvcRequestBuilders.post("/update/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestContent)
        );

        // Then
        result.andExpect(status().is4xxClientError());
    }
}
