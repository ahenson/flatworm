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

import com.blackbear.flatworm.config.Line;
import com.blackbear.flatworm.config.LineElement;
import com.blackbear.flatworm.config.Record;
import com.blackbear.flatworm.config.RecordDefinition;
import com.blackbear.flatworm.config.RecordElement;
import com.blackbear.flatworm.converters.ConversionHelper;
import com.blackbear.flatworm.converters.ConversionOption;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;

import org.apache.commons.beanutils.PropertyUtils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Used to create a flatfile. This class wraps the functionality that used to be in the main() of the examples. This way, the client knows
 * less about the internal workings of FlatWorm.
 */
@Slf4j
public class FileCreator {
    private String file;

    private FileFormat ff;

    private BufferedWriter bufOut;

    private Map<String, Object> beans = new HashMap<>();

    private String recordSeparator = null;

    private OutputStream outputStream;

    /**
     * Constructor for FileCreator.
     *
     * @param config Full path to the FlatWorm XML configuration file.
     * @param file   Full path to output file.
     * @throws FlatwormConfigurationException should any issues occur in parsing the configuration data.
     */
    public FileCreator(String config, String file) throws FlatwormConfigurationException {
        this.file = file;
        this.outputStream = null;
        loadConfigurationFile(config);
    }

    public FileCreator(String config, OutputStream stream) throws FlatwormConfigurationException {
        this.file = null;
        this.outputStream = stream;
        loadConfigurationFile(config);
    }

    public FileCreator(InputStream config, String file) throws FlatwormConfigurationException {
        this.file = file;
        this.outputStream = null;
        loadConfigurationFile(config);
    }

    public FileCreator(InputStream config, OutputStream stream) throws FlatwormConfigurationException {
        this.file = null;
        this.outputStream = stream;
        loadConfigurationFile(config);
    }

    private void loadConfigurationFile(InputStream configStream) throws FlatwormConfigurationException {
        ConfigurationReader parser = new ConfigurationReader();
        try {
            ff = parser.loadConfigurationFile(configStream);
        } catch (Exception ex) {
            throw new FlatwormConfigurationException(ex.getMessage(), ex);
        }
    }

    private void loadConfigurationFile(String config) throws FlatwormConfigurationException {
        // Load configuration xml file
        try {
            ConfigurationReader parser = new ConfigurationReader();
            InputStream configStream = this.getClass().getClassLoader().getResourceAsStream(config);
            if (configStream != null) {
                ff = parser.loadConfigurationFile(configStream);
            } else {
                ff = parser.loadConfigurationFile(config);
            }
        } catch (Exception ex) {
            throw new FlatwormConfigurationException(ex.getMessage(), ex);
        }
    }

    /**
     * Open the file for writing.
     *
     * @throws FileNotFoundException        should the file specified not exist.
     * @throws UnsupportedEncodingException should the format of the file not be supported.
     */
    public void open() throws FileNotFoundException, UnsupportedEncodingException {
        // Setup buffered writer
        if (file != null) {
            outputStream = new FileOutputStream(file);
        }
        bufOut = new BufferedWriter(new OutputStreamWriter(outputStream, ff.getEncoding()));
    }

    /**
     * This is a convenience method that lets the writer know about your bean without having to pass a HashMap to write()<br>
     *
     * @param name The name of bean as defined in your flatworm XML file
     * @param bean The bean object
     */
    public void setBean(String name, Object bean) {
        beans.put(name, bean);
    }

    /**
     * Flatworm does not assume you want a newline between records, call this method to set your record delimiter.<br>
     *
     * @param recordSeparator The String you want to use to separate your records. Could be "\n"
     */
    public void setRecordSeparator(String recordSeparator) {
        this.recordSeparator = recordSeparator;
    }

    /**
     * Close the output file, since we are using buffered IO, this is very important.<br>
     *
     * @throws IOException - If the file system chooses not to close your file for some unknown reason
     */
    public void close() throws IOException {
        bufOut.close();
    }

    /**
     * Write information to the output file. Make sure you have called the setBean() method with the needed beans before calling this
     * method.
     *
     * @param recordName The name specified in your flatworm configuration file for this record
     * @throws IOException              - If the file system has a problem with you writing information to the recently opened file.
     * @throws FlatwormConfigurationException - wraps various Exceptions so client doesn't have to handle too many
     */
    public void write(String recordName) throws IOException, FlatwormConfigurationException {
        Record record = ff.getRecord(recordName);
        RecordDefinition recDef = record.getRecordDefinition();

        List<Line> lines = recDef.getLines();

        // Iterate over lines
        boolean first = true;
        for (Line line : lines) {
            String delimit = line.getDelimiter();
            if (null == delimit)
                delimit = "";

            // record-ident contain what is considered hard-coded data
            // for the output line, these can be used to uniquely identify
            // lines for parsers. We need to write them out.
            // For multiline records they should only be written for the first line -
            // Dave Derry 11/2009
            List<String> recIdents = record.getFieldIdentMatchStrings();
            if (first) {
                for (String id : recIdents) {
                    bufOut.write(id + delimit);
                }
            }

            // Iterate over record-element items
            List<LineElement> recElements = line.getElements();
            for (Iterator<LineElement> itRecElements = recElements.iterator(); itRecElements.hasNext(); ) {
                LineElement lineElement = itRecElements.next();
                if (lineElement instanceof RecordElement) {
                    RecordElement recElement = (RecordElement) lineElement;
                    String beanRef = recElement.getBeanRef();
                    String type = recElement.getType();

                    if (recElement.getFieldLength() == null) {
                        throw new FlatwormConfigurationException(
                                String.format("Could not deduce field length (please provide more data in your xml file for : %s)",
                                        beanRef));
                    }

                    Map<String, ConversionOption> convOptions = recElement.getConversionOptions();

                    String val = "";
                    ConversionHelper convHelper = ff.getConversionHelper();
                    try {
                        if (beanRef != null) {
                            // Extract property name
                            Object bean;
                            String property;
                            try {
                                int posOfFirstDot = beanRef.indexOf('.');
                                bean = beans.get(beanRef.substring(0, posOfFirstDot));
                                property = beanRef.substring(posOfFirstDot + 1);
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                throw new FlatwormConfigurationException("Had trouble parsing : " + beanRef
                                        + " Its format should be <bean_name>.<property_name>");
                            }

                            // Convert to String for output
                            Object value = PropertyUtils.getProperty(bean, property);
                            val = convHelper.convert(type, value, convOptions, beanRef);
                            PropertyUtils.setProperty(bean, property, value);
                        } // end beanRef != null
                        // Handle any conversions that need to occur
                        if (val == null) {
                            val = "";
                        }
                        val = convHelper.transformString(val, recElement.getConversionOptions(),
                                recElement.getFieldLength());

                        if (itRecElements.hasNext())
                            bufOut.write(val + delimit);
                        else
                            bufOut.write(val);

                    } catch (Exception ex) {
                        throw new FlatwormConfigurationException("Exception getting/converting bean property : "
                                + beanRef + " : " + ex.getMessage());
                    }
                }
            } // end for all record elements

            if (null != recordSeparator)
                bufOut.write(recordSeparator);

            first = false;
        } // end for all lines

    } // end method

} // end class
