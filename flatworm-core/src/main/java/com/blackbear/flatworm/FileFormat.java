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

import com.blackbear.flatworm.config.ConverterBO;
import com.blackbear.flatworm.config.RecordBO;
import com.blackbear.flatworm.converters.ConversionHelper;
import com.blackbear.flatworm.errors.FlatwormParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
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

    @Getter
    private ConversionHelper conversionHelper;

    private Map<String, RecordBO> records;
    private List<RecordBO> recordOrder;

    @Getter
    private int lineNumber;

    // JBL - Used when parsing fails, gives access to bad line
    @Getter
    private String currentParsedLine = "";

    @Getter
    @Setter
    private String encoding;

    @Getter
    @Setter
    private boolean ignoreUnmappedRecords;

    @Getter
    private MatchedRecord lastRecordRead;
    
    @Getter
    private RecordBO lastParsingRecord;

    public FileFormat() {
        records = new HashMap<>();
        recordOrder = new ArrayList<>();
        currentParsedLine = "";
        lineNumber = 0;

        // JBL
        conversionHelper = new ConversionHelper();
    }

    public List<RecordBO> getRecords() {
        return new ArrayList<>(records.values());
    }

    public void addRecord(RecordBO r) {
        r.setParentFileFormat(this);
        records.put(r.getName(), r);
        recordOrder.add(r);
    }

    public RecordBO getRecord(String name) {
        return records.get(name);
    }

    private RecordBO findMatchingRecord(String firstLine) throws FlatwormParserException {
        RecordBO result = null;
        for (RecordBO record : recordOrder) {
            if (record.matchesLine(this, firstLine)) {
                result = record;
                break;
            }
        }
        
        return result;
    }

    /**
     * See if any of the {@code RecordBO} instances collected thus far are "default" records in that they lack a record identifier.
     *
     * @return {@code true} if there is a {@code RecordBO} that lacks an identifier.
     */
    public boolean hasDefaultRecord() {
        return records.values()
                .stream()
                .filter(record -> record.getRecordIdentity() == null)
                .findAny()
                .isPresent();
    }

    /**
     * Facilitates the storage of multiple converters. However, actual storage is delegated to the ConversionHelper class.
     *
     * @param converter The ConverterBO to store
     */
    public void addConverter(ConverterBO converter) {
        conversionHelper.addConverter(converter);
    }

    /**
     * When called with a {@code BufferedReader}, reads sufficient lines to parse a record, and returns the beans created.
     *
     * @param in The stream to read from. Note that the reader is not closed by this method so the caller must perform the {@code close()}
     *           operation on the reader.
     * @return The created beans in a MatchedRecord object.
     * @throws FlatwormParserException should an issue occur while parsing the data content.
     * @throws IOException             Should an I/O issue occur.
     */
    public MatchedRecord nextRecord(BufferedReader in) throws FlatwormParserException, IOException {

        MatchedRecord matchedRecord = null;
        if (lastParsingRecord == null || lastParsingRecord.isParsedLastReadLine()) {
            currentParsedLine = in.readLine();
            lineNumber++;
        } else if(lastParsingRecord != null) {
            currentParsedLine = lastParsingRecord.getLastReadLine();
        }

        if (currentParsedLine != null) {
            RecordBO record = findMatchingRecord(currentParsedLine);
            if (record != null) {
                lastParsingRecord = record;
                if (record.getBeforeScriptlet() != null) {
                    record.getBeforeScriptlet().invokeFunction(this, currentParsedLine);
                }

                Map<String, Object> beans = record.parseRecord(currentParsedLine, in, conversionHelper);
                matchedRecord = new MatchedRecord(record.getName(), beans, currentParsedLine);
                lastRecordRead = matchedRecord;

                if (record.getAfterScriptlet() != null) {
                    record.getAfterScriptlet().invokeFunction(this);
                }
            } else if (!ignoreUnmappedRecords) {
                throw new FlatwormParserException(String.format(
                        "Configuration not found for line in input file [line: %d] - %s", lineNumber, currentParsedLine
                ));
            }
        }

        return matchedRecord;
    }

    /**
     * Manually provide the next data to be parsed.
     *
     * @param line The line of data that should be parsed - this could be multiple lines if the {@code line.separator} is used to separate
     *             the lines within the {@code line} parameter.
     * @return The {@link MatchedRecord} for the given data if the data could be parsed.
     * @throws FlatwormParserException should an issue occur while parsing the data content.
     */
    public MatchedRecord nextRecord(String line) throws FlatwormParserException {
        MatchedRecord matchedRecord;
        try {
            matchedRecord = nextRecord(new BufferedReader(new StringReader(line)));
        } catch (Exception e) {
            throw new FlatwormParserException(e.getMessage(), e);
        }
        return matchedRecord;
    }

    /**
     * Manually provide the next data to be parsed.
     *
     * @param lines The lines of data that should be parsed - this wll be appended together with the correct system-based {@code
     *              line.separator}.
     * @return The {@link MatchedRecord} for the given data if the data could be parsed.
     * @throws FlatwormParserException should an issue occur while parsing the data content.
     */
    public MatchedRecord nextRecord(List<String> lines) throws FlatwormParserException {
        MatchedRecord matchedRecord;
        try {
            StringBuilder builder = new StringBuilder();
            lines.forEach(line -> builder.append(line).append(String.format("%n")));
            matchedRecord = nextRecord(new BufferedReader(new StringReader(builder.toString())));
        } catch (Exception e) {
            throw new FlatwormParserException(e.getMessage(), e);
        }
        return matchedRecord;
    }
}
