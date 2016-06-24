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
import java.util.Optional;

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
    private int index = -1;

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
    private Identity lineIdentity;

    @Getter
    @Setter
    CardinalityBO cardinality;

    @Getter
    @Setter
    private ScriptletBO beforeScriptlet;

    @Getter
    @Setter
    private ScriptletBO afterScriptlet;

    @Getter
    @Setter
    private Boolean recordStartLine;
    
    @Getter
    @Setter
    private Boolean recordEndLine;

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
        if (recordElement.getOrder() == null) {
            recordElement.setOrder(elements.size() + 1);
        }
        super.addLineElement(recordElement);
    }

    /**
     * @param inputLine        A single line from file to be parsed into its corresponding bean
     * @param beans            A HashMap containing a collection of beans which will be populated with parsed data
     * @param conversionHelper A ConversionHelper which aids in the conversion of data types and string formatting
     * @param identity         The {@link Identity} instance used to determine that this {@link LineBO} instance should parse this line.
     * @throws FlatwormParserException should any issues occur while parsing the data.
     */
    public void parseInput(String inputLine, Map<String, Object> beans, ConversionHelper conversionHelper, Identity identity)
            throws FlatwormParserException {
        this.conversionHelper = conversionHelper;
        this.beans = beans;

        if (beforeScriptlet != null) {
            beforeScriptlet.invokeFunction(this, inputLine, beans, conversionHelper);
        }

        // JBL - check for delimited status
        if (isDelimited()) {
            // Don't parse empty lines
            if (!Strings.isNullOrEmpty(inputLine)) {
                parseInputDelimited(inputLine, identity);
            }
        } else {
            // This is to help keep the configuration shorter in terms of what fields are required.
            int charPos = getStartingPosition(elements, identity);

            parseInput(inputLine, elements, charPos);
        }

        if (afterScriptlet != null) {
            afterScriptlet.invokeFunction(this, inputLine, beans, conversionHelper);
        }
    }

    /**
     * Determine the starting position for parsing the line using a non-delimited approach.
     *
     * @param elements The {@link LineBO} elements.
     * @param identity The {@link Identity} instance that was used to identify the line.
     * @return the starting position for parsing the line.
     */
    private int getStartingPosition(List<LineElement> elements, Identity identity) {
        int startPosition = 0;
        if (identity instanceof LineTokenIdentity) {
            Optional<RecordElementBO> recordElement = elements.stream()
                    .filter(element -> element instanceof RecordElementBO)
                    .map(RecordElementBO.class::cast)
                    .findFirst();

            if (recordElement.isPresent()
                    && (!recordElement.get().isFieldStartSet() || recordElement.get().getFieldStart() < 0)) {
                LineTokenIdentity lineTokenIdentity = LineTokenIdentity.class.cast(identity);
                startPosition = lineTokenIdentity.getLineParsingStartingPosition();
            }
        }

        return startPosition;
    }

    /**
     * Parse out the content of the line based upon the configured {@link RecordElementBO} and {@link SegmentElementBO} instances.
     *
     * @param inputLine    The line of data to parse.
     * @param lineElements The {@link LineElement} instances that drive how the line of data will be parsed.
     * @param charPos      The character position of the line to begin at.
     * @return The last character position of the line that was processed.
     * @throws FlatwormParserException should the parsing fail for any reason.
     */
    private int parseInput(String inputLine, List<LineElement> lineElements, int charPos)
            throws FlatwormParserException {
        
        boolean enforceLineLengths = getParentRecordDefinition().getParentRecord().isEnforceLineLengths();
        
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
                if (end > inputLine.length()) {
                    if (enforceLineLengths) {
                        throw new FlatwormParserException("Looking for field " + recordElement.getCardinality().getBeanRef()
                                + "." + recordElement.getCardinality().getPropertyName()
                                + " at pos " + start + ", end " + end + ", input length = " + inputLine.length());
                    } else {
                        end = charPos = inputLine.length();
                    }
                }
                if (recordElement.getCardinality().getBeanRef() != null) {
                    String fieldChars = inputLine.substring(start, end);

                    // JBL - to keep from dup. code, moved this to a private method
                    mapField(fieldChars, recordElement);
                }
            } else if (lineElement instanceof SegmentElementBO) {
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
        CardinalityBO cardinality = recordElement.getCardinality();
        String beanRef = cardinality.getBeanRef();
        String property = cardinality.getPropertyName();
        Object bean = beans.get(beanRef);

        if (recordElement.isTrimValue()) {
            fieldChars = fieldChars.trim();
        }

        Object value;
        if (!StringUtils.isBlank(recordElement.getConverterName())) {
            // Using the configuration based approach.
            value = conversionHelper.convert(recordElement.getConverterName(), fieldChars, recordElement.getConversionOptions(),
                    beanRef);
        } else {
            // Use the reflection approach.
            value = conversionHelper.convert(bean, beanRef, property, fieldChars, recordElement.getConversionOptions());
        }

        mappingStrategy.mapBean(bean, beanRef, property, value, recordElement.getConversionOptions());
    }

    /**
     * For non-delimited lines that are under a {@link SegmentElementBO} instance, capture them into the parent bean.
     *
     * @param segmentElement The {@link SegmentElementBO} instance being processed.
     * @throws FlatwormParserException should invoking the necessary reflection methods fail for any reason.
     */
    private void captureSegmentBean(SegmentElementBO segmentElement) throws FlatwormParserException {
        String beanRef = segmentElement.getCardinality().getBeanRef();
        String parentBeanRef = segmentElement.getCardinality().getParentBeanRef();
        String property = segmentElement.getCardinality().getPropertyName();

        Object parent = beans.get(parentBeanRef);
        Object toAdd = beans.get(beanRef);

        if (segmentElement.getCardinality().getCardinalityMode() == CardinalityMode.SINGLE) {
            ParseUtils.setProperty(parent, property, toAdd);
        } else {
            ParseUtils.addValueToCollection(segmentElement.getCardinality(), parent, toAdd);
        }
    }

    /**
     * Convert string field from file into appropriate converterName and set bean's value. This is used for delimited files only<br>
     *
     * @param inputLine the line of data read from the data file
     * @param identity  The {@link Identity} instance used to determine that this {@link LineBO} instance should parse this line.
     * @throws FlatwormParserException should any issues occur while parsing the data.
     */
    private void parseInputDelimited(String inputLine, Identity identity) throws FlatwormParserException {

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
        cleanupLineTokens(identity);
        currentField = 0;
        doParseDelimitedInput(elements);
    }

    /**
     * Remove any record-level, {@link LineTokenIdentity} instance tokens from the list of line tokens so that they don't affect the
     * processing of the data elements. Additional, for any place where there are segment-records, the {@link LineToken} instances need to
     * have the positional information updated to reflect that they are virtually at the head of the segment-record even though they aren't
     * at the head of the line.
     *
     * @param identity The {@link Identity} instance used to determine that this {@link LineBO} instance should parse this line.
     */
    private void cleanupLineTokens(Identity identity) {
        Iterator<LineToken> lineTokenIterator = lineTokens.iterator();
        if (identity instanceof LineTokenIdentity) {
            LineTokenIdentity lineTokenIdentity = LineTokenIdentity.class.cast(identity);
            while (lineTokenIterator.hasNext()) {
                if (lineTokenIdentity.matchesIdentity(lineTokenIterator.next())) {
                    lineTokenIterator.remove();
                }
            }
        }
    }

    /**
     * Walk the configured {@link LineElement}s and parse out the data based upon the configuration.
     *
     * @param elements the configured {@link LineElement} instances that control how data will be parsed.
     * @throws FlatwormParserException should the data not match the configuration.
     */
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

    /**
     * Map a read token of data into the correct object based upon the configuration of the {@link RecordElementBO} instance.
     *
     * @param recordElement The {@link RecordElementBO} instance that contains the configuration information driving how the data will be
     *                      added to the correct bean.
     * @param fieldStr      The token of data read.
     * @throws FlatwormParserException should the data not match the configuration.
     */
    private void parseDelimitedRecordElement(RecordElementBO recordElement, String fieldStr) throws FlatwormParserException {
        if (!recordElement.getIgnoreField()) {
            // JBL - to keep from dup. code, moved this to a private method
            mapField(fieldStr, recordElement);
        }
    }

    /**
     * Parse the data from the line for the given {@link SegmentElementBO} instance.
     *
     * @param segment The {@link SegmentElementBO} instance containing the configuration on how the data is to be parsed.
     * @throws FlatwormParserException should the data not match the configuration.
     */
    private void parseDelimitedSegmentElement(SegmentElementBO segment) throws FlatwormParserException {
        int minCount = segment.getCardinality().getMinCount();
        int maxCount = segment.getCardinality().getMaxCount();
        if (maxCount <= 0) {
            maxCount = Integer.MAX_VALUE;
        }
        if (minCount < 0) {
            minCount = 0;
        }

        String beanRef = segment.getCardinality().getBeanRef();
        if (currentField < lineTokens.size() && !segment.matchesIdentity(lineTokens.get(currentField)) && minCount > 0) {
            log.error("Segment " + segment.getCardinality().getPropertyName() + " with minimum required count of "
                    + minCount + " missing.");
        }
        int cardinality = 0;
        try {
            while (currentField < lineTokens.size() && segment.matchesIdentity(lineTokens.get(currentField))) {
                currentField++; // Advanced past the identifier token.
                if (beanRef != null) {
                    ++cardinality;
                    String parentRef = segment.getCardinality().getParentBeanRef();
                    if (parentRef != null) {
                        Object instance = ParseUtils.newBeanInstance(beans.get(beanRef));
                        beans.put(beanRef, instance);
                        ParseUtils.addObjectToProperty(beans.get(parentRef), instance, segment.getCardinality());
                    }
                    doParseDelimitedInput(segment.getLineElements());
                }
            }
        } finally {
            if (cardinality > maxCount) {
                log.error("Segment '" + segment.getCardinality().getPropertyName() + "' with maximum of " + maxCount
                        + " encountered actual count of " + cardinality);
            }
        }
    }

    /**
     * Determine if the configuration of this {@code LineBO} instance for a {@link RecordBO} line (returns {@code false}) or if it was
     * configured against a Java Bean's property (returns {@code true}).
     *
     * @return {@code true} if this {@code LineBO} instance has the {@code parentBeanRef}, {@code beanRef}, and {@code PropertyName}
     * properties defined with valid values and {@code false} if not.
     */
    public boolean isPropertyLine() {
        return cardinality != null
                && !StringUtils.isBlank(cardinality.getParentBeanRef())
                && !StringUtils.isBlank(cardinality.getBeanRef())
                && !StringUtils.isBlank(cardinality.getPropertyName());
    }

    @Override
    public String toString() {
        return "LineBO{" +
                "index='" + index + '\'' +
                ", lineIdentity=" + lineIdentity +
                ", delimiter='" + delimiter + '\'' +
                ", quoteChar=" + quoteChar +
                ", cardinality='" + cardinality + '\'' +
                '}';
    }
}