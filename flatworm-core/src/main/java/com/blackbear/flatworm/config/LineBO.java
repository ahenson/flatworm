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

import com.google.common.base.Strings;

import com.blackbear.flatworm.BeanMappingStrategy;
import com.blackbear.flatworm.CardinalityMode;
import com.blackbear.flatworm.ParseUtils;
import com.blackbear.flatworm.PropertyUtilsMappingStrategy;
import com.blackbear.flatworm.Util;
import com.blackbear.flatworm.converters.ConversionHelper;
import com.blackbear.flatworm.errors.FlatwormParserException;

import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * BeanBO class used to store the values from the LineBO XML tag
 */
@Slf4j
public class LineBO extends AbstractLineElementCollection {
    private ConversionHelper conversionHelper;
    private Map<String, Object> beans;
    private BeanMappingStrategy mappingStrategy = new PropertyUtilsMappingStrategy();

    // properties used for processing delimited input
    private List<LineToken> lineTokens;
    private int currentField = 0;

    @Getter
    @Setter
    private String delimiter;

    @Getter
    private char quoteChar = '\0';

    @Getter
    @Setter
    private RecordDefinitionBO parentRecordDefinition;

    @Getter
    @Setter
    private String id;
    
    @Getter
    @Setter
    private ScriptletBO beforeScriptlet;

    @Getter
    @Setter
    private ScriptletBO afterScriptlet;
    
    public LineBO() {
    }

    /**
     * <b>NOTE:</b> Only the first character in the string is considered.
     *
     * @param quote The quote character that encompass the tokens in a delimited file.
     */
    public void setQuoteChar(String quote) {
        if (quote != null) {
            quoteChar = quote.charAt(0);
        }
    }

    public void setQuoteChar(char quote) {
        quoteChar = quote;
    }

    public boolean isDelimited() {
        return !StringUtils.isBlank(delimiter);
    }

    @Override
    public void addLineElement(LineElement recordElement) {
        recordElement.setParentLine(this);
        if(recordElement.getOrder() == null) {
            recordElement.setOrder(elements.size() + 1);
        }
        super.addLineElement(recordElement);
    }

    @Override
    public String toString() {
        return super.toString() + "[elements = " + elements + "]";
    }

    /**
     * @param inputLine        A single line from file to be parsed into its corresponding bean
     * @param beans            A HashMap containing a collection of beans which will be populated with parsed data
     * @param conversionHelper A ConversionHelper which aids in the conversion of datatypes and string formatting
     * @throws FlatwormParserException should any issues occur while parsing the data.
     */
    public void parseInput(String inputLine, Map<String, Object> beans, ConversionHelper conversionHelper) throws FlatwormParserException {
        this.conversionHelper = conversionHelper;
        this.beans = beans;

        if(beforeScriptlet != null) {
            beforeScriptlet.invokeFunction(this, inputLine, beans, conversionHelper);
        }
        
        // JBL - check for delimited status
        if (isDelimited()) {
            // Don't parse empty lines
            if (!Strings.isNullOrEmpty(inputLine)) {
                parseInputDelimited(inputLine);
            }
        }
        else {
            int charPos = 0;
            parseInput(inputLine, elements, charPos);
        }
        
        if(afterScriptlet != null) {
            afterScriptlet.invokeFunction(this, inputLine, beans, conversionHelper);
        }
    }

