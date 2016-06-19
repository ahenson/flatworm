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

package com.blackbear.flatworm.converters;

import com.blackbear.flatworm.config.ConversionOption;

import java.util.Map;

/**
 * Provide a contract that aligns with how the {@link CoreConverters} manages its built in Converters.
 *
 * @author Alan Henson
 */
@FunctionalInterface
public interface FromTypeConverterFunction {

    /**
     * Convert the given {@code value} into a ({@code String}.
     * @param value The {@code value} to convert to a {@link String}.
     * @param options Any {@link ConversionOption}s that were configured to go along with the converter.
     * @return The {@code value} value converted to a {@link String}.
     * @throws Exception should anything unexpected occur.
     */
    String convert(Object value, Map<String, ConversionOption> options) throws Exception;
}
