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

import com.blackbear.flatworm.CardinalityMode;
import com.blackbear.flatworm.FileFormat;
import com.blackbear.flatworm.Util;
import com.blackbear.flatworm.annotations.ConversionOption;
import com.blackbear.flatworm.annotations.Converter;
import com.blackbear.flatworm.annotations.FieldIdentity;
import com.blackbear.flatworm.annotations.LengthIdentity;
import com.blackbear.flatworm.annotations.Line;
import com.blackbear.flatworm.annotations.Record;
import com.blackbear.flatworm.annotations.RecordElement;
import com.blackbear.flatworm.annotations.RecordLink;
import com.blackbear.flatworm.annotations.ScriptIdentity;
import com.blackbear.flatworm.annotations.SegmentElement;
import com.blackbear.flatworm.config.AnnotationConfigurationReader;
import com.blackbear.flatworm.config.BeanBO;
import com.blackbear.flatworm.config.ConfigurationValidator;
import com.blackbear.flatworm.config.ConversionOptionBO;
import com.blackbear.flatworm.config.ConverterBO;
import com.blackbear.flatworm.config.Identity;
import com.blackbear.flatworm.config.LineBO;
import com.blackbear.flatworm.config.LineElementCollection;
import com.blackbear.flatworm.config.RecordBO;
import com.blackbear.flatworm.config.RecordDefinitionBO;
import com.blackbear.flatworm.config.RecordElementBO;
import com.blackbear.flatworm.config.SegmentElementBO;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the {@link AnnotationConfigurationReader} interface. Parses all annotated classes and then constructs the
 * {@link FileFormat} instance from those annotations. This class is not thread safe - a new instance should be used for each parsing
 * activity that needs to be performed.
 *
 * @author Alan Henson
 */
@Slf4j
public class DefaultAnnotationConfigurationReaderImpl implements AnnotationConfigurationReader {

    private Map<String, RecordBO> recordCache;
    private Map<String, LineBO> lineCache;
    private Deque<LineElementCollection> lineElementDeque;

    @Setter
    @Getter
    private boolean performValidation;
    
    private boolean onFirstPassFlag;
    
    @Getter
    private FileFormat fileFormat;

    public DefaultAnnotationConfigurationReaderImpl() {
        fileFormat = new FileFormat();
        recordCache = new HashMap<>();
        lineCache = new HashMap<>();
        lineElementDeque = new ArrayDeque<>();
        performValidation = true;
        onFirstPassFlag = false;
    }

    @Override
    public FileFormat loadConfiguration(Class<?>... classes) throws FlatwormConfigurationException {
        if (classes != null) {
            fileFormat = loadConfiguration(Arrays.asList(classes));
        }
        return fileFormat;
    }

    @Override
    public FileFormat loadConfiguration(Collection<Class<?>> classes) throws FlatwormConfigurationException {
        boolean performCleanup = false;
        if (!onFirstPassFlag) {
            // The loadConfiguration method is recursively called, we only need to perform cleanup after the very first call
            // completes - not every time it is invoked.
            performCleanup = true;
            onFirstPassFlag = true;
        }

        // Load the configuration.
        fileFormat = loadConfiguration(Collections.emptyList(), classes);

        // Sort all LineElementCollection instances.
        if (performCleanup && performValidation) {
            fileFormat.getRecords().stream()
                    .filter(record -> record.getRecordDefinition() != null)
                    .forEach(record ->
                            record.getRecordDefinition().getLines()
                                .forEach(this::sortLineElementCollections));

            // Validate that all required metadata has been captured.
            ConfigurationValidator.validateFileFormat(fileFormat);
        }

        return fileFormat;
    }