    /**
     * Parse out the content of the line based upon the configured {@link RecordElementBO} and {@link SegmentElementBO} instances.
     * @param inputLine The line of data to parse.
     * @param lineElements The {@link LineElement} instances that drive how the line of data will be parsed.
     * @param charPos The character position of the line to begin at.
     * @return The last characater position of the line that was processed.
     * @throws FlatwormParserException should the parsing fail for any reason.
     */
    private int parseInput(String inputLine, List<LineElement> lineElements, int charPos) throws FlatwormParserException {
        for (LineElement lineElement : lineElements) {
            if (lineElement instanceof RecordElementBO) {
                RecordElementBO recordElement = (RecordElementBO) lineElement;
                int start = charPos;
                int end = charPos;
                if (recordElement.isFieldStartSet())
                    start = recordElement.getFieldStart();
                if (recordElement.isFieldEndSet()) {
                    end = recordElement.getFieldEnd();
                    charPos = end;
                }
                if (recordElement.isFieldLengthSet()) {
                    end = start + recordElement.getFieldLength();
                    charPos = end;
                }
                if (end > inputLine.length())
                    throw new FlatwormParserException("Looking for field " + recordElement.getBeanRef()
                            + " at pos " + start + ", end " + end + ", input length = " + inputLine.length());
                String beanRef = recordElement.getBeanRef();
                if (beanRef != null) {
                    String fieldChars = inputLine.substring(start, end);

                    // JBL - to keep from dup. code, moved this to a private method
                    mapField(fieldChars, recordElement);
                }
            }
            else if (lineElement instanceof SegmentElementBO) {
                SegmentElementBO segmentElement = (SegmentElementBO) lineElement;
                charPos = parseInput(inputLine, segmentElement.getLineElements(), charPos);
                captureSegmentBean(segmentElement);
            }
        }
        return charPos;
    }

    /**
     * Convert string field from file into appropriate converterName and set bean's value<br>
     *
     * @param fieldChars    the raw string data read from the field
     * @param recordElement the RecordElementBO, which contains detailed information about the field
     * @throws FlatwormParserException should any issues occur while parsing the data.
     */
    private void mapField(String fieldChars, RecordElementBO recordElement) throws FlatwormParserException {
        String beanRef = recordElement.getBeanRef();
        int posOfFirstDot = beanRef.lastIndexOf('.');
        String beanName = beanRef.substring(0, posOfFirstDot);
        String property = beanRef.substring(posOfFirstDot + 1);
        Object bean = beans.get(beanName);

        Object value;
        if (!StringUtils.isBlank(recordElement.getConverterName())) {
            // Using the configuration based approach.
            value = conversionHelper.convert(recordElement.getConverterName(), fieldChars, recordElement.getConversionOptions(),
                    recordElement.getBeanRef());
        } else {
            // Use the reflection approach.
            value = conversionHelper.convert(bean, beanName, property, fieldChars, recordElement.getConversionOptions());
        }

        mappingStrategy.mapBean(bean, beanName, property, value, recordElement.getConversionOptions());
    }

    /**
     * For non-delimited lines that are under a {@link SegmentElementBO} instance, capture them into the parent bean.
     * @param segmentElement The {@link SegmentElementBO} instance being processed.
     * @throws FlatwormParserException should invoking the necessary reflection methods fail for any reason.
     */
    private void captureSegmentBean(SegmentElementBO segmentElement) throws FlatwormParserException {
        String beanRef = segmentElement.getBeanRef();
        String parentBeanRef = segmentElement.getParentBeanRef();
        String property = segmentElement.getPropertyName();
        
        Object parent = beans.get(parentBeanRef);
        Object toAdd = beans.get(beanRef);
        
        if(segmentElement.getCardinalityMode() == CardinalityMode.SINGLE) {
            ParseUtils.setProperty(parent, property, toAdd);
        }
        else {
            ParseUtils.addValueToCollection(segmentElement, parent, toAdd);
        }
    }
    
    /**
     * Convert string field from file into appropriate converterName and set bean's value. This is used for delimited files only<br>
     *
     * @param inputLine the line of data read from the data file
     * @throws FlatwormParserException should any issues occur while parsing the data.
     */
    private void parseInputDelimited(String inputLine) throws FlatwormParserException {

        char split = delimiter.charAt(0);
        if (delimiter.length() == 2 && delimiter.charAt(0) == '\\') {
            char specialChar = delimiter.charAt(1);
            switch (specialChar) {
                case 't':
                    split = '\t';
                    break;
                case 'n':
                    split = '\n';
                    break;
                case 'r':
                    split = '\r';
                    break;
                case 'f':
                    split = '\f';
                    break;
                case '\\':
                    split = '\\';
                    break;
                default:
                    break;
            }
        }
        lineTokens = Util.split(inputLine, split, quoteChar);
        cleanupLineTokens();
        currentField = 0;
        doParseDelimitedInput(elements);
    }

