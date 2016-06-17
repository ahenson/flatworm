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
import com.blackbear.flatworm.config.Line;
import com.blackbear.flatworm.config.LineToken;
import com.blackbear.flatworm.config.Record;
import com.blackbear.flatworm.config.impl.AbstractIdentity;
import com.blackbear.flatworm.errors.FlatwormParserException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

/**
 * Manages field identity instances (field-ident) found in the flatworm configuration.
 *
 * @author Alan Henson
 */
public class FieldIdentity extends AbstractLineTokenIdentity {

    @Getter
    @Setter
    private Integer startPosition;

    @Getter
    @Setter
    private Integer fieldLength;

    @Getter
    @Setter
    private Set<String> matchingStrings;

    public FieldIdentity() {
        matchingStrings = new HashSet<>();
    }

    public void addMatchingString(String matchingString) {
        matchingStrings.add(matchingString);
    }

    /**
     * Determine if the given {@link Record} instance should be used to parse the line.
     *
     * @param record     The {@link Record} instance.
     * @param fileFormat The {@link FileFormat} instance representing the configuration that is driving the parsing and the last line that
     *                   was read.
     * @param line       The line of data to be evaluated.
     * @return {@code true} the {@code line} of data has the appropriate identity labels (i.e. it matches one of the {@code matchingString}
     * instances in the spot identified by the {@code startPosition} and is within the length of the {@code fieldLength} specified.
     * @throws FlatwormParserException should the script engine fail to invoke the script or should the return converterName of the script not be a
     *                                 {@code boolean} value.
     */
    @Override
    public boolean doesMatch(Record record, FileFormat fileFormat, String line) throws FlatwormParserException {
        boolean matchesLine = false;
        if (line.length() < startPosition + fieldLength) {
            matchesLine = false;
        } else {
            for (String matchingString : matchingStrings) {
                if (line.regionMatches(startPosition, matchingString, 0, fieldLength)) {
                    matchesLine = true;
                    break;
                }
            }
        }
        return matchesLine;
    }

    @Override
    public void write(BufferedWriter writer, Record record, Line line) throws IOException {
        String delimit = line.getDelimiter() != null ? line.getDelimiter() : "";
        for (String matchingString : matchingStrings) {
            writer.write(matchingString + delimit);
        }
    }

    @Override
    public boolean matchesIdentity(LineToken lineToken) {
        boolean matches = false;

        if(startPosition != null) {
            matches = lineToken.getColumnPosition() == startPosition;
        }

        if(fieldLength != null) {
            matches = matches && lineToken.getFullTokenLength() == fieldLength;
        }

        matches = matches && matchingStrings.contains(lineToken.getToken());

        return matches;
    }
}
