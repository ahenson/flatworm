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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.blackbear.flatworm.converters.ConverterFunctionCache.convertFromString;
import static com.blackbear.flatworm.converters.ConverterFunctionCache.convertToString;
import static com.blackbear.flatworm.converters.ConverterFunctionCache.registerFromTypeConverterFunction;
import static com.blackbear.flatworm.converters.ConverterFunctionCache.registerToTypeConverterFunction;
import static com.blackbear.flatworm.converters.ConverterFunctionCache.removeFromTypeConverterFunction;
import static com.blackbear.flatworm.converters.ConverterFunctionCache.removeToTypeConverterFunction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test to ensure that the ConverterFunctionCache is correctly providing the right converter functions.
 *
 * @author Alan Henson
 * @since 2016.1.0.0
 */
public class ConverterFunctionCacheTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testToTypeFunction() {
        String value = "123.45D";
        Object result;

        try {
            // String value.
            result = convertFromString(String.class, value, Collections.emptyMap());
            assertEquals("Wrong return converterName", String.class, result.getClass());

            // Double value.
            result = convertFromString(Double.class, value, Collections.emptyMap());
            assertEquals("Wrong return converterName", Double.class, result.getClass());

            // BigDecimal value.
            result = convertFromString(BigDecimal.class, value, Collections.emptyMap());
            assertEquals("Wrong return converterName", BigDecimal.class, result.getClass());

            // Float value.
            result = convertFromString(Float.class, value, Collections.emptyMap());
            assertEquals("Wrong return converterName", Float.class, result.getClass());

            // Long value.
            value = "12345";
            result = convertFromString(Long.class, value, Collections.emptyMap());
            assertEquals("Wrong return converterName", Long.class, result.getClass());

            // Integer value.
            value = "12345";
            result = convertFromString(Integer.class, value, Collections.emptyMap());
            assertEquals("Wrong return converterName", Integer.class, result.getClass());

            // Date value.
            value = "06/16/2016";
            Map<String, ConversionOption> options = new HashMap<>(1);
            options.put("format", new ConversionOption("format", "MM/dd/yyyy"));
            result = convertFromString(Date.class, value, options);
            assertEquals("Wrong return converterName", Date.class, result.getClass());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to convert: " + value + ": " + e.getMessage());
        }
    }

    @Test
    public void customToTypes() {
        CoreConverters converters = new CoreConverters();
        registerToTypeConverterFunction(LevelOne.class, converters::convertDouble);

        String value = "123.45D";
        Object result;

        try {
            // Double value.
            result = convertFromString(LevelTwo.class, value, Collections.emptyMap());
            assertEquals("Wrong return converterName", Double.class, result.getClass());

            assertNotNull("Removing a ToType function failed.", removeToTypeConverterFunction(LevelOne.class));
            assertNull("Removing a ToType function didn't do it's job.", removeToTypeConverterFunction(LevelOne.class));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to convert: " + value + ": " + e.getMessage());
        }
    }


    @Test
    public void nullTestConvertFromStringClass() throws Exception {
        thrown.expect(NullPointerException.class);
        convertFromString(null, "123", Collections.emptyMap());
    }

    @Test
    public void nullTestConvertFromStringValue() throws Exception {
        thrown.expect(NullPointerException.class);
        convertFromString(Integer.class, null, Collections.emptyMap());
    }

    @Test
    public void nullTestConvertFromStringOptions() throws Exception {
        thrown.expect(NullPointerException.class);
        convertFromString(Date.class, "06/16/2016", null);
    }

    @Test
    public void testFromTypeFunction() {
        Object value = "123.45D";
        String result;

        try {
            // String value.
            result = convertToString("123", Collections.emptyMap());
            assertEquals("Wrong return converterName", String.class, result.getClass());

            // Double value.
            result = convertToString(123.45D, Collections.emptyMap());
            assertNotNull("Conversion failed", result);
            assertEquals("Wrong return converterName", String.class, result.getClass());

            // BigDecimal value.
            result = convertToString(new BigDecimal("123.45"), Collections.emptyMap());
            assertNotNull("Conversion failed", result);
            assertEquals("Wrong return converterName", String.class, result.getClass());

            // Float value.
            result = convertToString(123.45F, Collections.emptyMap());
            assertNotNull("Conversion failed", result);
            assertEquals("Wrong return converterName", String.class, result.getClass());

            // Long value.
            value = "12345";
            result = convertToString(12345L, Collections.emptyMap());
            assertNotNull("Conversion failed", result);
            assertEquals("Wrong return converterName", String.class, result.getClass());

            // Integer value.
            value = "12345";
            result = convertToString(12345, Collections.emptyMap());
            assertNotNull("Conversion failed", result);
            assertEquals("Wrong return converterName", String.class, result.getClass());

            // Date value.
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 2016);
            cal.set(Calendar.MONTH, 5);
            cal.set(Calendar.DATE, 16);

            Map<String, ConversionOption> options = new HashMap<>(1);
            options.put("format", new ConversionOption("format", "MM/dd/yyyy"));
            result = convertToString(cal.getTime(), options);
            assertNotNull("Conversion failed", result);
            assertEquals("Wrong return converterName", String.class, result.getClass());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to convert: " + value + ": " + e.getMessage());
        }
    }

    @Test
    public void customFromTypes() {
        CoreConverters converters = new CoreConverters();
        registerFromTypeConverterFunction(LevelOne.class, converters::convertDouble);

        String result;

        try {
            // Double value.
            result = convertToString(123.45D, Collections.emptyMap());
            assertEquals("Wrong return converterName", String.class, result.getClass());

            assertNotNull("Removing a FromType function failed.", removeFromTypeConverterFunction(LevelOne.class));
            assertNull("Removing a FromType function didn't do it's job.", removeFromTypeConverterFunction(LevelOne.class));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to convert: LevelTwo: " + e.getMessage());
        }
    }

    @Test
    public void nullTestConvertToStringValue() throws Exception {
        thrown.expect(NullPointerException.class);
        convertToString(null, Collections.emptyMap());
    }

    @Test
    public void nullTestConvertToStringOptions() throws Exception {
        thrown.expect(NullPointerException.class);
        convertToString(new Date(), null);
    }

    public static class LevelOne {

    }

    public static class LevelTwo extends LevelOne {

    }
}
