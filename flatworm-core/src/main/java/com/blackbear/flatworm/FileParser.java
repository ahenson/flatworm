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

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import com.blackbear.flatworm.callbacks.ExceptionCallback;
import com.blackbear.flatworm.callbacks.RecordCallback;
import com.blackbear.flatworm.config.Record;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;
import com.blackbear.flatworm.errors.FlatwormParserException;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Used to read a flat file. Encapsulates parser setup and callback mechanism. This class wraps the functionality that used to be in the
 * main() of the examples. This way, the client knows less about the internal workings of FlatWorm.
 */
@Slf4j
public class FileParser implements Closeable {
    private ListMultimap<String, RecordCallback> recordCallbacks = ArrayListMultimap.create();

    private List<ExceptionCallback> exceptionCallbacks = new ArrayList<>();

    protected File configFile;
    protected File dataInputFile;

    protected String dataInputContent;
    protected String configContent;
    protected FileFormat fileFormat;
    protected BufferedReader bufIn;

    protected FileParser() {
    }

    /**
     * Constructor for FileParser.
     *
     * @param configContent    The config content to parse.
     * @param dataInputContent The content to parse using the provided configuration.
     */
    public FileParser(String configContent, String dataInputContent) {
        this.configContent = configContent;
        this.dataInputContent = dataInputContent;
    }

    /**
     * Constructor for FileParser.
     *
     * @param configFile    The config file to parse.
     * @param dataInputFile The data file to parse using the provided configuration.
     */
    public FileParser(File configFile, File dataInputFile) {
        this.configFile = configFile;
        this.dataInputFile = dataInputFile;
    }

    /**
     * Constructor for FileParser.
     *
     * @param configFile       The config file to parse.
     * @param dataInputContent The content to parse using the provided configuration.
     */
    public FileParser(File configFile, String dataInputContent) {
        this.configFile = configFile;
        this.dataInputContent = dataInputContent;
    }

    /**
     * Constructor for FileParser.
     *
     * @param configContent The config content to parse.
     * @param dataInputFile The data file to parse using the provided configuration.
     */
    public FileParser(String configContent, File dataInputFile) {
        this.configContent = configContent;
        this.dataInputFile = dataInputFile;
    }

    /**
     * Provide a callback object that doesn't require reflection to be invoked. The {@code MatchedRecord} will be passed back to the
     * callback. Add a callback for each record type specified in the configuration file.
     *
     * @param recordName The name of the record.
     * @param callback   The {@link RecordCallback} instance to register.
     */
    public void registerRecordCallback(String recordName, RecordCallback callback) {
        List<RecordCallback> callbacks = recordCallbacks.get(recordName);
        if (!callbacks.contains(callback)) {
            recordCallbacks.put(recordName, callback);
        }
    }

    /**
     * Remove a {@link RecordCallback} that has been registered.
     *
     * @param recordName The name of the {@link Record} for which the {@link RecordCallback} was registered.
     * @param callback   The {@link RecordCallback} instance to remove.
     * @return {@code true} if the {@link RecordCallback} instance was found and removed and {@code false} if it was not found and therefore
     * note removed.
     */
    public boolean removeRecordCallback(String recordName, RecordCallback callback) {
        return recordCallbacks.get(recordName).remove(callback);
    }

    /**
     * Set a callback for exceptions that doesn't require reflection to be invoked. The exception (rather than just the exception type) will
     * be passed to the callback, along with the input line that caused the exception. Should only be invoked once as any subsequent
     * invocations will replace the previous callback.
     *
     * @param callback The {@link ExceptionCallback} instance to register.
     */
    public void registerExceptionCallback(ExceptionCallback callback) {
        if (!exceptionCallbacks.contains(callback)) {
            exceptionCallbacks.add(callback);
        }
    }

    /**
     * Remove a {@link ExceptionCallback} that has been registered.
     *
     * @param callback The {@link ExceptionCallback} instance to remove.
     * @return {@code true} if the {@link ExceptionCallback} instance was found and removed and {@code false} if it was not found and
     * therefore note removed.
     */
    public boolean removeExceptionCallback(ExceptionCallback callback) {
        return exceptionCallbacks.remove(callback);
    }

