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

package io.barracks.devicegateway.model.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.barracks.devicegateway.exception.UnknownUpdateStatusException;
import io.barracks.devicegateway.model.UpdateStatus;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class UpdateStatusDeserializerTest {

    private DeserializationContext ctx;
    private ObjectMapper mapper;
    private UpdateStatusDeserializer deserializer;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        ctx = mapper.getDeserializationContext();
        deserializer = new UpdateStatusDeserializer();
    }

    @Test
    public void testDeserializeArchivedStatus() throws IOException {
        final String serializedStatus = "\"archived\"";
        final JsonParser parser = mapper.getFactory().createParser(serializedStatus);

        UpdateStatus status = deserializer.deserialize(parser, ctx);

        assertNotNull(status);
        assertEquals(UpdateStatus.ARCHIVED, status);
    }

    @Test
    public void testDeserializeDraftStatus() throws IOException {
        final String serializedStatus = "\"draft\"";
        final JsonParser parser = mapper.getFactory().createParser(serializedStatus);

        UpdateStatus status = deserializer.deserialize(parser, ctx);

        assertNotNull(status);
        assertEquals(UpdateStatus.DRAFT, status);
    }

    @Test
    public void testDeserializePublishedStatus() throws IOException {
        final String serializedStatus = "\"published\"";
        final JsonParser parser = mapper.getFactory().createParser(serializedStatus);

        UpdateStatus status = deserializer.deserialize(parser, ctx);

        assertNotNull(status);
        assertEquals(UpdateStatus.PUBLISHED, status);
    }

    @Test (expected = UnknownUpdateStatusException.class)
    public void testDeserializeInvalidStatus() throws IOException {
        final String serializedStatus = "\"youpla\"";
        final JsonParser parser = mapper.getFactory().createParser(serializedStatus);

        deserializer.deserialize(parser, ctx);
    }

}
