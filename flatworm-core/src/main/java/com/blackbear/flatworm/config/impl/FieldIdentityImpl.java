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

import com.google.common.base.Joiner;

import com.blackbear.flatworm.FileFormat;
import com.blackbear.flatworm.config.LineBO;
import com.blackbear.flatworm.config.LineToken;
import com.blackbear.flatworm.config.RecordBO;
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
public class FieldIdentityImpl extends AbstractLineTokenIdentity {

    @Getter
    @Setter
    private Integer startPosition;

    @Getter
    @Setter
    private Integer fieldLength;

    @Getter
    @Setter
    private Set<String> matchingStrings;

    @Getter
    private boolean ignoreCase;

    public FieldIdentityImpl() {
        matchingStrings = new HashSet<>();
        ignoreCase = false;
    }

    public FieldIdentityImpl(boolean ignoreCase) {
        this();
        this.ignoreCase = ignoreCase;
    }

    public void addMatchingString(String matchingString) {
        String valueToAdd = matchingString;
        if (ignoreCase) {
            valueToAdd = valueToAdd.toLowerCase();
        }

        matchingStrings.add(valueToAdd);
    }

    /**
     * Determine if the given {@link RecordBO} instance should be used to parse the line.
     *
     * @param record     The {@link RecordBO} instance.
     * @param fileFormat The {@link FileFormat} instance representing the configuration that is driving the parsing and the last line that
     *                   was read.
     * @param line       The line of data to be evaluated.
     * @return {@code true} the {@code line} of data has the appropriate identity labels (i.e. it matches one of the {@code matchingString}
     * instances in the spot identified by the {@code startPosition} and is within the length of the {@code fieldLength} specified.
     * @throws FlatwormParserException should the script engine fail to invoke the script or should the return converterName of the script
     *                                 not be a {@code boolean} value.
     */
    @Override
    public boolean matchesIdentity(RecordBO record, FileFormat fileFormat, String line) throws FlatwormParserException {
        if(line == null) return false;

        boolean matchesLine = false;
        if (line.length() < startPosition + fieldLength) {
            matchesLine = false;
        } else {
            for (String matchingString : matchingStrings) {
                if (line.regionMatches(ignoreCase, startPosition, matchingString, 0, fieldLength)) {
                    matchesLine = true;
                    break;
                }
            }
        }
        return matchesLine;
    }

    /**
     * Write out all delimiters to the given {@link BufferedWriter instance{.}}
     *
     * @param writer The {@link BufferedWriter} to write to.
     * @param record The {@link RecordBO} instance currently being processed.
     * @param line   The {@link LineBO} instance currently being processed.
     * @throws IOException should an I/O exception occur.
     */
    @Override
    public void write(BufferedWriter writer, RecordBO record, LineBO line) throws IOException {
        // TODO [AH] not sure about this approach as it writes out all matching strings instead of the primary one.
        // TODO [AH] I think the update might be an indicator as to the primary token and if set, only the primary is written - else, all.
        String delimit = line.getDelimiter() != null ? line.getDelimiter() : "";
        writer.write(Joiner.on(delimit).join(matchingStrings));
    }

    /**
     * See if the given {@link LineToken} instance matches any of the registered matching strings.
     *
     * @param lineToken The {@link LineToken} instance to evaluate.
     * @return {@code true} if a match is found and {@code false} if not.
     */
    @Override
    public boolean matchesIdentity(LineToken lineToken) {
        boolean matches = false;

        if (startPosition != null) {
            matches = lineToken.getColumnPosition() == startPosition;
        }

        if (matches && fieldLength != null) {
            matches = lineToken.getFullTokenLength() == fieldLength;
        }

        if (matches) {
            String comparisonValue = lineToken.getToken();
            if (ignoreCase) {
                comparisonValue = comparisonValue.toLowerCase();
            }

            matches = matchingStrings.contains(comparisonValue);
        }

        return matches;
    }

    @Override
    public boolean matchesIdentity(String token) {
        String tokenToTest = token;
        if(ignoreCase) {
            tokenToTest = tokenToTest.toLowerCase();
        }
        return matchingStrings.contains(tokenToTest);
    }
}
