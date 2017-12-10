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

package io.barracks.devicegateway.rest.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceRequestEntity {

    @NotBlank
    private final String unitId;
    @NotBlank
    private final String versionId;
    @Singular("clientData")
    private final Map<String, Object> customClientData;

    private final String deviceIP;

    @JsonCreator
    public static DeviceRequestEntity fromJson(
            @JsonProperty("unitId") String unitId,
            @JsonProperty("versionId") String versionId,
            @JsonProperty("customClientData") Map<String, Object> customClientData) {
        return DeviceRequestEntity.builder()
                .unitId(unitId)
                .versionId(versionId)
                .customClientData(customClientData == null ? Collections.emptyMap() : new LinkedHashMap<>(customClientData))
                .build();
    }

    public Map<String, Object> getCustomClientData() {
        return new LinkedHashMap<>(customClientData);
    }
}
