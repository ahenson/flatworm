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

package com.blackbear.flatworm.config.impl;

import com.blackbear.flatworm.errors.FlatwormParserException;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test that the FieldIdentityImpl class works as expected.
 *
 * @author Alan Henson
 */
public class FieldIdentityTest extends AbstractBaseIdentityTest {

    @Test
    public void singleFieldTest() {
        try {
            String line = "DVDFrozen Disney";
            FieldIdentityImpl identity = new FieldIdentityImpl();
            identity.addMatchingString("DVD");
            identity.setStartPosition(0);
            identity.setFieldLength(3);
            boolean matches = identity.matchesIdentity(record, fileFormat, line);

            assertTrue("Failed to correctly match the line.", matches);

            // Negative test
            line = "VHSFrozen Disney";
            matches = identity.matchesIdentity(record, fileFormat, line);
            assertFalse("Incorrectly matched a field identity.", matches);
        } catch (FlatwormParserException e) {
            e.printStackTrace();
            fail("Failed to correctly parse field identity line.");
        }
    }

    @Test
    public void multiFieldTest() {
        try {
            String line = "BETAFrozen Disney";
            FieldIdentityImpl identity = new FieldIdentityImpl(true);
            identity.addMatchingString("DVD ");
            identity.addMatchingString("vhs ");
            identity.addMatchingString("BETA");
            identity.setStartPosition(0);
            identity.setFieldLength(4);

            boolean matches = identity.matchesIdentity(record, fileFormat, line);
            assertTrue("Failed to correctly match the line.", matches);

            line = "DVD Frozen Disney";
            matches = identity.matchesIdentity(record, fileFormat, line);
            assertTrue("Failed to correctly match the line.", matches);

            line = "VHS Frozen Disney";
            matches = identity.matchesIdentity(record, fileFormat, line);
            assertTrue("Failed to correctly match the line.", matches);

            // Negative test.
            line = "CD  Frozen Disney";
            matches = identity.matchesIdentity(record, fileFormat, line);
            assertFalse("Incorrectly matched a field identity.", matches);
        } catch (FlatwormParserException e) {
            e.printStackTrace();
            fail("Failed to correctly parse field identity line.");
        }
    }

    @Test
    public void invalidConditionTest() {
        try {
            String line = "";
            FieldIdentityImpl identity = new FieldIdentityImpl();
            identity.addMatchingString("DVD");
            identity.setStartPosition(0);
            identity.setFieldLength(3);
            boolean matches = identity.matchesIdentity(record, fileFormat, line);
            assertFalse("Failed to correctly match the line.", matches);

            matches = identity.matchesIdentity(record, fileFormat, null);
            assertFalse("Failed to correctly match the line.", matches);
        } catch (FlatwormParserException e) {
            e.printStackTrace();
            fail("Failed to correctly parse field identity line.");
        }

    }
}
