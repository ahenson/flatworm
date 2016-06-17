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
import com.blackbear.flatworm.config.Record;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Goes above and beyond the {@link ConversionHelper} by providing a singleton like experience where multiple converter methods can be
 * registered based upon which property types they support. This is meant to be a generic approach so if you need a specific {@code
 * Converter} then please add it to the configuration file and reference it from your given {@link Record}
 * configurations.
 *
 * @author Alan Henson
 * @since 2016.1.0.0
 */
public class ConverterFunctionCache {

    private static Map<String, ToTypeConverterFunction> toTypeConverterFunctionCache = new HashMap<>();
    private static Map<String, FromTypeConverterFunction> fromTypeConverterFunctionCache = new HashMap<>();

    private static CoreConverters coreConverters = new CoreConverters();

    private ConverterFunctionCache() {
    }

    static {
        registerToTypeConverterFunction(String.class, coreConverters::convertChar);
        registerToTypeConverterFunction(Double.class, coreConverters::convertDouble);
        registerToTypeConverterFunction(BigDecimal.class, coreConverters::convertBigDecimal);
        registerToTypeConverterFunction(Float.class, coreConverters::convertFloat);
        registerToTypeConverterFunction(Long.class, coreConverters::convertLong);
        registerToTypeConverterFunction(Integer.class, coreConverters::convertInteger);
        registerToTypeConverterFunction(Date.class, coreConverters::convertDate);

        registerFromTypeConverterFunction(String.class, coreConverters::convertChar);
        registerFromTypeConverterFunction(Double.class, coreConverters::convertDouble);
        registerFromTypeConverterFunction(BigDecimal.class, coreConverters::convertBigDecimal);
        registerFromTypeConverterFunction(Float.class, coreConverters::convertFloat);
        registerFromTypeConverterFunction(Long.class, coreConverters::convertLong);
        registerFromTypeConverterFunction(Integer.class, coreConverters::convertInteger);
        registerFromTypeConverterFunction(Date.class, coreConverters::convertDate);
    }

    /**
     * Attempt to perform a conversion from a {@link String} to a designated {@link Object} instance.
     * @param clazz The class of the {@link Object} instance that the value is ultimately to be converted to.
     * @param value The value that is to be converted.
     * @param options Any options that are to be handed to the converter.
     * @return The {@link Object} instance created from the {@code value} if the conversion was successful and {@code null} if not.
     * @throws Exception Should the conversion process have unexpected issues.
     */
    public static Object convertFromString(Class<?> clazz, String value, Map<String, ConversionOption> options) throws Exception {
        Object result = null;
        ToTypeConverterFunction function = findToTypeConverter(clazz);
        if(function != null) {
            result = function.convert(value, options);
        }
        return result;
    }

    /**
     * Attempt to perform a conversion from a {@link Object} instance to a {@link String}.
     * @param value The value that is to be converted to a {@link String} - it's class will drive the lookup.
     * @param options Any options that are to be handed to the converter.
     * @return The {@link String} instance created from the {@code value} if the conversion was successful and {@code null} if not.
     * @throws Exception Should the conversion process have unexpected issues.
     */
    public static String convertToString(Object value, Map<String, ConversionOption> options) throws Exception {
        String result = null;
        FromTypeConverterFunction function = findFromTypeConverter(value.getClass());
        if(function != null) {
            result = function.convert(value, options);
        }
        return result;
    }

    /**
     * Register a {@link ToTypeConverterFunction} function by the {@link Class} that will trigger its selection
     * for a conversion.
     * @param clazz The {@link Class}.
     * @param function The {@link ToTypeConverterFunction} function to register.
     */
    public static void registerToTypeConverterFunction(Class<?> clazz, ToTypeConverterFunction function) {
        toTypeConverterFunctionCache.put(clazz.getName(), function);
    }

    /**
     * Remove a {@link ToTypeConverterFunction} converter from the cache.
     * @param clazz The class for which the {@link ToTypeConverterFunction} was registered.
     * @return the {@link ToTypeConverterFunction} instance removed from cache if found and {@code null} if not.
     */
    public static ToTypeConverterFunction removeToTypeConverterFunction(Class<?> clazz) {
        return toTypeConverterFunctionCache.remove(clazz.getName());
    }

