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

import lombok.Getter;
import lombok.Setter;

/**
 * Metadata Wrapper class around a token within a line of data that is being parsed. This helps keep track of where the parser is and what
 * data was extracted as a token.
 *
 * @author Alan Henson
 */
public class LineToken {
    @Getter
    @Setter
    private int columnPosition;

    @Getter
    private int fullTokenLength;

    @Getter
    private String token;

    /**
     * Create using the token that is the value to track as well as the current column position, which will be used with {@code
     * fullTokenLength} to back-calculate the column position where the token was found in the line.
     *
     * @param token                 The token to capture.
     * @param fullTokenLength   The actual length of the token when considering the quote characters that might have been present.
     * @param currentColumnPosition The current column position, which should be one column after where the token was read.
     */
    public LineToken(String token, int fullTokenLength, int currentColumnPosition) {
        this.token = token;
        this.fullTokenLength = fullTokenLength;
        this.columnPosition = currentColumnPosition - fullTokenLength;
    }
}
