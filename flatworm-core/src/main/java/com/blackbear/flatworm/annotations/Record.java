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
 * com.blackbear.flatworm.config.Identity} record is specified with the {@code apply} flag set to {@code true}, the first one read will be
 * the one that is used. Presently, the order is {@link LengthIdentity} then {@link FieldIdentity}, and then {@link Scriptlet}.
 *
 * @author Alan Henson
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Record {
    String name();

    String encoding() default "UTF-8";

    LengthIdentity lengthIdentity() default @LengthIdentity(minLength = -1, maxLength = -1, apply = false);

    FieldIdentity fieldIdentity() default @FieldIdentity(startPosition = -1, fieldLength = -1, apply = false, matchIdentities = {});

    Scriptlet scriptIdentity() default @Scriptlet;

    Converter[] converters() default {};

    Line[] lines() default { @Line() };

    /**
     * A scriptlet to execute prior to reading/parsing the next record.
     * @return the {@link Scriptlet} configuration.
     */
    Scriptlet beforeReadRecordScript() default @Scriptlet;

    /**
     * A scriptlet to execute after reading/parsing a record.
     * @return the {@link Scriptlet} configuration.
     */
    Scriptlet afterReadRecordScript() default @Scriptlet;
}
