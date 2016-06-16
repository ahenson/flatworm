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
import com.blackbear.flatworm.errors.FlatwormConversionException;
import com.blackbear.flatworm.errors.FlatwormInputLineLengthException;
import com.blackbear.flatworm.errors.FlatwormInvalidRecordException;
import com.blackbear.flatworm.errors.FlatwormParserException;
import com.blackbear.flatworm.errors.FlatwormUnsetFieldValueException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to read a flatfile. Encapsulates parser setup and callback mechanism.
 * This class wraps the functionality that used to be in the main() of the
 * examples. This way, the client knows less about the internal workings of
 * FlatWorm.
 */
public class FileParser {
    private static Log log = LogFactory.getLog(FileParser.class);
    private static Class[] METHODSIG = {MatchedRecord.class};
    private static Class[] EXCEPTIONSIG = {String.class, String.class};
    private static String EXCEPTIONS = "exception";

    private Map<String, Callback> callbacks = new HashMap<String, Callback>();
    // This map provides access to callback objects, rather than methods as with
    // the older callbacks Map.
    // It is intended to eventually replace that mechanism
    private Map<String, RecordCallback> recordCallbacks = new HashMap<String, RecordCallback>();
    // Provide a single callback object for exceptions. This is not stored in the
    // callbacks map because exception handling
    // is inherently different than record processing and the callback signature
    // is therefore different
    private ExceptionCallback exceptionCallback;
    private String file;
    protected FileFormat ff;
    protected BufferedReader bufIn = null;

    /**
     * Constructor for FileParser<br>
     *
     * @param config
     *          full path to the FlatWorm XML configuration file
     * @param file
     *          full path to input file
     * @throws FlatwormParserException
     *           - wraps FlatwormConfigurationValueException &
     *           FlatwormUnsetFieldValueException (to reduce number of exceptions
     *           clients have to be aware of)
     */
    public FileParser(String config, String file) throws FlatwormParserException {
        this.file = file;

        try {

            ConfigurationReader parser = new ConfigurationReader();
            ff = parser.loadConfigurationFile(config);

        } catch (FlatwormConfigurationValueException ex) {
            throw new FlatwormParserException(ex.getMessage());
        } catch (FlatwormUnsetFieldValueException ex) {
            throw new FlatwormParserException(ex.getMessage());
        }
    }

    /**
     * Provide a callback object that doesn't require reflection to be invoked.
     * The <code>MatchedRecord</code> will be passed back to the callback. Add a
     * callback for each record type specified in the configuration file.
     *
     * @since 2.0
     */
    public void addRecordCallback(String recordName, RecordCallback callback) {
        recordCallbacks.put(recordName, callback);
    }

    /**
     * Set a callback for exceptions that doesn't require reflection to be
     * invoked. The exception (rather than just the exception type) will be passed
     * to the callback, along with the input line that caused the exception.
     * Should only be invoked once as any subsequent invocations will replace the
     * previous callback.
     *
     * @since 2.0
     */
    public void setExceptionCallback(ExceptionCallback callback) {
        exceptionCallback = callback;
    }

    /**
     * Open the buffered reader for the input file<br>
     *
     * @throws FileNotFoundException
     *           - If the file you supplied does not happen to exist.
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public void open() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        InputStream in = new FileInputStream(file);
        String encoding = ff.getEncoding();
        bufIn = new BufferedReader(new InputStreamReader(in, encoding));

    }

    /**
     * Close the input file<br>
     *
     * @throws IOException
     *           - Should the file system choose to complain about closing an
     *           existing file opened for reading.
     */
    public void close() throws IOException {
        if (bufIn != null) {
            bufIn.close();
        }
    }

    /**
     * Read the entire input file. This method will call your handler methods, if
     * defined, to handle Records it parses. <br>
     * <br>
     * <b>NOTE:</b> All exceptions are consumed and passed to the exception
     * handler method you defined (The offending line is provided just in case you
     * want to do something with it.<br>
     *
     * @throws FlatwormConfigurationValueException
     */
    public void read() {

        MatchedRecord results = null;
        boolean exception = false;
        do {
            exception = true;

            // Attempt to parse the next line
            try {
                results = ff.getNextRecord(bufIn);
                exception = false;
            } catch (FlatwormInvalidRecordException ex) {
                doExceptionCallback(ex, "FlatwormInvalidRecordException", ff.getLastLine());
            } catch (FlatwormInputLineLengthException ex) {
                log.warn("Exception", ex);
                doExceptionCallback(ex, "FlatwormInputLineLengthException", ff.getLastLine());
            } catch (FlatwormUnsetFieldValueException ex) {
                doExceptionCallback(ex, "FlatwormUnsetFieldValueException", ff.getLastLine());
            } catch (FlatwormConversionException ex) {
                doExceptionCallback(ex, "FlatwormConversionException", ff.getLastLine());
            } catch (Exception ex) {
                doExceptionCallback(ex, ex.getMessage(), ff.getLastLine());
            }

            if (null != results) {
                String recordName = results.getRecordName();
                doCallback(recordName, results);
            }
        }
        while ((null != results) || exception);
    }

    /**
     * Encapsulated details about calling client's handler methods (for exceptions
     * too)
     *
     * @param callback
     *          The Callback object to be invoked
     * @param arg1
     *          first argument for callback - used for record handlers and
     *          exceptions
     * @param arg2
     *          second argument for callback - used for only for exceptions.
     *          Contains a string that contains the offending line from the input
     *          file <br>
     * <br>
     *          <b>NOTE:</b> All exceptions are consumed and passed to the
     *          exception handler method you defined (The offending line is
     *          provided just in case you want to do something with it.<br>
     * @throws FlatwormConfigurationValueException
     */
    private void doCallback(Callback callback, Object arg1, Object arg2) {
        try {
            Method method = callback.getMethod();
            Object[] args = null;

            if (null == arg2) {
                args = new Object[1];
                args[0] = arg1;
            } else {
                args = new Object[2];
                args[0] = arg1;
                args[1] = arg2;
            }

            method.invoke(callback.getInstance(), args);
        } catch (Exception ex) {
            // Something happened during the method call
            String details = callback.getInstance().getClass().getName() + "."
                    + callback.getMethod().getName();
            log.error("Bad handler method call: " + details + " - " + ex);
        }
    }

    private void doCallback(String recordType, MatchedRecord record) {
        // first check for an old style callback
        Callback oldType = callbacks.get(recordType);
        if (oldType != null) {
            doCallback(oldType, record, null);
        } else {
            RecordCallback callback = recordCallbacks.get(recordType);
            if (callback != null) {
                callback.processRecord(record);
            }
        }
    }

    private void doExceptionCallback(Exception ex, String message, String lastLine) {
        // first check for an old style callback
        Callback oldType = callbacks.get(EXCEPTIONS);
        if (oldType != null) {
            doCallback(oldType, message, lastLine);
        } else {
            if (exceptionCallback == null) {
                throw new RuntimeException("No callback specified for Exceptions. Exception occurred: "
                        + ex);
            }
            exceptionCallback.processException(ex, lastLine);
        }
    }
}
