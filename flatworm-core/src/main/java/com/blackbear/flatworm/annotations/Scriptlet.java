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

import com.blackbear.flatworm.config.ScriptletBO;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for capturing the data necessary to perform some action using a JVM-friendly script.
 *
 * @author Alan Henson
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Scriptlet {

    /**
     * The Script Engine to use. Java must be able to resolve this. The default is specified here: {@code
     * ScriptletBO.DEFAULT_SCRIPT_ENGINE}.
     *
     * @return The name of the script engine to use.
     */
    String scriptEngine() default ScriptletBO.DEFAULT_SCRIPT_ENGINE;

    /**
     * The script snippet to use (vs. loading a file). This will be used over a specified {@code scriptFile}.
     *
     * @return The specified script snippet.
     */
    String script() default "";

    /**
     * The name/path of a script file available on the classpath that contains the script to use. If used then {@code script} must not be
     * used. If a {@code functionName} is used in the script other than the {@code ScriptIdentityImp.DEFAULT_SCRIPT_IDENTITY_FUNCTION_NAME}
     * method name, then the {@code functionName} must also be specified.
     *
     * @return The name/path to the script file.
     */
    String scriptFile() default "";

    /**
     * The name of the function in the script to invoke. See specific documentation for the use of this annotation to see what the parameter
     * requirements are.
     *
     * @return The specified function name if other than the default.
     */
    String functionName() default "";

    /**
     * Flag to indicate whether or not this {@code Scriptlet} should be applied - set to {@code false} to have the framework ignore it.
     * Default is {@code false} so that it is not accidentally included.
     *
     * @return {@code true} if the {@code Scriptlet} is to be processed and executed and {@code false} if not.
     */
    boolean apply() default false;
}
