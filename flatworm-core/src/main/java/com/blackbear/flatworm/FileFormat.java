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
import com.blackbear.flatworm.errors.FlatwormUnsetFieldValueException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>FileFormat</code> is the point of entry into the Flatworm parser.
 * It is generated from the XML Flatworm description, and can then be used to
 * read a line or lines from a file, determine if there is a matching record
 * definition for the line(s), and return a <code>HashMap</code> with the beans
 * created by parsing the input.
 */

public class FileFormat {
    private static Log log = LogFactory.getLog(FileFormat.class);

    private Map<String, Record> records;
    private List<Record> recordOrder;
    private ConversionHelper convHelper = null;
    // JBL - Used when parsing fails, gives access to bad line
    private String lastLine = "";
    private String encoding;

    public FileFormat() {
        records = new HashMap<String, Record>();
        recordOrder = new ArrayList<Record>();

        // JBL
        convHelper = new ConversionHelper();
    }

    // JBL - getter
    public String getLastLine() {
        return lastLine;
    }

    public Map<String, Record> getRecords() {
        return Collections.unmodifiableMap(records);
    }

    public void setRecords(Map<String, Record> records) {
        this.records.clear();
        this.records.putAll(records);
    }

    public void addRecord(Record r) {
        records.put(r.getName(), r);
        recordOrder.add(r);
    }

    public Record getRecord(String name) {
        return records.get(name);
    }

    private Record findMatchingRecord(String firstLine) {
        for (int i = 0; i < recordOrder.size(); i++) {
            Record record = recordOrder.get(i);
            if (record.matchesLine(firstLine, this)) {
                return record;
            }
        }
        return null;
    }

    /**
     * Facilitates the storage of multiple converters. However, actual storage is
     * delegated to the ConversionHelper class.
     *
     * @param converter
     *          The Converter to store
     */
    public void addConverter(Converter converter) {
        convHelper.addConverter(converter);
    }

    /**
     * FileFormat is the keeper of ConversionHelper, but does not actually use it.
     * This allows access
     *
     * @return ConversionHelper
     */
    public ConversionHelper getConvertionHelper() {
        return convHelper;
    }

    /**
     * When called with a <code>BufferedReader</code>, reads sufficient lines to
     * parse a record, and returns the beans created.
     *
     * @param in
     *          The stream to read from
     * @return The created beans in a MatchedRecord object
     * @throws FlatwormInvalidRecordException
     * @throws FlatwormInputLineLengthException
     * @throws FlatwormConversionException
     * @throws FlatwormUnsetFieldValueException
     * @throws FlatwormCreatorException
     */
    public MatchedRecord getNextRecord(BufferedReader in) throws FlatwormInvalidRecordException,
            FlatwormInputLineLengthException, FlatwormConversionException,
            FlatwormUnsetFieldValueException, FlatwormCreatorException {
        try {
            String firstLine;
            firstLine = in.readLine();
            lastLine = firstLine;

            if (firstLine == null)
                return null;
            Record rd;
            rd = this.findMatchingRecord(firstLine);
            if (rd == null)
                throw new FlatwormInvalidRecordException("Unmatched line in input file");

            Map<String, Object> beans = rd.parseRecord(firstLine, in, convHelper);
            return new MatchedRecord(rd.getName(), beans);
        } catch (IOException e) {
            return null;
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

}
