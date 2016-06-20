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

import com.google.common.primitives.Ints;

import com.blackbear.flatworm.config.ConversionOptionBO;
import com.blackbear.flatworm.config.LineToken;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Static final class to hold string manipulation methods
 */
public final class Util {

    private static Pattern numbersOnly = Pattern.compile("[\\D]+");

    private static Pattern lettersOnly = Pattern.compile("[^A-Za-z]+");

    private static Pattern numbersOrLettersOnly = Pattern.compile("[^A-Za-z0-9]+");

    /**
     * <code>split</code> divides a string into many strings based on a delimiter The main difference between this split and the one that
     * comes with Java is this one will ignore delimiters that are within quoted fields <p> <b>NOTE:</b> Delimiter will be ignored once
     * chrQuote is encountered. Consideration will begin once matching chrQuote is encountered </p>
     *
     * @param str      The string you want to split
     * @param chrSplit character you want to split the string on
     * @param chrQuote character you want to be considered to be your quoting character
     * @return List of {@link LineToken} instances representing what was parsed from the line base upon the delimiter.
     */
    public static List<LineToken> split(String str, char chrSplit, char chrQuote) {
        List<LineToken> tokens = new ArrayList<>();
        StringBuilder str1 = new StringBuilder();
        boolean inQuote = false;

        int colIdx = 0;
        int tokenLength = 0;
        for (; colIdx < str.length(); colIdx++) {
            if (str.charAt(colIdx) == chrSplit && !inQuote) {
                tokens.add(new LineToken(str1.toString(), tokenLength, colIdx));
                str1 = new StringBuilder();
                tokenLength = 0;
            } else if (str.charAt(colIdx) == chrQuote) {
                tokenLength++;
                inQuote = (!inQuote);
            } else {
                tokenLength++;
                str1.append(str.charAt(colIdx));
            }
        }

        tokens.add(new LineToken(str1.toString(), tokenLength, colIdx));
        return tokens;
    }

    /**
     * Different from the method in CoreConverters, this one is used for file creation
     *
     * <br> <br> Specified in flatworm XML file like: <code>&lt;conversion-option name="format" value="yyyy-MM-dd"/&gt;</code>
     *
     * @param date              The Date to be converted to a string
     * @param defaultDateFormat provided by FileCreator and used when the date format is not supplied in the Flatworm XML file
     * @param options           collection of ConversionOptions to gather further justification options
     * @return The formatted Date string
     * @throws Exception - if date format is not specified in the XML file or given to FileCreator. At least one must be specified.
     */
    public static String formatDate(Date date, String defaultDateFormat,
                                    Map<String, ConversionOptionBO> options) throws Exception {
        String format = getValue(options, "format");

        // Default format, if none is supplied
        if (null == format)
            if (null != defaultDateFormat)
                format = defaultDateFormat;
            else
                throw new Exception(
                        "You must define a conversion-option with a date format or supply one, I can find neither");

        SimpleDateFormat sdf = new SimpleDateFormat(format);

        return sdf.format(date);
    }

