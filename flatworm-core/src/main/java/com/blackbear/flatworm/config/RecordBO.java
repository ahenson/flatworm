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
import com.blackbear.flatworm.ParseUtils;
import com.blackbear.flatworm.converters.ConversionHelper;
import com.blackbear.flatworm.errors.FlatwormParserException;
import com.blackbear.flatworm.errors.UncheckedFlatwormParserException;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Class used to store the values from the RecordBO XML tag. Also aids in parsing and matching lines in the input file.
 */
@Data
@Slf4j
public class RecordBO {

    /**
     * Default code for a record's identity configuration.
     */
    public static final char DEFAULT_IDENTITY_CODE = '\0';

    /**
     * Identity code for records that are identified by an identity of some sort.
     */
    public static final char FIELD_IDENTITY_CODE = 'F';

    /**
     * Identity code for records that are identified by the length of the record.
     */
    public static final char LENGTH_IDENTITY_CODE = 'L';

    /**
     * Identify code for records that are identified using JavaScript.
     */
    public static final char SCRIPT_IDENTITY_CODE = 'S';

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private Identity recordIdentity;

    @Getter
    @Setter
    private RecordDefinitionBO recordDefinition;

    @Getter
    @Setter
    private CardinalityBO cardinality;

    @Getter
    @Setter
    private ScriptletBO beforeScriptlet;

    @Getter
    @Setter
    private ScriptletBO afterScriptlet;

    @Getter
    @Setter
    private FileFormat parentFileFormat;

    @Getter
    private boolean parsedLastReadLine;
    
    @Getter
    private String lastReadLine;
    
    public RecordBO() {
    }

    /**
     * Determine if this {@code RecordBO} instance is capable of parsing the given line.
     *
     * @param fileFormat the parent {@link FileFormat} instance that contains the master configuration.
     * @param line       the input line from the file being parsed.
     * @return {@code true} if the {@link RecordBO} instance identifies the line and can parse it and {@code false} if not.
     * @throws FlatwormParserException should the matching logic fail for any reason.
     */
    public boolean matchesLine(FileFormat fileFormat, String line) throws FlatwormParserException {
        boolean matchesLine = true;
        if (recordIdentity != null) {
            matchesLine = recordIdentity.matchesIdentity(this, fileFormat, line);
        }
        return matchesLine;
    }

    /**
     * Parse the record into the bean(s).
     *
     * @param firstLine        first line to be considered.
     * @param in               used to retrieve additional lines of input for parsing multi-line records.
     * @param conversionHelper used to help convert datatypes and format strings.
     * @return collection of beans populated with file data.
     * @throws FlatwormParserException should an error occur while parsing the data.
     */
    public Map<String, Object> parseRecord(String firstLine, BufferedReader in,
                                           ConversionHelper conversionHelper) throws FlatwormParserException {
        Map<String, Object> beans = new HashMap<>();
        try {

            // TODO - Reconsider this approach.
            Object beanObj;
            for (BeanBO bean : recordDefinition.getBeans()) {
                beanObj = bean.getBeanObjectClass().newInstance();
                beans.put(bean.getBeanName(), beanObj);
            }

            List<LineBO> lines = recordDefinition.getLines();
            List<LineBO> linesWithIdentities = recordDefinition.getLinesWithIdentities();
            lastReadLine = firstLine;
            
            // Process all of the sequential lines first - for a record there will always be at least one sequential line..
            for (int i = 0; i < lines.size(); i++) {
                LineBO line = lines.get(i);
                line.parseInput(lastReadLine, beans, conversionHelper, recordIdentity);
                addBeanToBean(line, beans);
                
                parsedLastReadLine = true;
                if (i + 1 < lines.size()) {
                    lastReadLine = in.readLine();
                }
            }

            if (!linesWithIdentities.isEmpty()) {
                boolean continueParsing = true;
                do {
                    lastReadLine = in.readLine();
                    Optional<LineBO> matchingLine = linesWithIdentities
                            .stream()
                            .filter(line -> {
                                try {
                                    return line.getLineIdentity().matchesIdentity(line, parentFileFormat, lastReadLine);
                                }
                                catch(FlatwormParserException e) {
                                    throw new UncheckedFlatwormParserException("Failed to run matchesIdentity on line: " + line, e);
                                }
                            })
                            .findFirst();
                    if(matchingLine.isPresent()) {
                        matchingLine.get().parseInput(lastReadLine, beans, conversionHelper, matchingLine.get().getLineIdentity());
                        addBeanToBean(matchingLine.get(), beans);
                        parsedLastReadLine = true;
                        
                        if(matchingLine.get().getRecordEndLine()) {
                            continueParsing = false;
                        }
                    }
                    else {
                        continueParsing = false;
                        parsedLastReadLine = false;
                    }
                }
                while(continueParsing);
            }

        } catch (Exception e) {
            throw new FlatwormParserException(e.getMessage(), e);
        }
        return beans;
    }

    /**
     * If the {@link LineBO} configuration results in the need to add a built out bean to another bean, do it here.
     * @param line The {@link LineBO} instance whose configuration information will be inspected to see if we need to add a bean to a bean.
     * @param beans The {@link Map} of loaded beans thus far.
     * @throws FlatwormParserException should invoking the reflective properties fail for any reason.
     */
    private void addBeanToBean(LineBO line, Map<String, Object> beans) throws FlatwormParserException {
        // See if this is a Field-defined line - meaning we need to update a bean.
        if(line.isPropertyLine()) {
            Object parentBean = beans.get(line.getCardinality().getParentBeanRef());
            Object toAdd = beans.get(line.getCardinality().getBeanRef());
            
            ParseUtils.addObjectToProperty(parentBean, toAdd, line.getCardinality());
            
            // Refresh in case we have to build another one.
            beans.put(line.getCardinality().getBeanRef(), ParseUtils.newBeanInstance(toAdd));
        }
    }
    
    @Override
    public String toString() {
        return "RecordBO{" +
                "name='" + name + '\'' +
                ", recordIdentity=" + recordIdentity +
                ", recordDefinition=" + recordDefinition +
                '}';
    }
}