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

import com.blackbear.flatworm.errors.FlatwormConversionException;
import com.blackbear.flatworm.errors.FlatwormCreatorException;
import com.blackbear.flatworm.errors.FlatwormInputLineLengthException;
import com.blackbear.flatworm.errors.FlatwormInvalidRecordException;
import com.blackbear.flatworm.errors.FlatwormParserException;
import com.blackbear.flatworm.errors.FlatwormUnsetFieldValueException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The <code>FileFormat</code> is the point of entry into the Flatworm parser. It is generated from the XML Flatworm description, and can
 * then be used to read a line or lines from a file, determine if there is a matching record definition for the line(s), and return a
 * <code>HashMap</code> with the beans created by parsing the input.
 */
@Slf4j
public class FileFormat {
    private Map<String, Record> records;
    private List<Record> recordOrder;
    private ConversionHelper convHelper;

    @Getter
    private int lineNumber;

    // JBL - Used when parsing fails, gives access to bad line
    @Getter
    private String currentParsedLine = "";

    @Getter
    @Setter
    private String encoding;

    public FileFormat() {
        records = new HashMap<>();
        recordOrder = new ArrayList<>();
        currentParsedLine = "";
        lineNumber = 0;

        // JBL
        convHelper = new ConversionHelper();
    }

    public Map<String, Record> getRecords() {
        return Collections.unmodifiableMap(records);
    }

    public void addRecord(Record r) {
        records.put(r.getName(), r);
        recordOrder.add(r);
    }

    public Record getRecord(String name) {
        return records.get(name);
    }

    private Record findMatchingRecord(String firstLine) throws FlatwormParserException {
        Record result = null;
        for (Record record : recordOrder) {
            if (record.matchesLine(firstLine, this)) {
                result = record;
                break;
            }
        }
        return result;
    }

    /**
     * Facilitates the storage of multiple converters. However, actual storage is delegated to the ConversionHelper class.
     *
     * @param converter The Converter to store
     */
    public void addConverter(Converter converter) {
        convHelper.addConverter(converter);
    }

    /**
     * FileFormat is the keeper of ConversionHelper, but does not actually use it. This allows access.
     *
     * @return ConversionHelper
     */
    public ConversionHelper getConversionHelper() {
        return convHelper;
    }

    /**
     * When called with a {@code BufferedReader}, reads sufficient lines to parse a record, and returns the beans created.
     *
     * @param in The stream to read from
     * @return The created beans in a MatchedRecord object
     */
    public MatchedRecord nextRecord(BufferedReader in) throws FlatwormInvalidRecordException,
            FlatwormInputLineLengthException, FlatwormConversionException,
            FlatwormUnsetFieldValueException, FlatwormCreatorException,
            FlatwormParserException, IOException {

        MatchedRecord matchedRecord = null;
        currentParsedLine = in.readLine();
        lineNumber++;

        if (currentParsedLine != null) {
            Record rd = findMatchingRecord(currentParsedLine);
            if (rd == null)
                throw new FlatwormInvalidRecordException(String.format(
                        "Configuration not found for line in input file [line: %d] - %s", lineNumber, currentParsedLine
                ));

            Map<String, Object> beans = rd.parseRecord(currentParsedLine, in, convHelper);
            matchedRecord = new MatchedRecord(rd.getName(), beans);
        }

        return matchedRecord;
    }
}
