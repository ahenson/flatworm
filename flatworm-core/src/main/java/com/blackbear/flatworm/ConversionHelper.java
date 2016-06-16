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

import com.blackbear.flatworm.errors.FlatwormParserException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

/**
 * The {@code ConversionHelper} was created to separate formatting responsibility into a separate class. This class also makes writing your
 * own converter more of a reality by separating type conversion from string formatting. String formatting has moved to a separate class
 * called Util.
 */
@Slf4j
public class ConversionHelper {
    private Map<String, Converter> converters;

    private Map<Converter, Method> converterMethodCache;

    private Map<Converter, Method> converterToStringMethodCache;

    private Map<String, Object> converterObjectCache;

    public ConversionHelper() {
        converters = new HashMap<>();
        converterMethodCache = new HashMap<>();
        converterToStringMethodCache = new HashMap<>();
        converterObjectCache = new HashMap<>();
    }

    /**
     * @param type       The name of the converter from the xml configuration file
     * @param fieldChars The value of the field as read from the input file
     * @param options    Map of ConversionOptions (if any) for this field
     * @param beanRef    "class.property", used for more descriptive exception messages, should something go wrong
     * @return Java type corresponding to the field type, post conversion
     * @throws FlatwormParserException - if problems are encountered during the conversion process (wraps other exceptions)
     */
    public Object convert(String type, String fieldChars, Map<String, ConversionOption> options,
                          String beanRef) throws FlatwormParserException {

        Object value;

        try {
            Object object = getConverterObject(type);
            Method method = getConverterMethod(type);

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

    public String convert(String type, Object obj, Map<String, ConversionOption> options,
                          String beanRef) throws FlatwormParserException {
        String result;
        try {
            Object converter = getConverterObject(type);
            Method method = getToStringConverterMethod(type);
            Object[] args = {obj, options};
            result = (String) method.invoke(converter, args);
        } catch (Exception e) {
            log.error("While running toString convert method for " + beanRef, e);
            throw new FlatwormParserException("Converting field " + beanRef
                    + " to string for value '" + obj + "'");
        }
        return result;
    }

    /**
     * Handles the processing of the Conversion-Options from the flatworm XML file
     *
     * @param fieldChars The string to be transformed
     * @param options    Collection of ConversionOption objects
     * @param length     Used in justification to ensure proper formatting
     * @return The transformed string
     */
    public String transformString(String fieldChars, Map<String, ConversionOption> options, int length) {
        // JBL - Implement iteration of conversion-options
        // Iterate over conversion-options, that way, the xml file
        // can drive the order of conversions, instead of having them
        // hard-coded like in 'removePadding' (old way)
        Set<String> keys = options.keySet();
        for (String key : keys) {
            ConversionOption conv = options.get(key);

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
     * Facilitates the storage of multiple converters used by the <code>convert</code> method during processing
     *
     * @param converter The converter to be added
     */
    public void addConverter(Converter converter) {
        converters.put(converter.getName(), converter);
    }

    public Converter getConverter(String name) {
        Converter result = null;
        Converter convert = converters.get(name);
        if (convert != null) {
            result = new Converter();
            result.setConverterClass(convert.getConverterClass());
            result.setMethod(convert.getMethod());
            result.setName(convert.getName());
            result.setReturnType(convert.getReturnType());
        }

        return result;
    }

    /**
     * @param type The name of the converter. Used for lookup
     * @return Java reflection Object used to represent the conversion method
     */
    private Method getConverterMethod(String type) throws FlatwormParserException {
        try {
            Converter c = converters.get(type);
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

    private Method getToStringConverterMethod(String type) throws FlatwormParserException {
        Converter c = converters.get(type);
        if (converterToStringMethodCache.get(c) != null)
            return converterToStringMethodCache.get(c);
        try {
            Method meth;
            Class<?> cl = Class.forName(c.getConverterClass());
            Class args[] = {Object.class, Map.class};
            meth = cl.getMethod(c.getMethod(), args);
            converterToStringMethodCache.put(c, meth);
            return meth;
        } catch (NoSuchMethodException e) {
            log.error("Finding method", e);
            throw new FlatwormParserException("Couldn't Find Method 'String " + c.getMethod()
                    + "(Object, HashMap)'");
        } catch (ClassNotFoundException e) {
            log.error("Finding class", e);
            throw new FlatwormParserException("Couldn't Find Class");
        }
    }

    /**
     * @param type The name of the converter. Used for lookup
     * @return An instance of the conversion class
     * @throws FlatwormParserException if there is no Converter registered with the specified name.
     */
    private Object getConverterObject(String type) throws FlatwormParserException {
        try {
            Converter c = converters.get(type);
            if (c == null) {
                throw new FlatwormParserException("type '" + type + "' not registered");
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
        } catch (NoSuchMethodException e) {
            log.error("Finding method", e);
            throw new FlatwormParserException("Couldn't Find Method");
        } catch (IllegalAccessException e) {
            log.error("No access to class", e);
            throw new FlatwormParserException("Couldn't access class");
        } catch (InvocationTargetException e) {
            log.error("Invoking method", e);
            throw new FlatwormParserException("Couldn't invoke method");
        } catch (InstantiationException e) {
            log.error("Instantiating", e);
            throw new FlatwormParserException("Couldn't instantiate converter");
        } catch (ClassNotFoundException e) {
            log.error("Finding class", e);
            throw new FlatwormParserException("Couldn't Find Class");
        }
    }

}
