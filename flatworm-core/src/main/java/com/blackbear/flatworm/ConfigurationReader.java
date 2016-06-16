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

import com.blackbear.flatworm.errors.FlatwormConfigurationValueException;
import com.blackbear.flatworm.errors.FlatwormParserException;
import com.blackbear.flatworm.errors.FlatwormUnsetFieldValueException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * The {@code ConfigurationReader} class is used to initialize Flatworm with an XML configuration file which describes the format and
 * conversion options to be applied to the input file to produce output beans.
 */
public class ConfigurationReader {
    /**
     * {@code loadConfigurationFile} takes an XML configuration file, and returns a {@code FileFormat} object, which can be used to parse an
     * input file into beans.
     *
     * @param xmlFile An XML file which contains a valid Flatworm configuration.
     * @return A {@code FileFormat} object which can parse the specified format.
     * @throws FlatwormUnsetFieldValueException    If a required parameter of a tag is not set.
     * @throws FlatwormConfigurationValueException If the file contains invalid syntax.
     * @throws FlatwormParserException             Should the {@code xmlFile} fail to parse.
     * @throws IOException                         If the XML file cannot be opened for parsing.
     */
    public FileFormat loadConfigurationFile(String xmlFile) throws FlatwormUnsetFieldValueException,
            FlatwormConfigurationValueException, FlatwormParserException, IOException {
        FileFormat fileFormat;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(xmlFile)) {
            if (in != null) {
                fileFormat = loadConfigurationFile(in);
            } else {
                try (InputStream inTakeTwo = new FileInputStream(xmlFile)) {
                    fileFormat = loadConfigurationFile(inTakeTwo);
                }
            }
        }
        return fileFormat;
    }

    /**
     * {@code loadConfigurationFile} takes an {@link InputStream} and returns a {@code FileFormat} object, which can be used to parse an
     * input file into beans.
     *
     * @param in The {@link InputStream} instance to use in parsing the configuration file.
     * @return a constructed {@link FileFormat} if the parsing was successful.
     * @throws FlatwormUnsetFieldValueException    If a required parameter of a tag is not set.
     * @throws FlatwormConfigurationValueException If the file contains invalid syntax.
     * @throws FlatwormParserException             Should the {@code xmlFile} fail to parse.
     * @throws IOException                         If the XML file cannot be opened for parsing.
     */
    public FileFormat loadConfigurationFile(InputStream in) throws FlatwormUnsetFieldValueException,
            FlatwormConfigurationValueException, FlatwormParserException, IOException {
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
            throw new FlatwormParserException(e.getMessage(), e);
        }
        return fileFormat;
    }

    private List<Object> getChildNodes(Node node) throws FlatwormUnsetFieldValueException,
            FlatwormConfigurationValueException, FlatwormParserException {
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

    private Node getChildElementNodeOfType(String type, Node node) {
        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (type.equals(child.getNodeName()) && child.getNodeType() == Node.ELEMENT_NODE)
                    return child;
            }
        }
        return null;
    }

    private List<Node> getChildElementNodesOfType(String type, Node node) {
        List<Node> nodes = new ArrayList<>();
        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (type.equals(child.getNodeName()) && child.getNodeType() == Node.ELEMENT_NODE)
                    nodes.add(child);
            }
        }
        return nodes;
    }

    private String getChildTextNodeValue(Node node) {
        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE)
                    return child.getNodeValue();
            }
        }
        return null;
    }

    private String getChildCDataNodeValue(Node node) {
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

    private boolean hasAttributeValueNamed(Node node, String name) {
        return (node != null && node.getAttributes().getNamedItem(name) != null);
    }

    private String getAttributeValueNamed(Node node, String name) {
        return hasAttributeValueNamed(node, name) ? node.getAttributes().getNamedItem(name).getNodeValue() : null;
    }

    private Node getAttributeNamed(Node node, String name) {
        NamedNodeMap map = node.getAttributes();
        return map.getNamedItem(name);
    }

    private Object traverse(Node node) throws FlatwormUnsetFieldValueException, FlatwormConfigurationValueException,
            FlatwormParserException {
        int type = node.getNodeType();
        if (type == Node.ELEMENT_NODE) {
            String nodeName = node.getNodeName();

            // File Format.
            if (nodeName.equals("file-format")) {
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

            // Converter.
            if (nodeName.equals("converter")) {
                return Converter.builder()
                        .converterClass(getAttributeValueNamed(node, "class"))
                        .method(getAttributeValueNamed(node, "method"))
                        .returnType(getAttributeValueNamed(node, "return-type"))
                        .name(getAttributeValueNamed(node, "name"))
                        .build();
            }

            // Record.
            if (nodeName.equals("record")) {
                Record record = new Record();
                record.setName(getAttributeValueNamed(node, "name"));
                Node identChild = getChildElementNodeOfType("record-ident", node);
                if (identChild != null) {
                    Node fieldChild = getChildElementNodeOfType("field-ident", identChild);
                    Node lengthChild = getChildElementNodeOfType("length-ident", identChild);
                    Node scriptChild = getChildElementNodeOfType("script-ident", identChild);
                    if (lengthChild != null) {
                        record.setLengthIdentMin(Util.tryParseInt(getAttributeValueNamed(lengthChild, "minlength")));
                        record.setLengthIdentMax(Util.tryParseInt(getAttributeValueNamed(lengthChild, "maxlength")));
                        record.setIdentTypeFlag('L');
                    } else if (fieldChild != null) {
                        record.setFieldIdentStart(Util.tryParseInt(getAttributeValueNamed(fieldChild, "field-start")));
                        record.setFieldIdentLength(Util.tryParseInt(getAttributeValueNamed(fieldChild, "field-length")));
                        record.setIdentTypeFlag('F');

                        List<Node> matchNodes = getChildElementNodesOfType("match-string", fieldChild);
                        matchNodes.forEach(matchNode -> record.addFieldIdentMatchString(getChildTextNodeValue(matchNode)));
                    } else if (scriptChild != null) {
                        try {
                            record.setFieldIdentScript(getChildCDataNodeValue(scriptChild));
                            record.setIdentTypeFlag('S');
                        } catch (ScriptException e) {
                            throw new FlatwormParserException(String.format("Record entry %s has an invalid script. Err: %s",
                                    record.getName(), e.getMessage()), e);
                        }
                    }
                }
                Node recordChild = getChildElementNodeOfType("record-definition", node);
                record.setRecordDefinition((RecordDefinition) traverse(recordChild));
                return record;
            }

            // Record Definition.
            if (nodeName.equals("record-definition")) {
                RecordDefinition rd = new RecordDefinition();

                List<Object> childNodes = getChildNodes(node);
                childNodes.forEach(childNode -> {
                    if (childNode.getClass().isAssignableFrom(Bean.class)) {
                        rd.addBeanUsed(Bean.class.cast(childNode));
                    } else if (childNode.getClass().isAssignableFrom(Line.class)) {
                        rd.addLine(Line.class.cast(childNode));
                    }
                });
                return rd;
            }

            // Bean.
            if (nodeName.equals("bean")) {
                Bean b = new Bean();
                b.setBeanName(getAttributeValueNamed(node, "name"));
                b.setBeanClass(getAttributeValueNamed(node, "class"));
                try {
                    b.setBeanObjectClass(Class.forName(b.getBeanClass()));
                } catch (ClassNotFoundException e) {
                    throw new FlatwormConfigurationValueException("Unable to load class " + b.getBeanClass());
                }
                return b;
            }

            // Line.
            if (nodeName.equals("line")) {
                Line li = new Line();

                // JBL - Determine if this line is delimited
                // Determine value of quote character, default = "
                // These fields are optional
                li.setDelimiter(getAttributeValueNamed(node, "delimit"));
                li.setQuoteChar(getAttributeValueNamed(node, "quote"));

                List<Object> childNodes = getChildNodes(node);
                childNodes.stream()
                        .filter(childNode -> childNode instanceof LineElement)
                        .map(LineElement.class::cast)
                        .forEach(li::addElement);

                return li;
            }

            // Segments.
            if (nodeName.equals("segment-element")) {
                SegmentElement segment = new SegmentElement();
                segment.setCardinalityMode(CardinalityMode.LOOSE);
                segment.setMinCount(Util.tryParseInt(getAttributeValueNamed(node, "minCount")));
                segment.setMaxCount(Util.tryParseInt(getAttributeValueNamed(node, "maxCount")));
                segment.setBeanRef(getAttributeValueNamed(node, "beanref"));
                segment.setParentBeanRef(getAttributeValueNamed(node, "parent-beanref"));
                segment.setCollectionPropertyName(getAttributeValueNamed(node, "collection-property-name"));
                segment.setAddMethod(getAttributeValueNamed(node, "add-method"));
                String segmentMode = getAttributeValueNamed(node, "cardinality-mode");
                if (!StringUtils.isBlank(segmentMode)) {
                    if (segmentMode.toLowerCase().startsWith("strict")) {
                        segment.setCardinalityMode(CardinalityMode.STRICT);
                    } else if (segmentMode.toLowerCase().startsWith("restrict")) {
                        segment.setCardinalityMode(CardinalityMode.RESTRICTED);
                    }
                }

                Node fieldChild = getChildElementNodeOfType("field-ident", node);
                if (fieldChild != null) {
                    segment.setFieldIdentStart(Util.tryParseInt(getAttributeValueNamed(fieldChild, "field-start")));
                    segment.setFieldIdentLength(Util.tryParseInt(getAttributeValueNamed(fieldChild, "field-length")));
                    List<Node> matchNodes = getChildElementNodesOfType("match-string", fieldChild);
                    matchNodes.forEach(matchNode -> segment.addFieldIdentMatchString(getChildTextNodeValue(matchNode)));
                }
                validateSegmentConfiguration(segment);
                List<Object> childNodes = getChildNodes(node);
                childNodes.stream()
                        .filter(childNode -> childNode instanceof LineElement)
                        .map(LineElement.class::cast)
                        .forEach(segment::addElement);

                return segment;
            }

            // Records.
            if (nodeName.equals("record-element")) {
                RecordElement re = new RecordElement();

                Node start = getAttributeNamed(node, "start");
                Node end = getAttributeNamed(node, "end");
                Node length = getAttributeNamed(node, "length");
                Node beanref = getAttributeNamed(node, "beanref");
                Node beanType = getAttributeNamed(node, "type");
                if ((end == null) && (length == null)) {
                    throw new FlatwormConfigurationValueException("Must set either the 'end' or 'length' properties");
                }
                if ((end != null) && (length != null)) {
                    throw new FlatwormConfigurationValueException("Can't specify both the 'end' or 'length' properties");
                }
                if (start != null) {
                    re.setFieldStart(Util.tryParseInt(start.getNodeValue()));
                }
                if (end != null) {
                    re.setFieldEnd(Util.tryParseInt(end.getNodeValue()));
                }
                if (length != null) {
                    re.setFieldLength(Util.tryParseInt(length.getNodeValue()));
                }
                if (beanref != null) {
                    re.setBeanRef(beanref.getNodeValue());
                }
                if (beanType != null) {
                    re.setType(beanType.getNodeValue());
                }
                List<Node> childNodes = getChildElementNodesOfType("conversion-option", node);
                for (Node o : childNodes) {
                    String name = getAttributeValueNamed(o, "name");
                    String value = getAttributeValueNamed(o, "value");
                    ConversionOption co = new ConversionOption(name, value);
                    re.addConversionOption(name, co);
                }
                return re;
            }
        }
        return null;
    }

    private void validateSegmentConfiguration(SegmentElement segment) throws FlatwormConfigurationValueException {
        StringBuilder errors = new StringBuilder();
        if (StringUtils.isBlank(segment.getParentBeanRef())) {
            errors.append(String.format("Must specify the parent-beanref attribute for segment-elements.%n"));
        }
        if (StringUtils.isBlank(segment.getBeanRef())) {
            errors.append(String.format("Must specify the beanref attribute for segment-elements.%n"));
        }
        if (StringUtils.isBlank(segment.getCollectionPropertyName()) && StringUtils.isBlank(segment.getAddMethod())) {
            errors.append(String.format("Must specify either the collection-property-name attribute or add-method attribute for segment-elements.%n"));
        }
        if (segment.getFieldIdentMatchStrings().size() == 0) {
            errors.append(String.format("Must specify the segment identifier.%n"));
        }
        if (errors.length() > 0) {
            throw new FlatwormConfigurationValueException(errors.toString());
        }
    }
}
