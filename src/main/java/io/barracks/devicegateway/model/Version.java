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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode()
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class Version {
    private final String reference;
    private final String version;
    private final String url;
    private final String md5;
    private final Long size;
    private final String filename;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Singular("addMetadata")
    private final Map<String, ?> customUpdateData;

    @JsonCreator
    public static Version fromJson(
            @JsonProperty("packageRef") String reference,
            @JsonProperty("id") String version,
            @JsonProperty("length") long size,
            @JsonProperty("metadata") Map<String, ?> customUpdateData
    ) {
        return builder()
                .reference(reference)
                .version(version)
                .size(size)
                .customUpdateData(Optional.ofNullable(customUpdateData).orElse(Collections.emptyMap()))
                .build();
    }

    public static Version fromPackage(Package aPackage) {
        return builder()
                .reference(aPackage.getReference())
                .version(aPackage.getVersion().orElse(null))
                .build();
    }
}