    /**
     * Load the configuration for the specified Classes, but keep track of what is being requested for retry to avoid an infinite loop.
     *
     * @param lastRetryList The last set of classes that were marked for retry.
     * @param classes       The classes to process.
     * @return This is a convenience method to make it easy to assign the {@link FileFormat} constructed to the invoker of the {@code
     * loadConfiguration} call.
     * @throws FlatwormConfigurationException should any issues arise while parsing the annotated configuration.
     */
    protected FileFormat loadConfiguration(List<Class<?>> lastRetryList, Collection<Class<?>> classes)
            throws FlatwormConfigurationException {
        List<Class<?>> classesToReprocess = new ArrayList<>();
        FlatwormConfigurationException lastException = null;

        for (Class<?> clazz : classes) {
            try {
                RecordBO recordBO = null;

                // Record Annotations.
                if (clazz.isAnnotationPresent(Record.class)) {
                    recordBO = loadRecord(clazz);
                    recordCache.put(clazz.getName(), recordBO);
                    fileFormat.addRecord(recordBO);
                } else if (clazz.isAnnotationPresent(RecordLink.class)) {
                    // See if we have loaded the RecordBO yet - if not, we'll need to try again later.
                    Class<?> recordClass = loadRecordLinkClass(clazz);
                    if (recordCache.containsKey(recordClass.getName())) {
                        recordBO = recordCache.get(recordClass.getName());
                    } else {
                        classesToReprocess.add(clazz);
                    }
                }

                // We must always have a reference to the record - if we don't have it then we'll need to
                // reprocess the item later.
                if (recordBO != null) {
                    // See what field-level annotations might exist.
                    processFieldAnnotations(recordBO, clazz);
                }
            } catch (FlatwormConfigurationException e) {
                classesToReprocess.add(clazz);
                lastException = e;
            }
        }

        // See if we need to reprocess or kick-out.
        if (!classesToReprocess.isEmpty()) {
            if (isValidRetryList(lastRetryList, classesToReprocess)) {
                fileFormat = loadConfiguration(classesToReprocess, classesToReprocess);
            } else {
                String errMessage = "Unable to complete loading configuration as the following classes " +
                        "provided are not resolvable for a number of reasons. Ensure that all Records and RecordLinks are properly" +
                        " defined. All classes involved must either have a Record annotation or a RecordLink annotation." +
                        String.format("%n") + classesToReprocess.toString();
                if (lastException != null) {
                    throw new FlatwormConfigurationException(errMessage, lastException);
                } else {
                    throw new FlatwormConfigurationException(errMessage);
                }
            }
        }

        return fileFormat;
    }

    /**
     * Make sure we have a valid retry list before dropping into an infinite loop.
     *
     * @param lastRetryList      The last retry list.
     * @param classesToReprocess The retry list constructed this time around.
     * @return {@code true} if the list is valid for retrying and {@code false} if not.
     */
    private boolean isValidRetryList(List<Class<?>> lastRetryList, List<Class<?>> classesToReprocess) {
        boolean isValid = false;
        if (!classesToReprocess.isEmpty()) {
            if (lastRetryList.size() == classesToReprocess.size()) {
                for (Class<?> clazz : classesToReprocess) {
                    if (!lastRetryList.contains(clazz)) {
                        isValid = true;
                        break;
                    }
                }
            } else {
                isValid = true;
            }
        }
        return isValid;
    }

    /**
     * Get the linked {@link Record} class from the given {@link RecordLink} annotation instance within the {@code clazz} method.
     *
     * @param clazz The class from which to load the {@link RecordLink} annotation instance and fetch its linked class.
     * @return The Class instance found in the class parameter of the {@link RecordLink} instance or {@code null} if it wasn't found.
     */
    public Class<?> loadRecordLinkClass(Class<?> clazz) {
        Class<?> link = null;

        RecordLink recordLink = clazz.getAnnotation(RecordLink.class);
        if (recordLink != null) {
            link = recordLink.recordClass();
        }

        return link;
    }

    /**
     * For the given {@link Class}, load the {@link RecordBO} information if present.
     *
     * @param clazz The {@link Class} to check for the {@code @RecordBO} annotation.
     * @return A {@link RecordBO} instance if the annotation is found and {@code null} if not.
     * @throws FlatwormConfigurationException should any issues occur with parsing the configuration elements within the annotations.
     */
    public RecordBO loadRecord(Class<?> clazz) throws FlatwormConfigurationException {
        RecordBO record = null;
        if (clazz != null) {
            Record annotatedRecord = clazz.getAnnotation(Record.class);

            if (annotatedRecord != null) {
                record = new RecordBO();
                record.setRecordDefinition(new RecordDefinitionBO(record));

                // Capture the identifying information.
                record.setName(annotatedRecord.name());

                // Load the data that is provided.
                record.setRecordIdentity(loadRecordIdentity(clazz));

                loadConverters(annotatedRecord);

                // Load the lines.
                loadLines(record, annotatedRecord);
            }
        }
        return record;
    }