    /**
     * Parse the specified config information and then parse the file based upon the config information provided. Either the config file or
     * the config content will be parsed and either the data file or the data content will be parsed depending upon which constructor was
     * used.
     *
     * @throws FlatwormConfigurationException should parsing the config file have any issues.
     * @throws IOException                    should the {@link InputStream} fail to properly open.
     */
    public void open() throws FlatwormConfigurationException, IOException {
        Preconditions.checkState((configFile != null || dataInputFile != null)
                        || (!StringUtils.isBlank(configContent) || !StringUtils.isBlank(dataInputContent)),
                "Either the config file or config content must be provided and either the input file or input content must be provided.");

        loadConfiguration();

        InputStream in;
        String encoding;
        if (dataInputFile != null) {
            in = new FileInputStream(dataInputFile);
            encoding = fileFormat.getEncoding();
        } else {
            encoding = StandardCharsets.UTF_8.name();
            in = new ByteArrayInputStream(dataInputContent.getBytes(StandardCharsets.UTF_8));
        }
        bufIn = new BufferedReader(new InputStreamReader(in, encoding));
    }

    /**
     * Load the configuration file content.
     * @throws FlatwormConfigurationException should parsing the content cause any issues.
     */
    protected void loadConfiguration() throws FlatwormConfigurationException {
        try {
            ConfigurationReader parser = new ConfigurationReader();
            if (configFile != null) {
                fileFormat = parser.loadConfigurationFile(configFile);
            } else {
                fileFormat = parser.loadConfigurationFile(new ByteArrayInputStream(configContent.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception ex) {
            throw new FlatwormConfigurationException(ex.getMessage(), ex);
        }
    }

    /**
     * Close the input file.
     *
     * @throws IOException - Should the file system choose to complain about closing an existing file opened for reading.
     */
    @Override
    public void close() throws IOException {
        if (bufIn != null) {
            bufIn.close();
        }
    }

    /**
     * Read the entire input file. This method will call your handler methods, if defined, to handle Records it parses. <br> <br>
     * <b>NOTE:</b> All exceptions are consumed and passed to the exception handler method you defined (The offending line is provided just
     * in case you want to do something with it.
     */
    public void read() {
        Preconditions.checkState(bufIn != null && fileFormat != null, "You must first call open() before calling read().");

        MatchedRecord results = null;
        boolean exception;
        do {
            exception = true;

            // Attempt to parse the next line
            try {
                results = fileFormat.nextRecord(bufIn);
                exception = false;
            } catch (Exception ex) {
                doExceptionCallback(ex, ex.getMessage(), fileFormat.getCurrentParsedLine());
            }

            if (null != results) {
                String recordName = results.getRecordName();
                doCallback(recordName, results);
            }
        }
        while ((null != results) || exception);
    }

    /**
     * Execute all {@link RecordCallback}s for the given record name if any are registered. Exceptions are dumped to the logger vs. causing
     * a disruption and are also sent to the {@code doExceptionCallback} method.
     *
     * @param recordName The name of the {@link Record} - this comes from the configuration file.
     * @param record     The {@link MatchedRecord} instance that was loaded.
     */
    private void doCallback(String recordName, MatchedRecord record) {
        // first check for an old style callback
        if (recordCallbacks.containsKey(recordName)) {
            for (RecordCallback recordCallback : recordCallbacks.get(recordName)) {
                try {
                    recordCallback.processRecord(record);
                } catch (Exception e) {
                    String errMsg = String.format("Failed to invoke callback %s for Record %s: %s",
                            recordCallback.getClass().getName(), recordName, e.getMessage());
                    log.error(errMsg, e);
                    doExceptionCallback(e, errMsg, null);
                }
            }
        }
    }

    /**
     * Execute all {@link ExceptionCallback}s that have been registered. Exceptions are dumped to the logger vs. causing a disruption.
     *
     * @param ex The Exception that occurred.
     */
    private void doExceptionCallback(Exception ex, String message, String lastLine) {
        // Execute all ExceptionCallbacks.
        exceptionCallbacks.forEach(callback -> {
            try {
                callback.processException(ex, message, lastLine);
            } catch (Exception e) {
                log.error(String.format("Failed to execute ExceptionCallback %s for Exception %s and error message %s [line = %s].",
                        callback.getClass().getName(), ex.getClass().getName(), message, lastLine));
            }
        });
    }
}