    /**
     * Register a {@link FromTypeConverterFunction} function by the {@link Class} that will trigger its selection
     * for a conversion.
     * @param clazz The {@link Class}.
     * @param function The {@link FromTypeConverterFunction} function to register.
     */
    public static void registerFromTypeConverterFunction(Class<?> clazz, FromTypeConverterFunction function) {
        fromTypeConverterFunctionCache.put(clazz.getName(), function);
    }

    /**
     * Remove a {@link FromTypeConverterFunction} converter from the cache.
     * @param clazz The class for which the {@link FromTypeConverterFunction} was registered.
     * @return the {@link FromTypeConverterFunction} instance removed from cache if found and {@code null} if not.
     */
    public static FromTypeConverterFunction removeFromTypeConverterFunction(Class<?> clazz) {
        return fromTypeConverterFunctionCache.remove(clazz.getName());
    }

    /**
     * Attempt to find a converter from a {@link String} to a {@link Object} instance based
     * upon the {@code clazz} converterName. This will look for an exact match by class name and it will
     * then look for anything that {@code clazz} inherits from to see if that converter would work.
     * The best way to avoid having the wrong converter chosen is to have specific converters
     * for subtypes when the converter for the parent converterName won't work.
     * @param clazz The {@link Class} to find the converter for.
     * @return The {@link ToTypeConverterFunction} function if found by the {@code clazz} or {@code null}.
     */
    public static ToTypeConverterFunction findToTypeConverter(Class<?> clazz) {
        ToTypeConverterFunction result = null;

        Optional<String> match = findKey(clazz, toTypeConverterFunctionCache.keySet());
        if (match.isPresent()) {
            result = toTypeConverterFunctionCache.get(match.get());
        }

        return result;
    }

    /**
     * Attempt to find a converter from an instance of an {@link Object} to a {@link String} based
     * upon the {@code clazz} converterName. This will look for an exact match by class name and it will
     * then look for anything that {@code clazz} inherits from to see if that converter would work.
     * The best way to avoid having the wrong converter chosen is to have specific converters
     * for subtypes when the converter for the parent converterName won't work.
     * @param clazz The {@link Class} to find the converter for.
     * @return The {@link FromTypeConverterFunction} function if found by the {@code clazz} or {@code null}.
     */
    public static FromTypeConverterFunction findFromTypeConverter(Class<?> clazz) {
        FromTypeConverterFunction result = null;

        Optional<String> match = findKey(clazz, fromTypeConverterFunctionCache.keySet());
        if (match.isPresent()) {
            result = fromTypeConverterFunctionCache.get(match.get());
        }
        return result;
    }

    /**
     * See if the given clazz name is in the given {@code keys} collection by direct match and then
     * by seeing if it represents a subclass of a class that is in the keys collection.
     * @param clazz The Class to search by.
     * @param keys The collection of class names that are keys.
     * @return an {@link Optional} instance that contains the result of the search.
     */
    private static Optional<String> findKey(Class<?> clazz, Collection<String> keys) {
        Optional<String> match = keys
                .stream()
                .filter(entry -> entry.equals(clazz.getName()))
                .findFirst();

        // Try a heavier approach.
        if (!match.isPresent()) {
            match = keys.stream()
                    .map(ConverterFunctionCache::loadClass)
                    .filter(entry -> entry != null && entry.isAssignableFrom(clazz))
                    .map(Class::getName)
                    .findFirst();
        }
        return match;
    }

    /**
     * Attempt to load a class into memory by name and fail silently if unable to do so.
     * @param name The name of the class to attempt to load.
     * @return The loaded class or {@code null} if the class could not be found or failed to load.
     */
    private static Class<?> loadClass(String name) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(name);
        } catch (Exception e) {
            // Eating this as it technically shouldn't happen since we used the Class
            // to create the key (generated hot-swapping might create a weird condition so
            // eating to be safe).
        }
        return clazz;
    }
}
