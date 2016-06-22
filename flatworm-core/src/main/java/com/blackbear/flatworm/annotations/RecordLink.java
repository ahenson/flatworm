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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mimics a {@code record} configuration within the XML flatworm configuration file. Note that if more than one {@link
 * com.blackbear.flatworm.config.Identity} record is specified with the {@code enabled} flag set to {@code true}, the first one read will be
 * the one that is used. Presently, the order is {@link LengthIdentity} then {@link FieldIdentity}, and then {@link Scriptlet}.
 *
 * @author Alan Henson
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RecordLink {

    /**
     * Specify the {@link Class} that holds the {@link Record} definition that this class is linked to.
     * @return The class that contains the full {@link Record} definition.
     */
    Class recordClass();
}