    /**
     * Load any {@link ConverterBO} instances found from the configuration provided by the {@link Converter} elements within the {@code
     * annotatedRecord} {@link Record} instance. The {@link FileFormat} instance being built up will be updated with the loaded converters.
     *
     * @param annotatedRecord The {@link Record} instance.
     * @return A {@link List} of {@link ConverterBO} instances created from whatever {@link Converter} instances have been configured within
     * the {@link Record} annotation.
     */
    public List<ConverterBO> loadConverters(Record annotatedRecord) {
        ConverterBO converter;
        List<ConverterBO> converters = new ArrayList<>();
        for (Converter annotatedConverter : annotatedRecord.converters()) {
            converter = loadConverter(annotatedConverter);
            converters.add(converter);
            fileFormat.addConverter(converter);
        }

        return converters;
    }

    /**
     * Create a {@link ConverterBO} instance from the {@link Converter} annotation.
     *
     * @param annotatedConverter The {@link Converter} annotation instance.
     * @return A contsructed {@link ConverterBO} instance from the {@link Converter} instance.
     */
    public ConverterBO loadConverter(Converter annotatedConverter) {
        return new ConverterBO(
                annotatedConverter.clazz().getName(),
                annotatedConverter.name(),
                annotatedConverter.returnType().getName(),
                annotatedConverter.methodName());
    }

    /**
     * Load the discovered the {@link Line} annotation values into the {@link RecordBO} instance (via the {@link RecordDefinitionBO}
     * property.
     *
     * @param record          The {@link RecordBO} instance being built up.
     * @param annotatedRecord The loaded {@link Record} annotation.
     * @return a {@link List} of {@link Line} instances loaded.
     */
    public List<LineBO> loadLines(RecordBO record, Record annotatedRecord) {
        List<LineBO> lines = new ArrayList<>();
        LineBO line;
        for (Line annotatedLine : annotatedRecord.lines()) {
            line = new LineBO();
            line.setDelimiter(annotatedLine.delimiter());
            line.setQuoteChar(annotatedLine.quoteCharacter());
            line.setId(annotatedLine.id());
            record.getRecordDefinition().addLine(line);
            lineCache.put(annotatedLine.id(), line);
            lines.add(line);
        }
        return lines;
    }

    /**
     * Determine which {@link com.blackbear.flatworm.config.Identity} information is present in the {@link Record} annotation and create the
     * corresponding {@link com.blackbear.flatworm.config.Identity} implementation.
     *
     * @param clazz The clazz to check for the {@link Record} annotation.
     * @return the {@link Identity} instance loaded.
     * @throws FlatwormConfigurationException should parsing the annotation's values fail for any reason.
     */
    public Identity loadRecordIdentity(Class<?> clazz) throws FlatwormConfigurationException {
        Record annotatedRecord = clazz.getAnnotation(Record.class);
        Identity identity = null;

        fileFormat.setEncoding(annotatedRecord.encoding());

        // Load the identities.
        if (annotatedRecord.lengthIdentity().apply()) {
            identity = loadLengthIdentity(annotatedRecord.lengthIdentity());
        } else if (annotatedRecord.fieldIdentity().apply()) {
            identity = loadFieldIdentity(annotatedRecord.fieldIdentity());
        } else if (annotatedRecord.scriptIdentity().apply()) {
            identity = loadScriptIdentity(annotatedRecord.scriptIdentity());
        }

        return identity;
    }

    /**
     * Load the {@link LengthIdentity} annotation configuration into a {@link LengthIdentityImpl} instance and return it.
     *
     * @param annotatedIdentity The {@link LengthIdentity} annotation instance.
     * @return the {@link LengthIdentityImpl} instance constructed.
     */
    public LengthIdentityImpl loadLengthIdentity(LengthIdentity annotatedIdentity) {
        LengthIdentityImpl lengthIdentity = new LengthIdentityImpl();
        lengthIdentity.setMinLength(annotatedIdentity.minLength());
        lengthIdentity.setMaxLength(annotatedIdentity.maxLength());
        return lengthIdentity;
    }

