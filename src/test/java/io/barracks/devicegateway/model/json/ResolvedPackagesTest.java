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

package io.barracks.devicegateway.model.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.devicegateway.model.Package;
import io.barracks.devicegateway.model.ResolvedPackages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class ResolvedPackagesTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JacksonTester<ResolvedPackages> json;

    @Value("classpath:io/barracks/devicegateway/resolved-packages.json")
    private Resource resolved;

    @Test
    public void deserializeShouldFillPackagesCorrectly() throws Exception {
        // Given
        final ResolvedPackages expected = ResolvedPackages.builder()
                .addPresent(Package.builder()
                        .reference("abc.def")
                        .version("0.0.1")
                        .build()
                )
                .addPresent(Package.builder()
                        .reference("hij.klm")
                        .version("0.0.2")
                        .build()
                )
                .addAbsent(Package.builder()
                        .reference("nop.qrs")
                        .build()
                )
                .addAbsent(Package.builder()
                        .reference("tuv.wxy")
                        .build()
                )
                .build();

        // When
        final ResolvedPackages result = objectMapper.readValue(resolved.getInputStream(), ResolvedPackages.class);

        // Then
        assertThat(result).isEqualTo(expected);
    }
}
