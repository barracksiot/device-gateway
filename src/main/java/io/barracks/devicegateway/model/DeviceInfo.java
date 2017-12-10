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

package io.barracks.devicegateway.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DeviceInfo {

    @NotBlank
    @Pattern(regexp = "^[\\p{Print}]*$", message = "must be composed of printable characters only")
    private final String unitId;

    private final String userId;

    private final String segmentId;

    @NotBlank
    private final String versionId;

    private final Date receptionDate;

    private final String deviceIP;

    private final String userAgent;

    @NotNull
    @Singular
    private final Map<String, Object> additionalProperties;

    @JsonCreator
    public static DeviceInfo fromJson(
            @JsonProperty("unitId") String unitId,
            @JsonProperty("receptionDate") Date receptionDate,
            @JsonProperty("additionalProperties") Map<String, Object> additionalProperties) {
        return DeviceInfo.builder()
                // TODO check if we are to receive null values here
                .unitId(unitId != null ? unitId.trim() : null)
                .receptionDate(receptionDate != null ? new Date(receptionDate.getTime()) : null)
                .additionalProperties(additionalProperties == null ? Collections.emptyMap() : new LinkedHashMap<>(additionalProperties))
                .build();
    }

    public Date getReceptionDate() {
        if (this.receptionDate != null) {
            return new Date(receptionDate.getTime());
        }
        return null;
    }

    public Map<String, Object> getAdditionalProperties() {
        return new LinkedHashMap<>(additionalProperties);
    }

}