    /**
     * Load the {@link FieldIdentity} annotation configuration into a {@link FieldIdentityImpl} instance and return it.
     *
     * @param annotatedIdentity The {@link FieldIdentity} annotation instance.
     * @return the {@link FieldIdentityImpl} instance constructed.
     */
    public FieldIdentityImpl loadFieldIdentity(FieldIdentity annotatedIdentity) {
        FieldIdentityImpl fieldIdentity = new FieldIdentityImpl(annotatedIdentity.ignoreCase());
        fieldIdentity.setStartPosition(annotatedIdentity.startPosition());
        fieldIdentity.setFieldLength(annotatedIdentity.fieldLength());

        for (String matchIdentity : annotatedIdentity.matchIdentities()) {
            fieldIdentity.addMatchingString(matchIdentity);
        }
        return fieldIdentity;
    }

    /**
     * Load the {@link ScriptIdentity} annotation configuration into a {@link ScriptIdentityImpl} instance and return it.
     *
     * @param annotatedIdentity The {@link ScriptIdentity} annotation instance.
     * @return the {@link ScriptIdentityImpl} instance constructed.
     */
    public ScriptIdentityImpl loadScriptIdentity(ScriptIdentity annotatedIdentity) throws FlatwormConfigurationException {
        return new ScriptIdentityImpl(
                annotatedIdentity.scriptEngine(),
                annotatedIdentity.script(),
                annotatedIdentity.scriptMethod()
        );
    }

    /**
     * Look through the given {@code clazz}'s {@link Field}s and see if there are any that have {@code annotations} that are supported by
     * flatworm and if so, process them.
     *
     * @param record The {@link RecordBO} instance that is being built up - all processed data will get loaded to this {@link RecordBO}
     *               instance.
     * @param clazz  The {@link Class} to be interrogated.
     * @throws FlatwormConfigurationException should any of the configuration data be invalid.
     */
    public void processFieldAnnotations(RecordBO record, Class<?> clazz) throws FlatwormConfigurationException {
        for (Field field : clazz.getDeclaredFields()) {
            // Check for RecordElement.
            if (field.isAnnotationPresent(RecordElement.class)) {
                loadRecordElement(record, clazz, field);
            } else if (field.isAnnotationPresent(SegmentElement.class)) {
                loadSegment(clazz, field);
            }
        }
    }

    /**
     * Load all of the {@link RecordElement} annotation data to a {@link RecordElementBO} instance and return it. If the current {@link
     * LineElementCollection} instance can be determined then the {@link RecordElementBO} instance constructed will be added to it.
     *
     * @param record The {@link RecordBO} instance being built up.
     * @param clazz  The class that owns with the {@link Field} with the {@link RecordElement} annotation.
     * @param field  The {@link Field} that has the {@link RecordElement} annotation.
     * @return the built up {@link RecordElementBO} instance.
     * @throws FlatwormConfigurationException should any of the configuration data be invalid.
     */
    public RecordElementBO loadRecordElement(RecordBO record, Class<?> clazz, Field field) throws FlatwormConfigurationException {
        RecordElementBO recordElement = new RecordElementBO();
        RecordElement annotatedElement = field.getAnnotation(RecordElement.class);

        LineBO line = lineCache.get(annotatedElement.lineId());
        LineElementCollection elementCollection = lineElementDeque.isEmpty() ? line : lineElementDeque.getLast();

        try {
            // See if the bean has been registered.
            if (!record.getRecordDefinition().getBeanMap().containsKey(clazz.getName())) {
                BeanBO bean = new BeanBO();
                bean.setBeanName(clazz.getName());
                bean.setBeanClass(clazz.getName());
                bean.setBeanObjectClass(clazz);
                record.getRecordDefinition().addBean(bean);
            }

            recordElement.setBeanRef(clazz.getName() + "." + field.getName());
            recordElement.setConverterName(annotatedElement.converterName());
            recordElement.setOrder(annotatedElement.order());

            if (annotatedElement.length() != -1) {
                recordElement.setFieldLength(annotatedElement.length());
            }

            if (annotatedElement.startPosition() != -1) {
                recordElement.setFieldStart(annotatedElement.startPosition());
            }
            if (annotatedElement.endPosition() != -1) {
                recordElement.setFieldEnd(annotatedElement.endPosition());
            }

            if (annotatedElement.conversionOptions().length > 0) {
                for (ConversionOption annotatedOption : annotatedElement.conversionOptions()) {
                    loadConversionOption(recordElement, annotatedOption);
                }
            }

            if (elementCollection != null) {
                elementCollection.addLineElement(recordElement);
            }
        } catch (Exception e) {
            throw new FlatwormConfigurationException(String.format(
                    "For %s::%s, line with ID %s was specified, but could not be found.",
                    clazz.getName(), field.getName(), annotatedElement.lineId()));
        }

        return recordElement;
    }

