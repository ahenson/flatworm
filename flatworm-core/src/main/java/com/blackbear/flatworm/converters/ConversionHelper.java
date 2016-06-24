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

import com.blackbear.flatworm.Util;
import com.blackbear.flatworm.config.ConversionOptionBO;
import com.blackbear.flatworm.config.ConverterBO;
import com.blackbear.flatworm.errors.FlatwormParserException;

import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * The {@code ConversionHelper} was created to separate formatting responsibility into a separate class. This class also makes writing your
 * own converter more of a reality by separating converterName conversion from string formatting. String formatting has moved to a separate class
 * called Util.
 */
@Slf4j
public class ConversionHelper {
    private Map<String, ConverterBO> converters;

    private Map<ConverterBO, Method> converterMethodCache;
    private Map<ConverterBO, Method> converterToStringMethodCache;
    private Map<String, Object> converterObjectCache;

    public ConversionHelper() {
        converters = new HashMap<>();
        converterMethodCache = new HashMap<>();
        converterToStringMethodCache = new HashMap<>();
        converterObjectCache = new HashMap<>();
    }

    /**
     * Attempt to convert the given {@code fieldChars} to an instance of a {@link Object}.
     *
     * @param converterName       The name of the converter from the xml configuration file.
     * @param fieldChars The value of the field as read from the input file.
     * @param options    Map of ConversionOptions (if any) for this field.
     * @param beanRef    "class.property", used for more descriptive exception messages, should something go wrong.
     * @return The {@link Object} constructed from the {@code fieldChars} value.
     * @throws FlatwormParserException should parsing the value to a {@link Object} fail for any reason.
     */
    public Object convert(String converterName, String fieldChars, Map<String, ConversionOptionBO> options, String beanRef)
            throws FlatwormParserException {

        Object value;

        try {
            Object object = getConverterObject(converterName);
            Method method = getConverterMethod(converterName);

            fieldChars = transformString(fieldChars, options, 0);

            Object[] args = {fieldChars, options};
            value = method.invoke(object, args);
        } catch (Exception e) {
            log.error("While running convert method for " + beanRef, e);
            throw new FlatwormParserException("Converting field " + beanRef + " with value '"
                    + fieldChars + "'");
        }
        return value;
    }

    /**
     * Use an alternate method that attempts to use reflection to figure out which conversion routine to use.
     *
     * @param bean         The {@link Object} that contains the property.
     * @param beanName     The name of the bean as configured.
     * @param propertyName The name of the property that is to be set.
     * @param fieldChars   The value.
     * @param options      The {@link ConversionOptionBO}s.
     * @return The {@link Object} constructed from the {@code fieldChars} value.
     * @throws FlatwormParserException should parsing the value to a {@link Object} fail for any reason.
     */
    public Object convert(Object bean, String beanName, String propertyName, String fieldChars, Map<String, ConversionOptionBO> options)
            throws FlatwormParserException {
        Object value;
        try {
            PropertyDescriptor propDescriptor = PropertyUtils.getPropertyDescriptor(bean, propertyName);
            value = ConverterFunctionCache.convertFromString(propDescriptor.getPropertyType(), fieldChars, options);
        } catch (Exception e) {
            throw new FlatwormParserException(String.format("Failed to convert and set value '%s' on bean %s [%s] for property %s.",
                    fieldChars, beanName, bean.getClass().getName(), propertyName), e);
        }
        return value;
    }

    /**
     * Convert a given {@link Object} to a String.
     *
     * @param type    The converter converterName specified.
     * @param obj     The {@link Object} to convert to a String.
     * @param options The {@link ConversionOptionBO}s.
     * @param beanRef The reference to the bean that has the property.
     * @return The {@link String} value of the {@link Object}.
     * @throws FlatwormParserException should converting the {@link Object} fail for any reason.
     */
    public String convert(String type, Object obj, Map<String, ConversionOptionBO> options, String beanRef) throws FlatwormParserException {
        String result;
        try {
            Object converter = getConverterObject(type);
            Method method = getToStringConverterMethod(type);
            Object[] args = {obj, options};
            result = (String) method.invoke(converter, args);
        } catch (Exception e) {
            throw new FlatwormParserException("Converting field " + beanRef + " to string for value '" + obj + "'", e);
        }
        return result;
    }

    /**
     * Convert a given {@link Object} to a String.
     * @param obj     The {@link Object} to convert to a String.
     * @param options The {@link ConversionOptionBO}s.
     * @param beanRef The reference to the bean that has the property.
     * @return The {@link String} value of the {@link Object}.
     * @throws FlatwormParserException should converting the {@link Object} fail for any reason.
     */
    public String convert(Object obj, Map<String, ConversionOptionBO> options, String beanRef) throws FlatwormParserException {
        String result;
        try {
            result = ConverterFunctionCache.convertToString(obj, options);
        } catch (Exception e) {
            throw new FlatwormParserException("Converting field " + beanRef + " to string for value '" + obj + "'", e);
        }
        return result;
    }

