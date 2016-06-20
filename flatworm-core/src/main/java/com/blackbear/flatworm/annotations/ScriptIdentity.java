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

package com.blackbear.flatworm.annotations;

import com.blackbear.flatworm.config.impl.ScriptIdentityImpl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides the ability to configure the Script Identity element of the flatworm XML configuration using annotations.
 *
 * @author Alan Henson
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ScriptIdentity {

    /**
     * The Script Engine to use. Java must be able to resolve this. The default is specified here: {@code
     * ScriptIdentityImpl.DEFAULT_SCRIPT_ENGINE}.
     * @return The name of the script engine to use.
     */
    String scriptEngine() default ScriptIdentityImpl.DEFAULT_SCRIPT_ENGINE;

    /**
     * The script snippet to use (vs. loading a file. This will be used over a specified {@code scriptFile}.
     *
     * @return The specified script snippet.
     */
    String script() default "";

    /**
     * The name/path of a script file available on the classpath that contains the script to use. If used then {@code script} must not be
     * used. If a {@code methodName} is used in the script other than the {@code ScriptIdentityImpl.DEFAULT_SCRIPT_METHOD_NAME} method name,
     * then the {@code methodName} must also be specified.
     *
     * @return The name/path to the script file.
     */
    String scriptFile() default "";

    /**
     * The name of the method in the script that takes the {@link com.blackbear.flatworm.FileFormat} and {@code line} ({@link String})
     * parameters. This is only required if different than the {@code ScriptIdentityImpl.DEFAULT_SCRIPT_METHOD_NAME} value.
     *
     * @return The specified method name if other than the default.
     */
    String methodName() default ScriptIdentityImpl.DEFAULT_SCRIPT_METHOD_NAME;

    /**
     * Flag indicating that this identity instance should be used.
     * @return {@code true} if it is to be used and {@code false} if not.
     */
    boolean apply();
}
