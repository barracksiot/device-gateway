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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.barracks.devicegateway.exception.UnknownUpdateStatusException;
import io.barracks.devicegateway.model.utils.UpdateStatusDeserializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@JsonDeserialize(using = UpdateStatusDeserializer.class)
public enum UpdateStatus {
    DRAFT("draft"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    private String name;

    UpdateStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isCompatibleWith(UpdateStatus status) {
        return this.getCompatibleStatus().contains(status);
    }

    /**
     * Return all the status compatible with the given status
     * Ex: if an updateInfo is a DRAFT, it cannot be ARCHIVED, it must be PUBLISHED before
     *
     * @return a list of compatible status for the specified status
     */
    public List<UpdateStatus> getCompatibleStatus() {
        switch (this) {
            case DRAFT:
                return Arrays.asList(UpdateStatus.DRAFT, UpdateStatus.PUBLISHED);

            case PUBLISHED:
                return Arrays.asList(UpdateStatus.ARCHIVED);

            case ARCHIVED:
                return Arrays.asList(UpdateStatus.PUBLISHED);

            default:
                return Collections.EMPTY_LIST;
        }
    }

    public static UpdateStatus fromName(String statusName) {
        UpdateStatus statusFound = null;
        for (UpdateStatus status : UpdateStatus.values()) {
            if (statusName.equals(status.name)) {
                statusFound = status;
                break;
            }
        }

        if (statusFound == null) {
            throw new UnknownUpdateStatusException(statusName);
        }

        return statusFound;
    }
}
