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

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class UtilTest {
    @Test
    public void testRemoveBlanks() {
        HashMap options = new HashMap();
        assertEquals("foo", Util.justify("foo  ", "both", options, 0));
        assertEquals("foo", Util.justify("foo ", "both", options, 0));
        assertEquals("foo", Util.justify(" foo", "both", options, 0));
        assertEquals("foo", Util.justify("  foo", "both", options, 0));
        assertEquals("  foo", Util.justify("  foo  ", "left", options, 0));
        assertEquals("foo  ", Util.justify("  foo  ", "right", options, 0));
    }

    @Test
    public void testMultiplePadCharacters() {
        HashMap options = new HashMap();
        options.put("pad-character", new ConversionOption("pad-character", "0Oo"));
        assertEquals("f", Util.justify("foo", "both", options, 0));
        assertEquals("f", Util.justify("fooOO00", "both", options, 0));
        assertEquals("f", Util.justify("oofoo", "both", options, 0));
        assertEquals("f", Util.justify("oo00OOfooOO00", "both", options, 0));
        assertEquals("oof", Util.justify("oofoo", "left", options, 0));
        assertEquals("foo", Util.justify("oofoo", "right", options, 0));
    }
}
