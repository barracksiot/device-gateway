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

import io.barracks.devicegateway.model.ResolvedVersions;

import static io.barracks.devicegateway.utils.VersionUtils.getVersion;
import static org.assertj.core.api.Assertions.assertThat;

public class ResolvedVersionsUtils {
    public static ResolvedVersions getResolvedVersions() {
        final ResolvedVersions response = ResolvedVersions.builder()
                .addAvailable(getVersion())
                .addAvailable(getVersion())
                .addAvailable(getVersion())
                .addUnavailable(getVersion())
                .addUnavailable(getVersion())
                .addChanged(getVersion())
                .addChanged(getVersion())
                .addUnchanged(getVersion())
                .addUnchanged(getVersion())
                .build();
        assertThat(response).hasNoNullFieldsOrProperties();
        return response;
    }
}
