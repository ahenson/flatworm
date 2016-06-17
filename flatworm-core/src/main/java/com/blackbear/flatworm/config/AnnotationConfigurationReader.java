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

import java.util.Collection;

/**
 * The {@code AnnotationConfigurationReaderConfigurationReader} interface sets the contract for reading
 * the flatworm configuration via annotated classes.
 *
 * @author Alan Henson
 */
public interface AnnotationConfigurationReader {

    /**
     * Given the {@code classes}, parse through them looking for annotated configuration for the flatworm {@link FileFormat}
     * configuration.
     * @param classes The classes to use in building the {@link FileFormat} configuration.
     * @return The constructed {@link FileFormat} instance based upon the configuration found in the annotations within the classes. If
     * the {@code classes} parameter is {@code null} then {@code null} should be returned.
     */
    FileFormat loadConfiguration(Class<?> ... classes);

    /**
     * Given the {@code classes}, parse through them looking for annotated configuration for the flatworm {@link FileFormat}
     * configuration.
     * @param classes The classes to use in building the {@link FileFormat} configuration.
     * @return The constructed {@link FileFormat} instance based upon the configuration found in the annotations within the classes.
     */
    FileFormat loadConfiguration(Collection<Class<?>> classes);
}
