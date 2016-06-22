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

import com.blackbear.flatworm.FileFormat;
import com.blackbear.flatworm.config.LineBO;
import com.blackbear.flatworm.config.RecordBO;
import com.blackbear.flatworm.errors.FlatwormParserException;

import java.io.BufferedWriter;
import java.io.IOException;

import lombok.Getter;
import lombok.Setter;

/**
 * Manages length identity instances (length-ident) found in the flatworm configuration.
 *
 * @author Alan Henson
 */
public class LengthIdentityImpl extends AbstractIdentity {
    @Getter
    @Setter
    private Integer minLength;

    @Getter
    @Setter
    private Integer maxLength;

    /**
     * Determine if the given {@link RecordBO} instance should be used to parse the line.
     *
     * @param record     The {@link RecordBO} instance.
     * @param fileFormat The {@link FileFormat} instance representing the configuration that is driving the parsing and the last line that
     *                   was read.
     * @param line       The line of data to be evaluated.
     * @return {@code true} the {@code line} of data fits within the {@code minLength} and {@code maxLength} ranges provided for this {@link
     * FieldIdentityImpl} instance.
     * @throws FlatwormParserException should the script engine fail to invoke the script or should the return converterName of the script
     *                                 not be a {@code boolean} value.
     */
    @Override
    public boolean matchesIdentity(RecordBO record, FileFormat fileFormat, String line) throws FlatwormParserException {
        return doesMatchIdentity(line);
    }

    @Override
    public void write(BufferedWriter writer, RecordBO record, LineBO line) throws IOException {
        super.write(writer, record, line);
    }

    /**
     * Determine if the given LineBO should be used to parse the line.
     *
     * @param line       The {@link LineBO} instance that is a candidate for performing the parsing.
     * @param fileFormat The {@link FileFormat} instance representing the configuration that is driving the parsing and the last line that
     *                   was read.
     * @param dataLine   The line of data to be evaluated.
     * @return {@code true} the {@code line} of data fits within the {@code minLength} and {@code maxLength} ranges provided for this {@link
     * FieldIdentityImpl} instance.
     * @throws FlatwormParserException should the script engine fail to invoke the script or should the return converterName of the script
     *                                 not be a {@code boolean} value.
     */
    @Override
    public boolean matchesIdentity(LineBO line, FileFormat fileFormat, String dataLine) throws FlatwormParserException {
        return doesMatchIdentity(dataLine);
    }

    /**
     * Perform the matching logic using the full configuration.
     *
     * @param line The data line to test.
     * @return {@code true} the {@code line} of data fits within the {@code minLength} and {@code maxLength} ranges provided for this {@link
     * FieldIdentityImpl} instance.
     */
    private boolean doesMatchIdentity(String line) {
        return line.length() >= minLength && line.length() <= maxLength;
    }

    @Override
    public String toString() {
        return "LengthIdentityImpl{" +
                "minLength=" + minLength +
                ", maxLength=" + maxLength +
                '}';
    }
}
