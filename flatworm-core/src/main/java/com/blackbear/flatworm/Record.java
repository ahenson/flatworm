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

import org.apache.logging.log4j.util.Strings;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Class used to store the values from the Record XML tag. Also aids in parsing and matching lines in the input file.
 */
@Data
@Slf4j
@ToString
class Record {

    public static final String FIELD_IDENT_SCRIPT_ENTRY_METHOD_NAME = "matchesLine";

    private String name;
    private int lengthIdentMin;
    private int lengthIdentMax;
    private int fieldIdentStart;
    private int fieldIdentLength;
    private String fieldIdentScript;
    private List<String> fieldIdentMatchStrings;
    private char identTypeFlag;
    private RecordDefinition recordDefinition;
    private Invocable scriptInvocable;
    private ScriptEngine scriptEngine;

    public Record() {
        lengthIdentMin = 0;
        lengthIdentMax = 0;
        fieldIdentStart = 0;
        fieldIdentLength = 0;
        fieldIdentMatchStrings = new ArrayList<>();
        identTypeFlag = '\0';

        ScriptEngineManager engineManager = new ScriptEngineManager();
        scriptEngine = engineManager.getEngineByName("nashorn");
        scriptInvocable = (Invocable) scriptEngine;
    }

    public void addFieldIdentMatchString(String fieldIdentMatch) {
        fieldIdentMatchStrings.add(fieldIdentMatch);
    }

    /**
     * Determine if this {@code Record} instance is capable of parsing the given line.
     * @param line the input line from the file being parsed.
     * @param ff   not used at this time, for later expansion?
     * @return boolean does this line match according to the defined criteria?
     * @throws FlatwormParserException should the script function lack the {@code FIELD_IDENT_SCRIPT_ENTRY_METHOD_NAME} function, which
     * should take one parameter, the {@link FileFormat} instance - the method should return {@code true} if the line should
     * be parsed by this {@code Record} instance and {@code false} if not.
     */
    public boolean matchesLine(String line, FileFormat ff) throws FlatwormParserException {
        boolean matchesLine = true;
        switch (identTypeFlag) {

            // Recognition by value in a certain field
            // TODO: Will this work for delimited lines?
            // TODO: No - this won't - need some other way of identifying the record - thinking a line handler that maintains state.
            case 'F':
                if (line.length() < fieldIdentStart + fieldIdentLength) {
                    matchesLine = false;
                    break;
                } else {
                    matchesLine = false;
                    for (String matchingStrings : fieldIdentMatchStrings) {
                        if (line.regionMatches(fieldIdentStart, matchingStrings, 0, fieldIdentLength)) {
                            matchesLine = true;
                            break;
                        }
                    }
                    if(matchesLine) {
                        break;
                    }
                }
                matchesLine = false;
                break;

            // Recognition by length of line
            case 'L':
                matchesLine = line.length() >= lengthIdentMin && line.length() <= lengthIdentMax;
                break;
            case 'S':
                if(scriptInvocable != null && !Strings.isBlank(fieldIdentScript)) {
                    try {
                        Object result = scriptInvocable.invokeFunction(FIELD_IDENT_SCRIPT_ENTRY_METHOD_NAME, ff);
                        if(result instanceof Boolean) {
                            matchesLine = Boolean.class.cast(result);
                        }
                        else if(result != null) {
                            throw new FlatwormParserException(String.format("Record %s has a script identifier that does not return" +
                                    "a boolean value - a type of %s was returned.", name, result.getClass().getName()));
                        }
                    } catch (Exception e) {
                        throw new FlatwormParserException(e.getMessage(), e);
                    }
                }
                break;
        }
        return matchesLine;
    }

    public void setFieldIdentScript(String fieldIdentScript) throws ScriptException {
        this.fieldIdentScript = fieldIdentScript;
        if(scriptEngine != null && !Strings.isBlank(fieldIdentScript)) {
            scriptEngine.eval(this.fieldIdentScript);
        }
    }

    /**
     * Parse the record into the bean(s) <br>
     *
     * @param firstLine  first line to be considered
     * @param in         used to retrieve additional lines of input for parsing multi-line records
     * @param convHelper used to help convert datatypes and format strings
     * @return HashMap collection of beans populated with file data
     */
    public Map<String, Object> parseRecord(String firstLine, BufferedReader in,
                                           ConversionHelper convHelper) throws FlatwormInputLineLengthException,
            FlatwormConversionException, FlatwormUnsetFieldValueException,
            FlatwormInvalidRecordException, FlatwormCreatorException {
        Map<String, Object> beans = new HashMap<>();
        try {
            Map<String, Bean> beanHash = recordDefinition.getBeansUsed();
            Object beanObj;
            for (String beanName : beanHash.keySet()) {
                Bean bean = beanHash.get(beanName);
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
}