    /**
     * Removes pre-determined characters from string based on Java Patterns
     *
     * <br> <br> Specified in flatworm XML file like: <code>&lt;conversion-option name="justify" value="right"/&gt;</code>
     *
     * @param str     field to be justified
     * @param value   specifies the converterName of justification. Can be ('left'|'right'|'both') - default value is 'both' if not
     *                specified
     * @param options collection of ConversionOptions to gather further justification options
     * @param length  used in file creation to ensure string is padded to the proper length
     * @return padded string
     */
    public static String justify(String str, String value, Map<String, ConversionOptionBO> options,
                                 int length) {

        if (value == null) {
            value = "both";
        }

        boolean justifyLeft = false;
        boolean justifyRight = false;
        if (value.equalsIgnoreCase("left")) {
            justifyLeft = true;
        }
        if (value.equalsIgnoreCase("right")) {
            justifyRight = true;
        }
        if (value.equalsIgnoreCase("both")) {
            justifyLeft = true;
            justifyRight = true;
        }

        String strPadChar = " ";
        String arg = getValue(options, "pad-character");
        if (arg != null) {
            strPadChar = arg;
        }

        // if length is 0, we are removing padding, otherwise, we are adding it
        if (0 == length) {
            // Remove left justification
            if (justifyLeft) {
                int i;
                for (i = str.length() - 1; i > -1 && isPadChar(str.charAt(i), strPadChar); i--)
                    ;
                if (i != str.length() - 1) {
                    str = str.substring(0, i + 1);
                }
            }

            // Remove right justification
            if (justifyRight) {
                int i;
                for (i = 0; i < str.length() && isPadChar(str.charAt(i), strPadChar); i++)
                    ;
                if (i != 0) {
                    str = str.substring(i, str.length());
                }
            }
        } else {
            // pad only with first character
            strPadChar = strPadChar.substring(0, 1);

            if (str.length() < length) {
                // Figure out difference in length to create padding string
                int lenDiff = length - str.length();

                String padding = "";
                for (int i = 0; i < lenDiff; i++)
                    padding = padding + strPadChar;

                if (justifyLeft) {
                    str = str + padding;
                }

                if (justifyRight) {
                    str = padding + str;
                }
            }
        }

        return str;
    }

    private static boolean isPadChar(char c, String strPadChar) {
        return strPadChar.indexOf(c) != -1;
    }

    /**
     * Removes pre-determined characters from string based on Java Patterns
     *
     * <br> <br> Specified in flatworm XML file like: <code>&lt;conversion-option name="strip-chars" value="non-numeric"/&gt;</code>
     *
     * @param str     field to be stripped
     * @param value   converterName of characters to be stripped. Can be ('non-numeric'|'non-alpha'|'non-alphanumeric')
     * @param options collection of ConversionOptions, for future enhancement
     * @return the string stripped of the specified character types
     */
    public static String strip(String str, String value, Map<String, ConversionOptionBO> options) {

        if (value.equalsIgnoreCase("non-numeric")) {
            str = numbersOnly.matcher(str).replaceAll("");
        }
        if (value.equalsIgnoreCase("non-alpha")) {
            str = lettersOnly.matcher(str).replaceAll("");
        }
        if (value.equalsIgnoreCase("non-alphanumeric")) {
            str = numbersOrLettersOnly.matcher(str).replaceAll("");
        }

        return str;
    }

    /**
     * <br> <br> Specified in flatworm XML file like: <code>&lt;conversion-option name="substring" value="1,10"/&gt;</code>
     *
     * @param str     value of field
     * @param value   a string containing the beginning index and the ending index of the desired substring, separated by a comma ','
     * @param options collection of ConversionOptions, for future enhancement
     * @return The specified substring
     */
    public static String substring(String str, String value, Map<String, ConversionOptionBO> options) {

        String[] args = value.split(",");
        str = str.substring(new Integer(args[0]), new Integer(args[1]));

        return str;
    }

    /**
     * If str is length zero (after trimming), value is returned. Default values <b>should not</b> be specified in the flatworm XML file
     * when you want a string of only spaces.
     *
     * @param str     value of field
     * @param value   default value
     * @param options collection of ConversionOptions, for future enhancement
     * @return The string passed in, or the default value if the string is blank
     */
    public static String defaultValue(String str, String value, Map<String, ConversionOptionBO> options) {
        return StringUtils.isBlank(str) ? value : str;
    }

    /**
     * Conversion-Option now stored in class, more than a simple Hashmap lookup
     *
     * @param options The conversion-option values for the field
     * @param key     The key to lookup appropriate ConversionOptionBO
     * @return The string that contains value of the ConversionOptionBO
     */
    public static String getValue(Map<String, ConversionOptionBO> options, String key) {

        if (options.containsKey(key)) {
            return options.get(key).getValue();
        } else {
            return null;
        }
    }

    /**
     * Try to parse using Google's {@code Int.tryParse} method, but make it {@code null} safe.
     *
     * @param value The value to attempt to parse.
     * @return The value as an {@link Integer} if properly formatted as an {@link Integer} or {@code null}.
     */
    public static Integer tryParseInt(String value) {
        Integer result = null;
        if (value != null) {
            result = Ints.tryParse(value);
        }
        return result;
    }

