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

package com.blackbear.flatworm.config.impl;

import com.blackbear.flatworm.FileFormat;
import com.blackbear.flatworm.config.Record;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;
import com.blackbear.flatworm.errors.FlatwormParserException;

import org.apache.commons.lang.StringUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import lombok.Getter;

/**
 * Manages script identity instances (script-ident) found in the flatworm configuration.
 *
 * @author Alan Henson
 */
public class ScriptIdentity extends AbstractIdentity {
    /**
     * Default JavaScript method name that will be invoked for Script Identity configurations.
     */
    public static final String DEFAULT_SCRIPT_METHOD_NAME = "matchesLine";

    /**
     * Default Script Engine to use - nashorn.
     */
    public static final String DEFAULT_SCRIPT_ENGINE = "nashorn";

    @Getter
    private String scriptEngineName;

    @Getter
    private String script;

    @Getter
    private String methodName;

    private Invocable scriptInvocable;

    /**
     * Constructor for ScriptIdentity. The default Script Engine is used ({@code ScriptIdentity.DEFAULT_SCRIPT_ENGINE}) and the default
     * method name {@code ScriptIdentity.DEFAULT_SCRIPT_METHOD_NAME} is used.
     *
     * @param script The script that will be executed - should take two parameters with the first being (@link FileFormat} and the second
     *               being a {@link String}, which will be the line of data to examine. The script should return a {@link Boolean}.
     */
    public ScriptIdentity(String script) throws FlatwormConfigurationException {
        this(DEFAULT_SCRIPT_ENGINE, script, DEFAULT_SCRIPT_METHOD_NAME);
    }


    /**
     * Constructor for ScriptIdentity that attempts to load the Script Engine specified by name. The default method name ({@code
     * ScriptIdentity.DEFAULT_SCRIPT_METHOD_NAME}) is used.
     *
     * @param scriptEngineName The name of the script engine to load. If {@code null} the {@code ScriptIdentity.DEFAULT_SCRIPT_ENGINE} is
          *                         used.
     * @param script           The script that will be executed - should take two parameters with the first being (@link FileFormat} and the
     *                         second being a {@link String}, which will be the line of data to examine. The script should return a {@link
     *                         Boolean}.
     */
    public ScriptIdentity(String scriptEngineName, String script) throws FlatwormConfigurationException {
        this(scriptEngineName, script, DEFAULT_SCRIPT_METHOD_NAME);
    }

    /**
     * Constructor for ScriptIdentity that attempts to load the Script Engine specified by name.
     *
     * @param scriptEngineName The name of the script engine to load. If {@code null} the {@code ScriptIdentity.DEFAULT_SCRIPT_ENGINE} is
     *                         used.
     * @param script           The script that will be executed - should take two parameters with the first being (@link FileFormat} and the
     *                         second being a {@link String}, which will be the line of data to examine. The script should return a {@link
     *                         Boolean}.
     * @param methodName       The name of the method in the script that should be executed - the {@link com.blackbear.flatworm.FileFormat}
     *                         instance will be the only parameter sent to the function specified. If the {@code methodName} is {@code null}
     *                         then the {@code ScriptIdentity.DEFAULT_SCRIPT_METHOD_NAME} value will be used.
     */
    public ScriptIdentity(String scriptEngineName, String script, String methodName) throws FlatwormConfigurationException {
        this.script = script;
        this.scriptEngineName = StringUtils.isBlank(scriptEngineName) ? DEFAULT_SCRIPT_ENGINE : scriptEngineName;
        this.methodName = StringUtils.isBlank(methodName) ? DEFAULT_SCRIPT_METHOD_NAME : methodName;

        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = engineManager.getEngineByName(this.scriptEngineName);

        if (scriptEngine == null) {
            throw new FlatwormConfigurationException(String.format("The %s ScriptEngine could not be found by the given name.",
                    this.scriptEngineName));
        }
        else {
            scriptInvocable = (Invocable) scriptEngine;
            try {
                scriptEngine.eval(script);
            } catch (ScriptException e) {
                throw new FlatwormConfigurationException(String.format("The script provided failed to evaluate: %s%n%s",
                        e.getMessage(), script), e);
            }
        }
    }

    /**
     * Determine if the given {@link Record} instance should be used to parse the line.
     *
     * @param record     The {@link Record} instance whose {@link ScriptIdentity} instance is being tested.
     * @param fileFormat The {@link FileFormat} instance representing the configuration that is driving the parsing and the last line that
     *                   was read.
     * @param line       The line of data to be evaluated.
     * @return {@code true} if the script determined that the last line should be parsed according to the {@code record}'s configuration and
     * {@code false} if not.
     * @throws FlatwormParserException should the script engine fail to invoke the script or should the return converterName of the script not be a
     *                                 {@code boolean} value.
     */
    @Override
    public boolean doesMatch(Record record, FileFormat fileFormat, String line) throws FlatwormParserException {
        boolean matches = false;
        try {
            Object result = scriptInvocable.invokeFunction(methodName, fileFormat, line);
            if (result instanceof Boolean) {
                matches = Boolean.class.cast(result);
            } else if (result != null) {
                throw new FlatwormParserException(String.format("Record %s has a script identifier that does not return" +
                        "a boolean value - a converterName of %s was returned.", record.getName(), result.getClass().getName()));
            }
        } catch (FlatwormParserException e) {
            throw e;
        } catch (Exception e) {
            throw new FlatwormParserException(e.getMessage(), e);
        }
        return matches;
    }
}
