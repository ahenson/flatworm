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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.blackbear.flatworm.config.impl.ScriptIdentityImpl;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;
import com.blackbear.flatworm.errors.FlatwormParserException;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URL;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import lombok.Getter;

/**
 * Defines the components of a Scriptlet which allow for external scripts to be run to modify runtime configuration.
 *
 * @author Alan Henson
 */
public class ScriptletBO {

    /**
     * Default Script Engine to use - nashorn.
     */
    public static final String DEFAULT_SCRIPT_ENGINE = "nashorn";
    @Getter
    private String scriptEngineName;

    @Getter
    private String script;

    @Getter
    private String scriptFile;

    private ScriptEngine scriptEngine;

    @Getter
    private String functionName;

    private Invocable scriptInvocable;

    /**
     * Constructor for ScriptletBO. The default Script Engine is used ({@code ScriptletBO.DEFAULT_SCRIPT_ENGINE}) and the default function
     * name {@code ScriptIdentityImp.DEFAULT_SCRIPT_IDENTITY_FUNCTION_NAME} is used.
     */
    public ScriptletBO() throws FlatwormConfigurationException {
        this(DEFAULT_SCRIPT_ENGINE, ScriptIdentityImpl.DEFAULT_SCRIPT_IDENTITY_FUNCTION_NAME);
    }

    /**
     * Constructor for ScriptIdentityImpl that attempts to load the Script Engine specified by name. The default function name ({@code
     * ScriptIdentityImp.DEFAULT_SCRIPT_IDENTITY_FUNCTION_NAME}) is used.
     *
     * @param scriptEngineName The name of the script engine to load. If {@code null} the {@code ScriptletBO.DEFAULT_SCRIPT_ENGINE} is
     *                         used.
     * @throws FlatwormConfigurationException should the script be invalid or should the {@code scriptEngineName} not resolve to a valid
     *                                        script engine.
     */
    public ScriptletBO(String scriptEngineName) throws FlatwormConfigurationException {
        this(scriptEngineName, ScriptIdentityImpl.DEFAULT_SCRIPT_IDENTITY_FUNCTION_NAME);
    }

    /**
     * Constructor for ScriptIdentityImpl that attempts to load the Script Engine specified by name.
     *
     * @param scriptEngineName The name of the script engine to load. If {@code null} the {@code ScriptletBO.DEFAULT_SCRIPT_ENGINE} is
     *                         used.
     * @param functionName     The name of the function in the script that should be executed - the {@link
     *                         com.blackbear.flatworm.FileFormat} instance will be the only parameter sent to the function specified. If the
     *                         {@code functionName} is {@code null} then the {@code ScriptIdentityImp.DEFAULT_SCRIPT_IDENTITY_FUNCTION_NAME} value will be
     *                         used.
     * @throws FlatwormConfigurationException should the script be invalid or should the {@code scriptEngineName} not resolve to a valid
     *                                        script engine.
     */
    public ScriptletBO(String scriptEngineName, String functionName) throws FlatwormConfigurationException {
        this.scriptEngineName = StringUtils.isBlank(scriptEngineName) ? DEFAULT_SCRIPT_ENGINE : scriptEngineName;
        this.functionName = StringUtils.isBlank(functionName) ? ScriptIdentityImpl.DEFAULT_SCRIPT_IDENTITY_FUNCTION_NAME : functionName;

        ScriptEngineManager engineManager = new ScriptEngineManager();
        scriptEngine = engineManager.getEngineByName(this.scriptEngineName);

        if (scriptEngine == null) {
            throw new FlatwormConfigurationException(String.format("The %s ScriptEngine could not be found by the given name.",
                    this.scriptEngineName));
        } else {
            scriptInvocable = (Invocable) scriptEngine;
        }
    }

    /**
     * Set the script snippet to be used in running the {@code matchesIdentity} test.
     *
     * @param script The script that will be executed - should take two parameters with the first being (@link FileFormat} and the second
     *               being a {@link String}, which will be the line of data to examine. The script should return a {@link Boolean}.
     * @throws FlatwormConfigurationException should the script be invalid or should the {@code scriptEngineName} not resolve to a valid
     *                                        script engine.
     */
    public void setScript(String script) throws FlatwormConfigurationException {
        this.script = script;
        try {
            scriptEngine.eval(script);
        } catch (ScriptException e) {
            throw new FlatwormConfigurationException(String.format("The script provided failed to evaluate: %s%n%s",
                    e.getMessage(), script), e);
        }
    }

    /**
     * Set the script file to use where the script to evaluate identity matching is kept.
     *
     * @param scriptFile The script file path (this will be loaded via the classloader.
     * @throws FlatwormConfigurationException should the file not be found, fail to be read, or contain invalid script accordingly the
     *                                        script engine specified.
     */
    public void setScriptFile(String scriptFile) throws FlatwormConfigurationException {
        this.scriptFile = scriptFile;
        try {
            URL url = Resources.getResource(scriptFile);
            String scriptContents = Resources.toString(url, Charsets.UTF_8);
            setScript(scriptContents);
        } catch (IOException e) {
            throw new FlatwormConfigurationException("Failed to load scriptFile: " + scriptFile, e);
        }
    }

    /**
     * Invoke the configured function and return the results.
     * @return the value returned from the invocation of the function if execution was successful.
     * @throws FlatwormParserException should invoking the script fail for any reason.
     */
    public Object invokeFunction() throws FlatwormParserException {
        Object result;
        try {
            result = scriptInvocable.invokeFunction(functionName);
        } catch (Exception e) {
            throw new FlatwormParserException(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Invoke the configured function with the specified parameters and return the results.
     * @param parameters The parameters to send to the script.
     * @return the value returned from the invocation of the function if execution was successful.
     * @throws FlatwormParserException should invoking the script fail for any reason.
     */
    public Object invokeFunction(Object... parameters) throws FlatwormParserException {
        Object result;
        try {
            result = scriptInvocable.invokeFunction(functionName, parameters);
        } catch (Exception e) {
            throw new FlatwormParserException(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public String toString() {
        return "ScriptletBO{" +
                "scriptEngineName='" + scriptEngineName + '\'' +
                ", functionName='" + functionName + '\'' +
                ", scriptFile='" + scriptFile + '\'' +
                ", script='" + script + '\'' +
                '}';
    }
}
