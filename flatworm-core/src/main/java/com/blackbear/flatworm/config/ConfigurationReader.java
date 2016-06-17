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
import com.blackbear.flatworm.errors.FlatwormConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * The {@code ConfigurationReader} interface sets the contract for reading the XML format of the flatworm configuration format.
 */
public interface ConfigurationReader {
    /**
     * {@code loadConfigurationFile} takes an XML configuration file path, and returns a {@code FileFormat} object, which can be used to
     * parse an input file into beans.
     *
     * @param xmlFilePath The path to an XML file which contains a valid Flatworm configuration.
     * @return A {@code FileFormat} object which can parse the specified format.
     * @throws FlatwormConfigurationException If the configuration data contains invalid syntax.
     * @throws IOException                    If issues occur while reading from I/O.
     */
    FileFormat loadConfigurationFile(String xmlFilePath) throws FlatwormConfigurationException, IOException;

    /**
     * {@code loadConfigurationFile} takes an XML configuration file, and returns a {@code FileFormat} object, which can be used to parse an
     * input file into beans.
     *
     * @param xmlFile An XML file which contains a valid Flatworm configuration.
     * @return A {@code FileFormat} object which can parse the specified format.
     * @throws FlatwormConfigurationException If the configuration data contains invalid syntax.
     * @throws IOException                    If issues occur while reading from I/O.
     */
    FileFormat loadConfigurationFile(File xmlFile) throws FlatwormConfigurationException, IOException;

    /**
     * {@code loadConfigurationFile} takes an {@link InputStream} and returns a {@code FileFormat} object, which can be used to parse an
     * input file into beans.
     *
     * @param in The {@link InputStream} instance to use in parsing the configuration file.
     * @return a constructed {@link FileFormat} if the parsing was successful.
     * @throws FlatwormConfigurationException If the configuration data contains invalid syntax.
     * @throws IOException                    If issues occur while reading from I/O.
     */
    FileFormat loadConfigurationFile(InputStream in) throws FlatwormConfigurationException, IOException;
}
