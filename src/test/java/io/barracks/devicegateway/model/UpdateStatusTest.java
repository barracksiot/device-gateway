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

import io.barracks.devicegateway.exception.UnknownUpdateStatusException;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by gregoire on 2016-06-01.
 */
public class UpdateStatusTest {

    @Test
    public void testGetCompatibleStatusWithArchivedStatus() {
        final List<UpdateStatus> compatibleStatus = UpdateStatus.ARCHIVED.getCompatibleStatus();

        Assert.assertNotNull(compatibleStatus);
        Assert.assertTrue(compatibleStatus.contains(UpdateStatus.PUBLISHED));
        Assert.assertFalse(compatibleStatus.contains(UpdateStatus.ARCHIVED));
        Assert.assertFalse(compatibleStatus.contains(UpdateStatus.DRAFT));
    }

    @Test
    public void testGetCompatibleStatusWithDraftStatus() {
        final List<UpdateStatus> compatibleStatus = UpdateStatus.DRAFT.getCompatibleStatus();

        Assert.assertNotNull(compatibleStatus);
        Assert.assertTrue(compatibleStatus.contains(UpdateStatus.PUBLISHED));
        Assert.assertFalse(compatibleStatus.contains(UpdateStatus.ARCHIVED));
        Assert.assertTrue(compatibleStatus.contains(UpdateStatus.DRAFT));
    }

    @Test
    public void testGetCompatibleStatusWithPublishedStatus() {
        final List<UpdateStatus> compatibleStatus = UpdateStatus.PUBLISHED.getCompatibleStatus();

        Assert.assertNotNull(compatibleStatus);
        Assert.assertTrue(compatibleStatus.contains(UpdateStatus.ARCHIVED));
        Assert.assertFalse(compatibleStatus.contains(UpdateStatus.DRAFT));
        Assert.assertFalse(compatibleStatus.contains(UpdateStatus.PUBLISHED));
    }

    @Test
    public void testArchivedIsCompatibleWithArchived() {
        Assert.assertFalse(UpdateStatus.ARCHIVED.isCompatibleWith(UpdateStatus.ARCHIVED));
    }

    @Test
    public void testArchivedIsCompatibleWithDraft() {
        Assert.assertFalse(UpdateStatus.ARCHIVED.isCompatibleWith(UpdateStatus.DRAFT));
    }

    @Test
    public void testArchivedIsCompatibleWithPublished() {
        Assert.assertTrue(UpdateStatus.ARCHIVED.isCompatibleWith(UpdateStatus.PUBLISHED));
    }

    @Test
    public void testDraftIsCompatibleWithArchived() {
        Assert.assertFalse(UpdateStatus.DRAFT.isCompatibleWith(UpdateStatus.ARCHIVED));
    }

    @Test
    public void testDraftIsCompatibleWithDraft() {
        Assert.assertTrue(UpdateStatus.DRAFT.isCompatibleWith(UpdateStatus.DRAFT));
    }

    @Test
    public void testDraftIsCompatibleWithPublished() {
        Assert.assertTrue(UpdateStatus.DRAFT.isCompatibleWith(UpdateStatus.PUBLISHED));
    }

    @Test
    public void testPublishedIsCompatibleWithArchived() {
        Assert.assertTrue(UpdateStatus.PUBLISHED.isCompatibleWith(UpdateStatus.ARCHIVED));
    }

    @Test
    public void testPublishedIsCompatibleWithDraft() {
        Assert.assertFalse(UpdateStatus.PUBLISHED.isCompatibleWith(UpdateStatus.DRAFT));
    }

    @Test
    public void testPublishedIsCompatibleWithPublished() {
        Assert.assertFalse(UpdateStatus.PUBLISHED.isCompatibleWith(UpdateStatus.PUBLISHED));
    }

    @Test
    public void testGetFromNameArchived() {
        Assert.assertEquals(UpdateStatus.ARCHIVED, UpdateStatus.fromName(UpdateStatus.ARCHIVED.getName()));
    }

    @Test
    public void testGetFromNameDraft() {
        Assert.assertEquals(UpdateStatus.DRAFT, UpdateStatus.fromName(UpdateStatus.DRAFT.getName()));
    }

    @Test
    public void testGetFromNamePublished() {
        Assert.assertEquals(UpdateStatus.PUBLISHED, UpdateStatus.fromName(UpdateStatus.PUBLISHED.getName()));
    }

    @Test(expected = UnknownUpdateStatusException.class)
    public void testGetFromNameWithInvalidStatusName() {
        UpdateStatus.fromName("coucou");
    }

}
