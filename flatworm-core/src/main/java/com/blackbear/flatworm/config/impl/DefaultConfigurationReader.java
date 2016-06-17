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

import com.google.common.base.Joiner;

import com.blackbear.flatworm.CardinalityMode;
import com.blackbear.flatworm.FileFormat;
import com.blackbear.flatworm.Util;
import com.blackbear.flatworm.config.Bean;
import com.blackbear.flatworm.config.ConfigurationReader;
import com.blackbear.flatworm.config.ConfigurationValidator;
import com.blackbear.flatworm.config.ConversionOption;
import com.blackbear.flatworm.config.Converter;
import com.blackbear.flatworm.config.Line;
import com.blackbear.flatworm.config.LineElement;
import com.blackbear.flatworm.config.Record;
import com.blackbear.flatworm.config.RecordDefinition;
import com.blackbear.flatworm.config.RecordElement;
import com.blackbear.flatworm.config.SegmentElement;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;
import com.blackbear.flatworm.errors.FlatwormParserException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The {@code ConfigurationReader} class is used to initialize Flatworm with an XML configuration file which describes the format and
 * conversion options to be applied to the input file to produce output beans.
 *
 * @author Alan Henson
 */
public class DefaultConfigurationReader implements ConfigurationReader {
    /**
     * {@code loadConfigurationFile} takes an XML configuration file, and returns a {@code FileFormat} object, which can be used to parse an
     * input file into beans.
     *
     * @param xmlFilePath The path to an XML file which contains a valid Flatworm configuration.
     * @return A {@code FileFormat} object which can parse the specified format.
     * @throws FlatwormConfigurationException If the configuration data contains invalid syntax.
     * @throws IOException                    If issues occur while reading from I/O.
     */
    @Override
    public FileFormat loadConfigurationFile(String xmlFilePath) throws FlatwormConfigurationException, IOException {
        FileFormat fileFormat;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(xmlFilePath)) {
            if (in != null) {
                fileFormat = loadConfigurationFile(in);
            } else {
                try (InputStream inTakeTwo = new FileInputStream(xmlFilePath)) {
                    fileFormat = loadConfigurationFile(inTakeTwo);
                }
            }
        }
        return fileFormat;
    }


    /**
     * {@code loadConfigurationFile} takes an XML configuration file, and returns a {@code FileFormat} object, which can be used to parse an
     * input file into beans.
     *
     * @param xmlFile An XML file which contains a valid Flatworm configuration.
     * @return A {@code FileFormat} object which can parse the specified format.
     * @throws FlatwormConfigurationException If the configuration data contains invalid syntax.
     * @throws IOException                    If issues occur while reading from I/O.
     */
    @Override
    public FileFormat loadConfigurationFile(File xmlFile) throws FlatwormConfigurationException, IOException {
        return loadConfigurationFile(xmlFile.getAbsolutePath());
    }

    /**
     * {@code loadConfigurationFile} takes an {@link InputStream} and returns a {@code FileFormat} object, which can be used to parse an
     * input file into beans.
     *
     * @param in The {@link InputStream} instance to use in parsing the configuration file.
     * @return a constructed {@link FileFormat} if the parsing was successful.
     * @throws FlatwormConfigurationException If the configuration data contains invalid syntax.
     * @throws IOException                    If issues occur while reading from I/O.
     */
    @Override
    public FileFormat loadConfigurationFile(InputStream in) throws FlatwormConfigurationException, IOException {
        DocumentBuilder parser;
        Document document;
        NodeList children;
        FileFormat fileFormat = null;

        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            parser = fact.newDocumentBuilder();
            document = parser.parse((new InputSource(in)));
            children = document.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (("file-format".equals(child.getNodeName())) && (child.getNodeType() == Node.ELEMENT_NODE)) {
                    fileFormat = (FileFormat) traverse(child);
                    break;
                }
            }

            if (fileFormat != null) {
                // Make sure we haven't double dipped the default handling of data.
                if (fileFormat.hasDefaultRecord() && fileFormat.isIgnoreUnmappedRecords()) {
                    throw new FlatwormParserException("You cannot have default Records (those lacking identifier configuration) and " +
                            "the ignore-unmapped-records flag set to true - you must have one or the other.");
                }
            }
        } catch (Exception e) {
            throw new FlatwormConfigurationException(e.getMessage(), e);
        }
        return fileFormat;
    }

    /**
     * Given the {@code node}, collect the child nodes and return them in a {@link List} as their reconstituted object - this assumes that
     * the child nodes have configuration data that result in configuration DTOs.
     *
     * @param node The parent node.
     * @return a {@link List} of all child configuration elements in their DTO format.
     * @throws FlatwormConfigurationException should parsing the child nodes fail due configuration syntax errors.
     */
    protected List<Object> getChildNodes(Node node) throws FlatwormConfigurationException {
        List<Object> nodes = new ArrayList<>();
        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                Object o = traverse(child);
                if (o != null)
                    nodes.add(o);
            }
        }
        return nodes;
    }

    /**
     * Fetch the child node of {@code node} that has the given name.
     *
     * @param node     The parent node.
     * @param nodeName The name of the node to search for.
     * @return The child node by the given {@code nodeName} if it is found, else {@code null} is returned.
     */
    protected Node getChildElementNodeName(Node node, String nodeName) {
        NodeList children = node.getChildNodes();
        Node childNode = null;
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (nodeName.equals(child.getNodeName()) && child.getNodeType() == Node.ELEMENT_NODE) {
                    childNode = child;
                    break;
                }
            }
        }
        return childNode;
    }

    /**
     * Retrieve all children nodes of a given name from the parent {@code node}.
     *
     * @param node     The parent node.
     * @param nodeName The name of the nodes to collect.
     * @return a {@link List} of all nodes found by the given name.
     */
    protected List<Node> getChildElementNodesOfType(Node node, String nodeName) {
        List<Node> nodes = new ArrayList<>();
        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (nodeName.equals(child.getNodeName()) && child.getNodeType() == Node.ELEMENT_NODE)
                    nodes.add(child);
            }
        }
        return nodes;
    }

    /**
     * Retrieve the text value found within the given {@code node} and return it.
     *
     * @param node The node from which the text value will be retrieved.
     * @return the text value found in the given {@code node} or {@code null} if no text value exists.
     */
    protected String getChildTextNodeValue(Node node) {
        String textValue = null;
        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    textValue = child.getNodeValue();
                }
            }
        }
        return textValue;
    }

    /**
     * Retrieve the {@code CDATA} value from a given Node - noting that the {@code node}'s child nodes will be searched and the first {@code
     * CDATA} tag will be returned.
     *
     * @param node The node that will be searched.
     * @return The {@code CDATA} found as a {@link String} or {@code null} if a {@code CDATA} node was not found.
     */
    protected String getChildCDataNodeValue(Node node) {
        String result = null;
        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                    result = CharacterData.class.cast(child).getData();
                    if (result != null) {
                        result = result.trim();
                    }
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Determine if a given {@code node} has the given named attribute and if the value of that attribute is {@code null}.
     *
     * @param node The node to search.
     * @param name The name of the attribute.
     * @return {@code true} if the attribute exists in the node and isn't {@code null} and {@code false} if not.
     */
    protected boolean hasAttributeValueNamed(Node node, String name) {
        return (node != null && node.getAttributes().getNamedItem(name) != null);
    }

    /**
     * Get the value of a given attribute for the given node.
     *
     * @param node The node.
     * @param name The attribute name.
     * @return The value of the attribute if present and {@code null} if not.
     */
    protected String getAttributeValueNamed(Node node, String name) {
        return hasAttributeValueNamed(node, name) ? node.getAttributes().getNamedItem(name).getNodeValue() : null;
    }

    /**
     * Retrieve an attribute by name as a {@link Node}.
     *
     * @param node The node that may contain the attribute.
     * @param name The name of the attribute.
     * @return The attribute as a {@link Node} instance if found and {@code null} if not.
     */
    protected Node getAttributeNamed(Node node, String name) {
        NamedNodeMap map = node.getAttributes();
        return map.getNamedItem(name);
    }

    /**
     * Traverse the given {@link Node} and attemt to extract flatworm configuration DTOs from the content.
     *
     * @param node The node to traverse.
     * @return a constructed flatform DTO object if found - if not, {@code null} is returned.
     * @throws FlatwormConfigurationException should the parsing hit unsupported or incorrect syntax.
     */
    private Object traverse(Node node) throws FlatwormConfigurationException {
        int type = node.getNodeType();
        if (type == Node.ELEMENT_NODE) {
            String nodeName = node.getNodeName();

            // File Format.
            if (nodeName.equals("file-format")) {
                FileFormat fileFormat = readFileFormat(node);
                List<String> errors = ConfigurationValidator.validateFileFormat(fileFormat);
                if(!errors.isEmpty()) {
                    throw new FlatwormConfigurationException(
                            String.format("Validation of the configuration content failed. See below:%n%s",
                                Joiner.on(String.format("%n")).join(errors)));
                }
                return fileFormat;
            }

            // Converter.
            if (nodeName.equals("converter")) {
                return readConverter(node);
            }

            // Record.
            if (nodeName.equals("record")) {
                return readRecord(node);
            }

            // Record Definition.
            if (nodeName.equals("record-definition")) {
                return readRecordDefinition(node);
            }

            // Bean.
            if (nodeName.equals("bean")) {
                return readBean(node);
            }

            // Line.
            if (nodeName.equals("line")) {
                return readLine(node);
            }

            // Segments.
            if (nodeName.equals("segment-element")) {
                return readSegmentElement(node);
            }

            // Records.
            if (nodeName.equals("record-element")) {
                return readRecordElement(node);
            }
        }
        return null;
    }

    /**
     * Read {@link FileFormat} data from the given {@code node}. Note that all child nodes will be parsed and the DTO data that makes up a
     * {@link FileFormat} instance will also be parsed.
     *
     * @param node The node to parse.
     * @return a {@link FileFormat} instance constructed from the {@code node}'s data.
     * @throws FlatwormConfigurationException should the configuration data contain invalid syntax or values.
     */
    protected FileFormat readFileFormat(Node node) throws FlatwormConfigurationException {
        FileFormat fileFormat = new FileFormat();
        String encoding = Charset.defaultCharset().name();
        if (hasAttributeValueNamed(node, "encoding")) {
            encoding = getAttributeValueNamed(node, "encoding");
        }
        fileFormat.setEncoding(encoding);

        fileFormat.setIgnoreUnmappedRecords(Boolean.parseBoolean(getAttributeValueNamed(node, "ignore-unmapped-records")));

        List<Object> childNodes = getChildNodes(node);
        childNodes.forEach(childNode -> {
            if (childNode.getClass().isAssignableFrom(Converter.class)) {
                fileFormat.addConverter(Converter.class.cast(childNode));
            } else if (childNode.getClass().isAssignableFrom(Record.class)) {
                fileFormat.addRecord(Record.class.cast(childNode));
            }
        });
        return fileFormat;
    }

    /**
     * Read a {@link Converter} instance from the given {@code node}.
     *
     * @param node The node to parse.
     * @return a constructed {@link Converter} instance.
     */
    protected Converter readConverter(Node node) {
        return Converter.builder()
                .converterClass(getAttributeValueNamed(node, "class"))
                .method(getAttributeValueNamed(node, "method"))
                .returnType(getAttributeValueNamed(node, "return-type"))
                .name(getAttributeValueNamed(node, "name"))
                .build();
    }

    /**
     * Parse a {@link Record} instance out of the information found in the given node.
     *
     * @param node The node.
     * @return a constructed {@link Record} instance.
     * @throws FlatwormConfigurationException should the configuration data contain invalid syntax or values.
     */
    protected Record readRecord(Node node) throws FlatwormConfigurationException {
        Record record = new Record();
        record.setName(getAttributeValueNamed(node, "name"));
        Node identChild = getChildElementNodeName(node, "record-ident");
        if (identChild != null) {
            Node lengthChild = getChildElementNodeName(identChild, "length-ident");
            Node fieldChild = getChildElementNodeName(identChild, "field-ident");
            Node scriptChild = getChildElementNodeName(identChild, "script-ident");
            if (lengthChild != null) {
                record.setRecordIdentity(readLengthIdentity(lengthChild));
            } else if (fieldChild != null) {
                record.setRecordIdentity(readFieldIdentity(fieldChild));
            } else if (scriptChild != null) {
                record.setRecordIdentity(readScriptIdentity(scriptChild));
            }
        }

        Node recordChild = getChildElementNodeName(node, "record-definition");

        RecordDefinition recordDefinition = readRecordDefinition(recordChild);
        recordDefinition.setParentRecord(record);
        record.setRecordDefinition(recordDefinition);
        return record;
    }

    /**
     * Read the {@code length-ident} information from the given {@code node} and populate it in the {@link Record} instance.
     *
     * @param node The node containing the data.
     * @return a built {@link LengthIdentity} instance.
     */
    protected LengthIdentity readLengthIdentity(Node node) {
        LengthIdentity lengthIdentity = new LengthIdentity();
        lengthIdentity.setMinLength(Util.tryParseInt(getAttributeValueNamed(node, "min-length")));
        lengthIdentity.setMaxLength(Util.tryParseInt(getAttributeValueNamed(node, "max-length")));
        return lengthIdentity;
    }

    /**
     * Read the {@code field-ident} information from the given {@code node} and populate it in the {@link Record} instance.
     *
     * @param node The node containing the data.
     * @return a built {@link FieldIdentity} instance.
     */
    protected FieldIdentity readFieldIdentity(Node node) {
        FieldIdentity fieldIdentity = new FieldIdentity();

        fieldIdentity.setStartPosition(Util.tryParseInt(getAttributeValueNamed(node, "field-start")));
        fieldIdentity.setFieldLength(Util.tryParseInt(getAttributeValueNamed(node, "field-length")));

        List<Node> matchNodes = getChildElementNodesOfType(node, "match-string");
        matchNodes.forEach(matchNode -> fieldIdentity.addMatchingString(getChildTextNodeValue(matchNode)));

        return fieldIdentity;
    }

    /**
     * Read the {@code script-identity} tag values from the given node and populate the {@link Record} instance with the values.
     *
     * @param node The node containing the data.
     * @return a built {@link ScriptIdentity} instance.
     * @throws FlatwormConfigurationException should the {@code script-ident} node be invalid.
     */
    protected ScriptIdentity readScriptIdentity(Node node) throws FlatwormConfigurationException {
        String scriptEngineName = getAttributeValueNamed(node, "script-engine");
        String scriptMethodName = getAttributeValueNamed(node, "method-name");

        // Simple JavaScript doesn't require CDATA - see if it exists in the text field alone.
        String script = getChildTextNodeValue(node);

        // If not, look for CDATA.
        if (StringUtils.isBlank(script)) {
            script = getChildCDataNodeValue(node);
        }

        return new ScriptIdentity(scriptEngineName, script, scriptMethodName);
    }

    /**
     * Parse a {@link RecordDefinition} instance out of the information found in the given node.
     *
     * @param node The node.
     * @return a constructed {@link RecordDefinition} instance.
     * @throws FlatwormConfigurationException should the configuration data contain invalid syntax or values.
     */
    protected RecordDefinition readRecordDefinition(Node node) throws FlatwormConfigurationException {
        RecordDefinition rd = new RecordDefinition();

        List<Object> childNodes = getChildNodes(node);
        childNodes.forEach(childNode -> {
            if (childNode.getClass().isAssignableFrom(Bean.class)) {
                rd.addBean(Bean.class.cast(childNode));
            } else if (childNode.getClass().isAssignableFrom(Line.class)) {
                rd.addLine(Line.class.cast(childNode));
            }
        });
        return rd;
    }

    /**
     * Parse a {@link Bean} instance out of the information found in the given node.
     *
     * @param node The node.
     * @return a constructed {@link Bean} instance.
     * @throws FlatwormConfigurationException should the configuration data contain invalid syntax or values.
     */
    protected Bean readBean(Node node) throws FlatwormConfigurationException {
        Bean bean = new Bean();
        bean.setBeanName(getAttributeValueNamed(node, "name"));
        bean.setBeanClass(getAttributeValueNamed(node, "class"));
        try {
            bean.setBeanObjectClass(Class.forName(bean.getBeanClass()));
        } catch (ClassNotFoundException e) {
            throw new FlatwormConfigurationException("Unable to load class " + bean.getBeanClass(), e);
        }
        return bean;
    }

    /**
     * Parse a {@link Line} instance out of the information found in the given node.
     *
     * @param node The node.
     * @return a constructed {@link Line} instance.
     * @throws FlatwormConfigurationException should the configuration data contain invalid syntax or values.
     */
    protected Line readLine(Node node) throws FlatwormConfigurationException {
        Line line = new Line();

        // JBL - Determine if this line is delimited
        // Determine value of quote character, default = "
        // These fields are optional
        line.setDelimiter(getAttributeValueNamed(node, "delimit"));
        line.setQuoteChar(getAttributeValueNamed(node, "quote"));

        // Note the child nodes will be a collection of record-elements and segment-elements.
        // TODO - create a readLineElement method.
        List<Object> childNodes = getChildNodes(node);
        childNodes.stream()
                .filter(childNode -> childNode instanceof LineElement)
                .map(LineElement.class::cast)
                .forEach(line::addElement);

        return line;
    }

    /**
     * Parse a {@link SegmentElement} instance out of the information found in the given node.
     *
     * @param node The node.
     * @return a constructed {@link SegmentElement} instance.
     * @throws FlatwormConfigurationException should the configuration data contain invalid syntax or values.
     */
    protected SegmentElement readSegmentElement(Node node) throws FlatwormConfigurationException {
        SegmentElement segment = new SegmentElement();
        segment.setMinCount(Util.tryParseInt(getAttributeValueNamed(node, "minCount")));
        segment.setMaxCount(Util.tryParseInt(getAttributeValueNamed(node, "maxCount")));
        segment.setBeanRef(getAttributeValueNamed(node, "beanref"));
        segment.setParentBeanRef(getAttributeValueNamed(node, "parent-beanref"));
        segment.setAddMethod(getAttributeValueNamed(node, "add-method"));
        segment.setCollectionPropertyName(getAttributeValueNamed(node, "collection-property-name"));

        readCardinalityMode(node, segment);

        Node fieldChild = getChildElementNodeName(node, "field-ident");
        if (fieldChild != null) {
            segment.setFieldIdentity(readFieldIdentity(fieldChild));
        }

        List<Object> childNodes = getChildNodes(node);
        childNodes.stream()
                .filter(childNode -> childNode instanceof LineElement)
                .map(LineElement.class::cast)
                .forEach(segment::addElement);

        return segment;
    }

    /**
     * Parse a {@link SegmentElement}'s cardinality-mode out of the information found in the given node.
     *
     * @param node    The node.
     * @param segment The {@link SegmentElement} instance to populate with the data found.
     */
    protected void readCardinalityMode(Node node, SegmentElement segment) {
        String segmentMode = getAttributeValueNamed(node, "cardinality-mode");

        if (!StringUtils.isBlank(segmentMode)) {
            if (segmentMode.toLowerCase().startsWith("strict")) {
                segment.setCardinalityMode(CardinalityMode.STRICT);
            } else if (segmentMode.toLowerCase().startsWith("restrict")) {
                segment.setCardinalityMode(CardinalityMode.RESTRICTED);
            }
        } else {
            segment.setCardinalityMode(CardinalityMode.LOOSE);
        }
    }

    /**
     * Parse a {@link RecordElement} instance out of the information found in the given node.
     *
     * @param node The node.
     * @return a constructed {@link RecordElement} instance.
     * @throws FlatwormConfigurationException should the configuration data contain invalid syntax or values.
     */
    protected RecordElement readRecordElement(Node node) throws FlatwormConfigurationException {
        RecordElement recordElement = new RecordElement();

        Node start = getAttributeNamed(node, "start");
        Node end = getAttributeNamed(node, "end");

        Node length = getAttributeNamed(node, "length");
        Node beanref = getAttributeNamed(node, "beanref");
        Node converterName = getAttributeNamed(node, "converter-name");
        Node ignoreField = getAttributeNamed(node, "ignore-field");

        if (start != null) {
            recordElement.setFieldStart(Util.tryParseInt(start.getNodeValue()));
        }
        if (end != null) {
            recordElement.setFieldEnd(Util.tryParseInt(end.getNodeValue()));
        }
        if (length != null) {
            recordElement.setFieldLength(Util.tryParseInt(length.getNodeValue()));
        }
        if (beanref != null) {
            recordElement.setBeanRef(beanref.getNodeValue());
        }
        if (converterName != null) {
            recordElement.setConverterName(converterName.getNodeValue());
        }
        if(ignoreField != null) {
            recordElement.setIgnoreField(Util.tryParseBoolean(ignoreField.getNodeValue()));
        }

        readConversionOptions(node, recordElement);
        return recordElement;
    }

    /**
     * Parse a {@link ConversionOption}s out of the information found in the given node.
     *
     * @param node          The node.
     * @param recordElement The {@link RecordElement} instance to update with the {@link ConversionOption} instances found.
     */
    protected void readConversionOptions(Node node, RecordElement recordElement) {
        List<Node> childNodes = getChildElementNodesOfType(node, "conversion-option");
        for (Node o : childNodes) {
            String name = getAttributeValueNamed(o, "name");
            String value = getAttributeValueNamed(o, "value");
            ConversionOption co = new ConversionOption(name, value);
            recordElement.addConversionOption(name, co);
        }
    }

    // TODO need to refactor the validation logic to a single validation class.


}
