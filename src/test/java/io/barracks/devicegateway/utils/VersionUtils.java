
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

package io.barracks.devicegateway.utils;

import io.barracks.devicegateway.model.Version;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionUtils {

    public static Version getVersion() {
        final Version version = Version.builder()
                .reference(UUID.randomUUID().toString())
                .filename(UUID.randomUUID().toString())
                .version(UUID.randomUUID().toString())
                .size(198465432358486L)
                .md5(UUID.randomUUID().toString())
                .url(UUID.randomUUID().toString())
                .addMetadata(UUID.randomUUID().toString(), UUID.randomUUID().toString())
                .build();
        assertThat(version).hasNoNullFieldsOrProperties();
        return version;
    }
}
