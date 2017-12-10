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

package io.barracks.devicegateway.model.validation;

import io.barracks.devicegateway.model.DeviceRequest;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static io.barracks.devicegateway.utils.DeviceRequestUtils.getDeviceRequest;
import static org.assertj.core.api.Assertions.assertThat;

public class DeviceRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void utils_shouldProvideValidObject() {
        // Given
        final DeviceRequest deviceRequest = getDeviceRequest();

        // When
        final Set<ConstraintViolation<DeviceRequest>> violations = validator.validate(deviceRequest);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    public void validation_shouldFail_whenCharactersNotInCorrectRange() {
        // Given
        final DeviceRequest deviceRequest = getDeviceRequest().toBuilder()
                .unitId("A Unit ID \n with spaces and line \t breaks")
                .build();

        // When
        final Set<ConstraintViolation<DeviceRequest>> violations = validator.validate(deviceRequest);

        // Then
        assertThat(violations).hasSize(1);
    }
}
