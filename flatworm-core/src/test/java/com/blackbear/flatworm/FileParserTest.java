/*
 * Flatworm - A Java Flat File Importer/Exporter Copyright (C) 2004 James M. Turner.
 * Extended by James Lawrence 2005
 * Extended by Josh Brackett in 2011 and 2012
 * Extended by Alan Henson in 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.blackbear.flatworm;

import com.blackbear.flatworm.callbacks.ExceptionCallback;
import com.blackbear.flatworm.callbacks.RecordCallback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Class description goes here.
 *
 * @author Alan Henson
 * @since 2016.1.0.0
 */
@RunWith(MockitoJUnitRunner.class)
public class FileParserTest {

    @Test
    public void registerAndUnregisterRecordCallback() {
        RecordCallback callback = mock(RecordCallback.class);

        FileParser parser = new FileParser("", "");
        parser.registerRecordCallback("test", callback);

        assertTrue("RecordCallback was not successfully registered or unregistered.", parser.removeRecordCallback("test", callback));
    }

    @Test
    public void executeRecordCallback() {
        RecordCallback callback = mock(RecordCallback.class);
        MatchedRecord matchedRecord = mock(MatchedRecord.class);

        FileParser parser = new FileParser("", "");
        parser.registerRecordCallback("test", callback);

        // TODO when there is more time - need to have content and config embedded into the test.
//        verify(callback, atMost(1)).processRecord(matchedRecord);
    }

    @Test
    public void registerAndUnregisterExceptionCallback() {
        ExceptionCallback callback = mock(ExceptionCallback.class);

        FileParser parser = new FileParser("", "");
        parser.registerExceptionCallback(callback);

        assertTrue("ExceptionCallback was not successfully registered or unregistered.", parser.removeExceptionCallback(callback));
    }
}