    /**
     * Load the {@link SegmentElement} metadata and associated {@link RecordElement} data (and so on) for the given {@code Field} within the
     * given {@code clazz}. Due to the tree-like structure of {@link SegmentElementBO} instances, this could result in several recursive
     * calls as the bean tree is traversed. This will add the {@link SegmentElementBO} instance to the {@link LineBO} referenced within the
     * {@link SegmentElement} annotation.
     *
     * @param clazz The class instance that owns the {@code field}.
     * @param field The {@code field} that has the {@link SegmentElement} annotation.
     * @return a constructed {@link SegmentElementBO} instance from the data found within the {@link SegmentElement} annotation.
     * @throws FlatwormConfigurationException should the annotated metadata prove invalid.
     */
    public SegmentElementBO loadSegment(Class<?> clazz, Field field) throws FlatwormConfigurationException {
        SegmentElementBO segmentElementBO = new SegmentElementBO();
        SegmentElement annotatedElement = field.getAnnotation(SegmentElement.class);

        LineBO line = lineCache.get(annotatedElement.lineId());
        LineElementCollection elementCollection = lineElementDeque.isEmpty() ? line : lineElementDeque.getLast();
        elementCollection.addLineElement(segmentElementBO);

        Class<?> fieldType = Util.getActualFieldType(field);

        // Need to see if this is a collection or a single attributes.
        if (fieldType != null) {
            if (fieldType.isAnnotationPresent(RecordLink.class) || fieldType.isAnnotationPresent(Record.class)) {
                lineElementDeque.add(segmentElementBO);
                FieldIdentityImpl fieldIdentity = loadFieldIdentity(annotatedElement.fieldIdentity());
                segmentElementBO.setOrder(annotatedElement.order());
                segmentElementBO.setFieldIdentity(fieldIdentity);

                segmentElementBO.setParentBeanRef(clazz.getName());
                segmentElementBO.setPropertyName(field.getName());
                segmentElementBO.setBeanRef(fieldType.getName());

                if(Collection.class.isAssignableFrom(field.getType()) || field.getType().isArray()) {
                    segmentElementBO.setCardinalityMode(annotatedElement.cardinalityMode());
                    segmentElementBO.setMinCount(annotatedElement.mintCount());
                    segmentElementBO.setMaxCount(annotatedElement.maxCount());
                }
                else {
                    // This is a singular instance.
                    segmentElementBO.setCardinalityMode(CardinalityMode.SINGLE);
                }

                loadConfiguration(fieldType);
                lineElementDeque.removeLast();
            } else {
                throw new FlatwormConfigurationException(String.format(
                        "Class %s has a %s defined with type %s, which must have a %s or %s annotation.",
                        clazz.getName(), SegmentElement.class.getName(), fieldType.getName(),
                        RecordLink.class.getName(), Record.class.getName()));
            }
        }

        return segmentElementBO;
    }

    /**
     * Load a {@link ConversionOption} annotation instance into the {@code recordElement} instance.
     *
     * @param recordElement   The {@link RecordElementBO} instance to load the {@code ConversionOption} into.
     * @param annotatedOption The {@link ConversionOption} annotation instance.
     * @return The built up {@link ConversionOptionBO} for convenience - it will already be loaded to the {@code recordElement} instance.
     */
    public ConversionOptionBO loadConversionOption(RecordElementBO recordElement, ConversionOption annotatedOption) {
        ConversionOptionBO option = new ConversionOptionBO(annotatedOption.name(), annotatedOption.option());
        recordElement.addConversionOption(annotatedOption.name(), option);
        return option;
    }

    /**
     * Ensure all {@link LineElementCollection} instances in the tree have been properly sorted - this method is recursively called
     * to navigate the full tree.
     * @param lineElementCollection The {@link LineElementCollection} to sort.
     */
    protected void sortLineElementCollections(LineElementCollection lineElementCollection) {
        lineElementCollection.sortLineElements();
        lineElementCollection.getLineElements().stream()
                .filter(lineElement -> lineElement instanceof LineElementCollection)
                .map(LineElementCollection.class::cast)
                .forEach(this::sortLineElementCollections);
    }
}