    /**
     * Handles the processing of the Conversion-Options from the flatworm XML file
     *
     * @param fieldChars The string to be transformed
     * @param options    Collection of ConversionOptionBO objects
     * @param length     Used in justification to ensure proper formatting
     * @return The transformed string
     */
    public String transformString(String fieldChars, Map<String, ConversionOptionBO> options, int length) {
        // JBL - Implement iteration of conversion-options
        // Iterate over conversion-options, that way, the xml file
        // can drive the order of conversions, instead of having them
        // hard-coded like in 'removePadding' (old way)
        Set<String> keys = options.keySet();
        for (String key : keys) {
            ConversionOptionBO conv = options.get(key);

            if (conv.getName().equals("justify"))
                fieldChars = Util.justify(fieldChars, conv.getValue(), options, length);
            if (conv.getName().equals("strip-chars"))
                fieldChars = Util.strip(fieldChars, conv.getValue(), options);
            if (conv.getName().equals("substring"))
                fieldChars = Util.substring(fieldChars, conv.getValue(), options);
            if (conv.getName().equals("default-value"))
                fieldChars = Util.defaultValue(fieldChars, conv.getValue(), options);
        }

        if (length > 0) // Never request string to be zero length
            if (fieldChars.length() > length) // too long, chop it off
                fieldChars = fieldChars.substring(0, length);

        return fieldChars;
    }

    /**
     * Facilitates the storage of multiple converters used by the {@code convert} method during processing.
     *
     * @param converter The converter to be added
     */
    public void addConverter(ConverterBO converter) {
        converters.put(converter.getName(), converter);
    }

    /**
     * Return all of the currently registered {@link ConverterBO} instances.
     * @return the currently registered {@link ConverterBO} instances.
     */
    public Collection<ConverterBO> getConverters() {
        return converters.values();
    }

    /**
     * Get the {@link ConverterBO} by name.
     * @param name The name.
     * @return The {@link ConverterBO} instance if found by the {@code name} and {@code null} if not.
     */
    public ConverterBO getConverter(String name) {
        return converters.get(name);
    }

    /**
     * Retrieve the method to use in performing the conversion based upon the {@code converterName}.
     *
     * @param converterName The name of the converter. Used for lookup.
     * @return Java reflection Object used to represent the conversion method.
     */
    private Method getConverterMethod(String converterName) throws FlatwormParserException {
        try {
            ConverterBO c = converters.get(converterName);
            if (converterMethodCache.get(c) != null)
                return converterMethodCache.get(c);
            Method meth;
            Class<?> cl = Class.forName(c.getConverterClass());
            Class args[] = {String.class, Map.class};
            meth = cl.getMethod(c.getMethod(), args);
            converterMethodCache.put(c, meth);
            return meth;
        } catch (NoSuchMethodException e) {
            log.error("Finding method", e);
            throw new FlatwormParserException("Couldn't Find Method");
        } catch (ClassNotFoundException e) {
            log.error("Finding class", e);
            throw new FlatwormParserException("Couldn't Find Class");
        }
    }

    /**
     * Get the method that is reponsible for transorming an {@link Object} to a {@link String}.
     *
     * @param converterName The name of the converter.
     * @return The {@link Method} that can turn an {@link Object} into a {@link String}. The expectation is that this functionality is more
     * advanced than {@code toString()};
     * @throws FlatwormParserException should attempting to retrieving the {@link Method} fail for any reason.
     */
    private Method getToStringConverterMethod(String converterName) throws FlatwormParserException {
        ConverterBO c = converters.get(converterName);
        if (converterToStringMethodCache.get(c) != null)
            return converterToStringMethodCache.get(c);
        try {
            Class<?> cl = Class.forName(c.getConverterClass());
            Class args[] = {Object.class, Map.class};
            Method meth = cl.getMethod(c.getMethod(), args);
            converterToStringMethodCache.put(c, meth);
            return meth;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new FlatwormParserException(e.getMessage(), e);
        }
    }

    /**
     * Fetch the {@link ConverterBO} object read from the config data.
     *
     * @param converterName The name of the converter. Used for lookup.
     * @return An instance of the conversion class.
     * @throws FlatwormParserException if there is no ConverterBO registered with the specified name or the {@link Method} failed be
     *                                 retrieved.
     */
    private Object getConverterObject(String converterName) throws FlatwormParserException {
        try {
            ConverterBO c = converters.get(converterName);
            if (c == null) {
                throw new FlatwormParserException("converterName '" + converterName + "' not registered");
            }
            if (converterObjectCache.get(c.getConverterClass()) != null)
                return converterObjectCache.get(c.getConverterClass());
            Object o;
            Class<?> cl = Class.forName(c.getConverterClass());
            Class args[] = new Class[0];
            Object objArgs[] = new Object[0];
            o = cl.getConstructor(args).newInstance(objArgs);
            converterObjectCache.put(c.getConverterClass(), o);
            return o;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new FlatwormParserException(e.getMessage(), e);
        }
    }
}