    /**
     * Remove any record-level, {@link LineTokenIdentity} instance tokens from the list of line tokens so that they don't affect the
     * processing of the data elements. Additional, for any place where there are segment-records, the {@link LineToken} instances need to
     * have the positional information updated to reflect that they are virtually at the head of the segment-record even though they aren't
     * at the head of the line.
     */
    private void cleanupLineTokens() {
        Iterator<LineToken> lineTokenIterator = lineTokens.iterator();
        while (lineTokenIterator.hasNext()) {
            if (parentRecordDefinition.getParentRecord().matchesIdentifier(lineTokenIterator.next())) {
                lineTokenIterator.remove();
            }
        }
    }

    private void doParseDelimitedInput(List<LineElement> elements) throws FlatwormParserException {
        for (LineElement lineElement : elements) {
            if (lineElement instanceof RecordElementBO) {
                try {
                    RecordElementBO recordElement = RecordElementBO.class.cast(lineElement);
                    parseDelimitedRecordElement(recordElement, lineTokens.get(currentField).getToken());
                    ++currentField;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    log.warn("Ran out of data on field " + (currentField + 1));
                }
            } else if (lineElement instanceof SegmentElementBO) {
                parseDelimitedSegmentElement(SegmentElementBO.class.cast(lineElement));
            }
        }
    }

    private void parseDelimitedRecordElement(RecordElementBO recordElement, String fieldStr) throws FlatwormParserException {
        if (!recordElement.getIgnoreField()) {
            // JBL - to keep from dup. code, moved this to a private method
            mapField(fieldStr, recordElement);
        }
    }

    private void parseDelimitedSegmentElement(SegmentElementBO segment) throws FlatwormParserException {
        int minCount = segment.getMinCount();
        int maxCount = segment.getMaxCount();
        if (maxCount <= 0) {
            maxCount = Integer.MAX_VALUE;
        }
        if (minCount < 0) {
            minCount = 0;
        }

        String beanRef = segment.getBeanRef();
        if (currentField < lineTokens.size() && !segment.matchesIdentity(lineTokens.get(currentField)) && minCount > 0) {
            log.error("Segment " + segment.getPropertyName() + " with minimum required count of " + minCount + " missing.");
        }
        int cardinality = 0;
        try {
            while (currentField < lineTokens.size() && segment.matchesIdentity(lineTokens.get(currentField))) {
                currentField++; // Advanced past the identifier token.
                if (beanRef != null) {
                    ++cardinality;
                    String parentRef = segment.getParentBeanRef();
                    if (parentRef != null) {
                        Object instance = ParseUtils.newBeanInstance(beans.get(beanRef));
                        beans.put(beanRef, instance);
                        // Handle collections
                        if (segment.getCardinalityMode() != CardinalityMode.SINGLE) {
                            if (cardinality > maxCount) {
                                if (segment.getCardinalityMode() == CardinalityMode.STRICT) {
                                    throw new FlatwormParserException("Cardinality exceeded with mode set to STRICT");
                                } else if (segment.getCardinalityMode() != CardinalityMode.RESTRICTED) {
                                    ParseUtils.addValueToCollection(segment, beans.get(parentRef), instance);
                                }
                            } else {
                                ParseUtils.addValueToCollection(segment, beans.get(parentRef), instance);
                            }
                        }
                        // Handle single child instances.
                        else {
                            ParseUtils.setProperty(beans.get(parentRef), segment.getPropertyName(), instance);
                        }
                    }
                    doParseDelimitedInput(segment.getLineElements());
                }
            }
        } finally {
            if (cardinality > maxCount) {
                log.error("Segment '" + segment.getPropertyName() + "' with maximum of " + maxCount
                        + " encountered actual count of " + cardinality);
            }
        }
    }
}