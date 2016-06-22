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

package com.blackbear.flatworm.config;

import com.blackbear.flatworm.FileFormat;
import com.blackbear.flatworm.errors.FlatwormParserException;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Contract for identifying whether a line of data should be parsed by the given {@link RecordBO} instance.
 *
 * @author Alan Henson
 */
public interface Identity {

    /**
     * Determine if the given RecordBO should be used to parse the line.
     *
     * @param record     The {@link RecordBO} instance that is a candidate for performing the parsing.
     * @param fileFormat The {@link FileFormat} instance representing the configuration that is driving the parsing and the last line that
     *                   was read.
     * @param line       The line of data to be evaluated.
     * @return {@code true} if the {@link RecordBO} instance should be used to perform the parsing and {@code false} if not.
     * @throws FlatwormParserException should the {@code Identity} instance determine that something egregious happened while determining if
     *                                 the parsing should be handled by the {@link RecordBO} instance.
     */
    boolean matchesIdentity(RecordBO record, FileFormat fileFormat, String line) throws FlatwormParserException;

    /**
     * Determine if the given LineBO should be used to parse the line.
     *
     * @param line       The {@link LineBO} instance that is a candidate for performing the parsing.
     * @param fileFormat The {@link FileFormat} instance representing the configuration that is driving the parsing and the last line that
     *                   was read.
     * @param dataLine   The line of data to be evaluated.
     * @return {@code true} if the {@link RecordBO} instance should be used to perform the parsing and {@code false} if not.
     * @throws FlatwormParserException should the {@code Identity} instance determine that something egregious happened while determining if
     *                                 the parsing should be handled by the {@link RecordBO} instance.
     */
    boolean matchesIdentity(LineBO line, FileFormat fileFormat, String dataLine) throws FlatwormParserException;

    /**
     * Write out the Identity value, if necessary.
     *
     * @param writer The {@link BufferedWriter} to write to.
     * @param record The {@link RecordBO} instance currently being processed.
     * @param line   The {@link LineBO} instance currently being processed.
     * @throws IOException should an issue occur writing out to the {@code writer}.
     */
    void write(BufferedWriter writer, RecordBO record, LineBO line) throws IOException;
}
