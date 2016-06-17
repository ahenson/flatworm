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

import com.blackbear.flatworm.config.Identity;
import com.blackbear.flatworm.config.Line;
import com.blackbear.flatworm.config.Record;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Abstract implementation of {@link Identity} to provide helper methods and default implementations of some methods.
 *
 * @author Alan Henson
 */
public abstract class AbstractIdentity implements Identity {

    /**
     * Writes nothing.
     * @param writer The {@link BufferedWriter} to write to.
     * @param record The {@link Record} instance currently being processed.
     * @param line The {@link Line} instance currently being processed.
     * @throws IOException never thrown as no activity is taken against the {@code writer}.
     */
    @Override
    public void write(BufferedWriter writer, Record record, Line line) throws IOException {
        // do nothing.
    }
}
