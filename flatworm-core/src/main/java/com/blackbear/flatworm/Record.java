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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Class used to store the values from the Record XML tag. Also aids in parsing
 * and matching lines in the inputfile.
 */

class Record {
    private static Log log = LogFactory.getLog(Record.class);

    private String name;
    private int lengthIdentMin;
    private int lengthIdentMax;
    private int fieldIdentStart;
    private int fieldIdentLength;
    private List<String> fieldIdentMatchStrings;
    private char identTypeFlag;
    private RecordDefinition recordDefinition;

    public Record() {
        lengthIdentMin = 0;
        lengthIdentMax = 0;
        fieldIdentStart = 0;
        fieldIdentLength = 0;
        fieldIdentMatchStrings = new ArrayList<String>();
        identTypeFlag = '\0';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLengthIdentMin() {
        return lengthIdentMin;
    }

    public void setLengthIdentMin(int lengthIdentMin) {
        this.lengthIdentMin = lengthIdentMin;
    }

    public int getLengthIdentMax() {
        return lengthIdentMax;
    }

    public void setLengthIdentMax(int lengthIdentMax) {
        this.lengthIdentMax = lengthIdentMax;
    }

    public int getFieldIdentLength() {
        return fieldIdentLength;
    }

    public void setFieldIdentLength(int fieldIdentLength) {
        this.fieldIdentLength = fieldIdentLength;
    }

    public List<String> getFieldIdentMatchStrings() {
        return fieldIdentMatchStrings;
    }

    public void setFieldIdentMatchStrings(List<String> fieldIdentMatchStrings) {
        this.fieldIdentMatchStrings = fieldIdentMatchStrings;
    }

    public void addFieldIdentMatchString(String s) {
        fieldIdentMatchStrings.add(s);
    }

    public char getIdentTypeFlag() {
        return identTypeFlag;
    }

    public void setIdentTypeFlag(char identTypeFlag) {
        this.identTypeFlag = identTypeFlag;
    }

    public RecordDefinition getRecordDefinition() {
        return recordDefinition;
    }

    public void setRecordDefinition(RecordDefinition recordDefinition) {
        this.recordDefinition = recordDefinition;
    }

    public int getFieldIdentStart() {
        return fieldIdentStart;
    }

    public void setFieldIdentStart(int fieldIdentStart) {
        this.fieldIdentStart = fieldIdentStart;
    }

    /**
     *
     * @param String
     *          line of input from the file
     * @param FileFormat
     *          not used at this time, for later expansion?
     * @return boolean does this line match according to the defined criteria?
     */
    public boolean matchesLine(String line, FileFormat ff) {
        switch (identTypeFlag) {

            // Recognition by value in a certain field
            // TODO: Will this work for delimited lines?
            case 'F':
                if (line.length() < fieldIdentStart + fieldIdentLength) {
                    return false;
                } else {
                    for (int i = 0; i < fieldIdentMatchStrings.size(); i++) {
                        String s = (String) fieldIdentMatchStrings.get(i);
                        if (line.regionMatches(fieldIdentStart, s, 0, fieldIdentLength)) {
                            return true;
                        }
                    }
                }
                return false;

            // Recognition by length of line
            case 'L':
                return line.length() >= lengthIdentMin && line.length() <= lengthIdentMax;
        }
        return true;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append(super.toString() + "[");
        b.append("name = " + getName());
        if (getIdentTypeFlag() == 'L')
            b.append(", identLength=(" + getLengthIdentMin() + "," + getLengthIdentMax() + ")");
        if (getIdentTypeFlag() == 'F')
            b.append(", identField=(" + getFieldIdentStart() + "," + getFieldIdentLength() + ","
                    + getFieldIdentMatchStrings().toString() + ")");
        if (getRecordDefinition() != null)
            b.append(", recordDefinition = " + getRecordDefinition().toString());
        b.append("]");
        return b.toString();
    }

    /**
     * Parse the record into the bean(s) <br>
     *
     * @param String
     *          first line to be considered
     * @param BufferedReader
     *          used to retrieve additional lines of input for parsing multi-line
     *          records
     * @param ConversionHelper
     *          used to help convert datatypes and format strings
     * @return HashMap collection of beans populated with file data
     * @throws FlatwormInvalidRecordException
     * @throws FlatwormCreatorException
     */
    public Map<String, Object> parseRecord(String firstLine, BufferedReader in,
                                           ConversionHelper convHelper) throws FlatwormInputLineLengthException,
            FlatwormConversionException, FlatwormUnsetFieldValueException,
            FlatwormInvalidRecordException, FlatwormCreatorException {
        Map<String, Object> beans = new HashMap<String, Object>();
        try {
            Map<String, Bean> beanHash = recordDefinition.getBeansUsed();
            String beanName;
            Object beanObj;
            for (Iterator<String> bean_it = beanHash.keySet().iterator(); bean_it.hasNext(); ) {
                beanName = bean_it.next();
                Bean bean = (Bean) beanHash.get(beanName);
                beanObj = bean.getBeanObjectClass().newInstance();
                beans.put(beanName, beanObj);
            }

            List<Line> lines = recordDefinition.getLines();
            String inputLine = firstLine;
            for (int i = 0; i < lines.size(); i++) {
                Line line = lines.get(i);
                line.parseInput(inputLine, beans, convHelper);
                if (i + 1 < lines.size())
                    inputLine = in.readLine();
            }

        } catch (SecurityException e) {
            log.error("Invoking method", e);
            throw new FlatwormConversionException("Couldn't invoke Method");
        } catch (IOException e) {
            log.error("Reading input", e);
            throw new FlatwormConversionException("Couldn't read line");
        } catch (InstantiationException e) {
            log.error("Creating bean", e);
            throw new FlatwormConversionException("Couldn't create bean");
        } catch (IllegalAccessException e) {
            log.error("No access to class", e);
            throw new FlatwormConversionException("Couldn't access class");
        }
        return beans;
    }

    private String[] getFieldNames() {
        List<String> names = new ArrayList<String>();
        List<Line> lines = recordDefinition.getLines();
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            List<LineElement> el = l.getElements();
            for (int j = 0; j < el.size(); j++) {
                LineElement re = el.get(j);
                names.add(re.getBeanRef());
            }

        }

        String propertyNames[] = new String[names.size()];
        for (int i = 0; i < names.size(); i++)
            propertyNames[i] = (String) names.get(i);

        return propertyNames;
    }

}