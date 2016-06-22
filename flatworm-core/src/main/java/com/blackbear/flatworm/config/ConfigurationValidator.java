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
import com.blackbear.flatworm.config.impl.FieldIdentityImpl;
import com.blackbear.flatworm.config.impl.LengthIdentityImpl;
import com.blackbear.flatworm.config.impl.ScriptIdentityImpl;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates most of the validation logic that is run against a flatworm's configuration for parsing a data file.
 *
 * @author Alan Henson
 */
public class ConfigurationValidator {

    /**
     * Valdate the configuration captured in the {@link FileFormat} instance.
     *
     * @param fileFormat The {@link FileFormat} instance to check.
     * @return An empty {@link List} if no errors or found else all errors found will be in the return with each specific error being its
     * own entry in the {@link List}.
     */
    public static List<String> validateFileFormat(FileFormat fileFormat) {
        List<String> errors = new ArrayList<>();

        fileFormat.getConversionHelper().getConverters()
                .forEach(converter -> validateConverter(converter, errors));

        fileFormat.getRecords().forEach(record -> validateRecord(record, errors));

        return errors;
    }

    /**
     * Validate that the {@code record} tag and its children were properly populated.
     *
     * @param record The {@link RecordBO} instance containing the values to validate.
     * @param errors A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateRecord(RecordBO record, List<String> errors) {
        if (StringUtils.isBlank(record.getName())) {
            errors.add("Must specify a name for the record.");
        }

        validateIdentity(record.getRecordIdentity(), errors);
        validateRecordDefinition(record.getRecordDefinition(), errors);
    }

    /**
     * Validate that the {@code converter} tag was properly populated.
     *
     * @param converter The {@link ConverterBO} instance containing the values to validate.
     * @param errors    A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateConverter(ConverterBO converter, List<String> errors) {
        Class<?> clazz = null;
        if (StringUtils.isBlank(converter.getName())) {
            errors.add("Must specify the name of the ConverterBO." +
                    "This is used to identify which converter to use in the record-element configuration.");
        }

        if (StringUtils.isBlank(converter.getConverterClass())) {
            errors.add("Must specify the class of the ConverterBO." +
                    "This is used to identify which converter to use in the record-element configuration.");
        } else {
            try {
                clazz = Class.forName(converter.getConverterClass());
            } catch (Exception e) {
                errors.add(String.format("Failed to locate ConverterBO class: %s for ConverterBO:%s." +
                                "Please double check the fully qualified name for accuracy",
                        converter.getName(), converter.getConverterClass()));
            }
        }

        if (StringUtils.isBlank(converter.getReturnType())) {
            errors.add(String.format("Must specify the return-type for ConverterBO %s.", converter.getName()));
        } else {
            try {
                Class.forName(converter.getReturnType());
            } catch (Exception e) {
                errors.add(String.format("Failed to find return-type %s for ConverterBO %s.",
                        converter.getReturnType(), converter.getName()));
            }
        }

        if (StringUtils.isBlank(converter.getMethod())) {
            errors.add(String.format("Must specify the method for ConverterBO %s.", converter.getName()));
        } else if (clazz != null) {
            try {
                clazz.getMethod(converter.getMethod(), Object.class, Map.class);
            } catch (Exception e) {
                errors.add(String.format("Failed to find method %s on ConverterBO %s.",
                        converter.getMethod(), converter.getConverterClass()));
            }
        }
    }

    /**
     * If a known {@link Identity} has been used, attempt to validate it.
     *
     * @param identity The {@link Identity} instance containing the values to validate.
     * @param errors   A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateIdentity(Identity identity, List<String> errors) {
        if (identity instanceof LengthIdentityImpl) {
            validateLengthIdentity(LengthIdentityImpl.class.cast(identity), errors);
        } else if (identity instanceof FieldIdentityImpl) {
            validateFieldIdentity(FieldIdentityImpl.class.cast(identity), errors);

        } else if (identity instanceof ScriptIdentityImpl) {
            validateScriptIdentity(ScriptIdentityImpl.class.cast(identity), errors);
        }
    }

    /**
     * Validate that the {@code length-ident} tag was properly populated.
     *
     * @param lengthIdentity The {@link LengthIdentityImpl} instance containing the values to validate.
     * @param errors         A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateLengthIdentity(LengthIdentityImpl lengthIdentity, List<String> errors) {
        if (lengthIdentity.getMinLength() == null) {
            errors.add("Must specify the min-length attribute when using length-ident. " +
                    "This specifies that a line of data must be of a minimum length for it to be parsed by this RecordBO.");
        }
        if (lengthIdentity.getMaxLength() == null) {
            errors.add("Must specify the max-length attribute when using length-ident. " +
                    "This specifies that a line of data must not be more than a given length for it to be parsed by this RecordBO.");
        }
    }

    /**
     * Validate that the {@code field-ident} tag was properly populated.
     *
     * @param fieldIdentity The {@link FieldIdentityImpl} instance containing the values to validate.
     * @param errors        A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateFieldIdentity(FieldIdentityImpl fieldIdentity, List<String> errors) {
        if (fieldIdentity.getStartPosition() == null) {
            errors.add("Must specify the field-start attribute when using field-ident. " +
                    "This indicates where the field identify value starts for the record.");
        }
        if (fieldIdentity.getFieldLength() == null) {
            errors.add("Must specify the field-length attribute when using field-ident. " +
                    "This indicates the length of field identity for the record.");
        }
        if (fieldIdentity.getMatchingStrings().isEmpty()) {
            errors.add("Must specify at least one match-string element when using field-ident. " +
                    "These are used to determine if a line of data should be parsed by this RecordBO.");
        }
    }

    /**
     * Validate that the {@code script-ident} tag was properly populated.
     *
     * @param scriptIdentity The {@link ScriptIdentityImpl} instance containing the values to validate.
     * @param errors         A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateScriptIdentity(ScriptIdentityImpl scriptIdentity, List<String> errors) {
        validateScriptlet(scriptIdentity.getScriptlet(), errors);
    }

    /**
     * Validate that the {@code Scriptlet} information was properly configured.
     *
     * @param scriptlet The {@link ScriptletBO} instance containing the values to validate.
     * @param errors    A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateScriptlet(ScriptletBO scriptlet, List<String> errors) {
        if (StringUtils.isBlank(scriptlet.getScript()) && StringUtils.isBlank(scriptlet.getScriptFile())) {
            errors.add("The Script Identity configuration must include the script or script-file parameter.");
        }
    }

    /**
     * Validate that the {@code record-definition} tag was properly populated.
     *
     * @param recordDefinition The {@link RecordDefinitionBO} instance containing the values to validate.
     * @param errors           A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateRecordDefinition(RecordDefinitionBO recordDefinition, List<String> errors) {
        if (recordDefinition.getBeans().isEmpty()) {
            errors.add("Must specify at least 1 bean element for a record-definition. " +
                    "This indicates what Java object will be populated with the values parsed.");
        } else {
            recordDefinition.getBeans().forEach(bean -> validateBean(bean, errors));
        }

        if (recordDefinition.getLines().isEmpty() && recordDefinition.getLinesWithIdentities().isEmpty()) {
            errors.add("Must specify at least 1 line element for a record-definition. " +
                    "This indicates how the line of data is to be parsed.");
        } else {
            recordDefinition.getLines().forEach(line -> validateLine(line, errors));
        }
    }

    /**
     * Validate that the {@code bean} tag was properly populated.
     *
     * @param bean   The {@link BeanBO} instance containing the values to validate.
     * @param errors A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateBean(BeanBO bean, List<String> errors) {
        if (StringUtils.isBlank(bean.getBeanName())) {
            errors.add("Must specify the name for a bean element. " +
                    "This is how the bean will be referenced within the record-element configuration.");
        }

        if (StringUtils.isBlank(bean.getBeanClass())) {
            errors.add("Must specify the class for a bean element. " +
                    "This indicates the fully qualified name of the Java class that will be instantiated and populated when data " +
                    "is parsed.");
        }
    }

    /**
     * Validate that the {@code line} tag was properly populated.
     *
     * @param line   The {@link LineBO} instance containing the values to validate.
     * @param errors A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateLine(LineBO line, List<String> errors) {
        
        if(line.getLineIdentity() != null) {
            validateIdentity(line.getLineIdentity(), errors);
            
            if(line.getIndex() != -1) {
                errors.add("Lines with Identities defined should not be given an index - use -1 to indicate no index specified.");
            }
        }
        else {
            if(line.getRecordEndLine() != null && line.getRecordEndLine()) {
                errors.add("Only Lines with an Identity defined should have the Record End Line set to true." +
                        "These types of Lines are configured on the Field of a bean vs. in the Record header.");
            }
        }
        
        line.getLineElements()
                .stream()
                .filter(lineElement -> lineElement instanceof RecordElementBO)
                .map(RecordElementBO.class::cast)
                .forEach(recordElement -> validateRecordElement(line, recordElement, errors));

        line.getLineElements()
                .stream()
                .filter(lineElement -> lineElement instanceof SegmentElementBO)
                .map(SegmentElementBO.class::cast)
                .forEach(segmentElement -> validateSegmentElement(segmentElement, errors));
    }

    /**
     * Validate that the {@code segment-element} configuration contains all of the required components.
     *
     * @param segment The segment instance to validate.
     * @param errors  A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateSegmentElement(SegmentElementBO segment, List<String> errors) {
        if (segment.getFieldIdentity() != null) {
            validateFieldIdentity(segment.getFieldIdentity(), errors);
        } else {
            errors.add("Must specify the Field Identity configuration to use so that the lines can be correctly subdivided " +
                    "during parsing.");
        }
        
        validateCardinality(segment.getCardinality(), errors);
    }

    /**
     * Validate that a {@link CardinalityBO} instance has been correctly parsed.
     * @param cardinality The {@link CardinalityBO} instance to validate.
     * @param errors  A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateCardinality(CardinalityBO cardinality, List<String> errors) {
        if (cardinality == null) {
            errors.add("Must specify a valid Cardinality Mode or leave the attribute out of the configuration to default to LOOSE.");
        }
        else {
            if (cardinality.getCardinalityMode() == null) {
                errors.add("Must specify a valid Cardinality Mode or leave the attribute out of the configuration to default to LOOSE.");
            }
            if (StringUtils.isBlank(cardinality.getBeanRef())) {
                errors.add("Must specify the beanref attribute for segment and record elements.");
            }
            if (StringUtils.isBlank(cardinality.getPropertyName()) && StringUtils.isBlank(cardinality.getAddMethod())) {
                errors.add("Must specify either the property-name attribute or add-method attribute for segment-elements.");
            }
        }
    }

    /**
     * Validate that the {@code record-element} configuration contains all of the required components.
     *
     * @param parentLine    The parent {@link LineBO} instance - used for checking to see if a delimiter is present.
     * @param recordElement The {@link RecordElementBO} instance to validate.
     * @param errors        A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateRecordElement(LineBO parentLine, RecordElementBO recordElement, List<String> errors) {
        if (recordElement.getIgnoreField() == null || !recordElement.getIgnoreField()) {
            if (recordElement.getCardinality() == null || StringUtils.isBlank(recordElement.getCardinality().getBeanRef())) {
                errors.add("Must specify a beanref attribute for a record-element.");
            }

            if (StringUtils.isBlank(parentLine.getDelimiter())) {
                if (recordElement.getFieldEnd() == null && recordElement.getFieldLength() == null) {
                    errors.add("Must set either the 'end' or 'length' properties for a record-element.");
                }
                if (recordElement.getFieldEnd() != null && recordElement.getFieldLength() != null) {
                    errors.add("Can't specify both the 'end' or 'length' properties for a record-element.");
                }
            }

            for (ConversionOptionBO option : recordElement.getConversionOptions().values()) {
                validateConversionOption(option, errors);
            }
        }
    }

    /**
     * Validate that the {@code conversion-option} configuration contains all of the required components.
     *
     * @param conversionOption The {@link ConversionOptionBO} instance to validate.
     * @param errors           A non-null {@link List} that will be appended to if errors are found.
     */
    public static void validateConversionOption(ConversionOptionBO conversionOption, List<String> errors) {
        if (StringUtils.isBlank(conversionOption.getName())) {
            errors.add("Must specify the name attribute for a conversion-option element.");
        }
        if (StringUtils.isBlank(conversionOption.getValue())) {
            errors.add("Must specify the value attribute for a conversion-option element.");
        }
    }
}