    /**
     * Try parse the {@code value} using {@code java.lang.Boolean::parseBoolean}, but only if {@code value} isn't blank or {@code null}.
     *
     * @param value The value to parse (should be {@code true} or {@code false}.
     * @return the {@link Boolean} result if {@code value} is valid accordingly to {@code java.lang.Boolean::parseBoolean} or {@code null}
     * if {@code value} is {@code null} or blank.
     */
    public static Boolean tryParseBoolean(String value) {
        Boolean result = null;
        if (!StringUtils.isBlank(value)) {
            result = Boolean.parseBoolean(value);
        }
        return result;
    }

    /**
     * Try parse the {@code value} using {@code java.lang.Boolean::parseBoolean}, and if unable return the {@code defaultValue}.
     *
     * @param value The value to parse (should be {@code true} or {@code false}.
     * @param defaultValue The default {@link Boolean} value to return if the {@code value} could not be parsed or is {@code null}.
     * @return the {@link Boolean} result if {@code value} is valid accordingly to {@code java.lang.Boolean::parseBoolean} or the
     * {@code defaultValue} if {@code value} could not be parsed..
     */
    public static Boolean tryParseBoolean(String value, boolean defaultValue) {
        Boolean result = tryParseBoolean(value);
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * See if the field is based upon generics and if so, get the underlying type, else return the declared type of the field.
     * @param field The field to interrogate.
     * @return the type of the field if there are no generics or collections at play or the declared type.
     */
    public static Class<?> getActualFieldType(Field field) {
        Class<?> fieldType = null;
        ParameterizedType paramType;
        if(field.getGenericType() instanceof ParameterizedType) {
            paramType = ParameterizedType.class.cast(field.getGenericType());
            if(paramType.getActualTypeArguments().length > 0) {
                fieldType = (Class<?>) paramType.getActualTypeArguments()[0];
            }
        }
        else {
            fieldType = field.getType();
        }
        return fieldType;
    }

    /**
     * Load all classes within a given package (recursive) that are annotated by the given {@code annotationClass} parameter.
     * @param packageName The package name to search - it will be recusrively searched.
     * @param annotationClass The {@link Annotation} that a class must be annotated by to be included in the results.
     * @return The list of classes found annotated by the {@code annotatedClass} parameter.
     * @throws FlatwormConfigurationException should parsing the classpath fail.
     */
    public static List<Class<?>> findRecordAnnotatedClasses(String packageName, Class<? extends Annotation> annotationClass) 
            throws FlatwormConfigurationException {
        List<Class<?>> discoveredClasses = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            List<File> dirs = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
            for (File directory : dirs) {
                findRecordAnnotatedClasses(directory, packageName, annotationClass, discoveredClasses);
            }
        } catch (FlatwormConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new FlatwormConfigurationException(e.getMessage(), e);
        }

        return discoveredClasses;
    }

    /**
     * For a given {@code directory}, recursively look for all classes that are annotated by the {@code annotationClass} parameter
     * and add them to the {@code classes} list.
     * @param directory The directory to search - should be a classpath path.
     * @param packageName The name of the package to search.
     * @param annotationClass The {@link Annotation} the classes most be annotated by to be included in the results. 
     * @param classes The classes list that will be built up as annotated clases are found.
     * @throws FlatwormConfigurationException should parsing the classpath fail.
     */
    public static void findRecordAnnotatedClasses(File directory, String packageName, Class<? extends Annotation> annotationClass, 
                                            List<Class<?>> classes) throws FlatwormConfigurationException {
        try {
            if (directory.exists()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getName().contains(".")) {
                            findRecordAnnotatedClasses(file, packageName + "." + file.getName(), annotationClass, classes);
                        } else if (!file.isDirectory() && file.getName().endsWith(".class")) {
                            Class<?> clazz = Class.forName(packageName + "." + file.getName().substring(0, file.getName().length() - 6));
                            if (clazz.isAnnotationPresent(annotationClass)) {
                                classes.add(clazz);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new FlatwormConfigurationException(e.getMessage(), e);
        }
    }
}
