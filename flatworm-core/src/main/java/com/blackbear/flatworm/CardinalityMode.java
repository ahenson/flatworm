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

public enum CardinalityMode {
    /**
     * If the total number of entities read exceeds the max limit then an exception will be thrown.
     */
    STRICT,

    /**
     * If the total number of entities read exceeds the max limit then all entities after the max limit will be ignored.
     */
    RESTRICTED,

    /**
     * All entities read will be captured even if the limit is exceeded.
     */
    LOOSE,

    /**
     * If the bean isn't a collection but is instead a single child object.
     */
    SINGLE,

    /**
     * Default value for the annotation approach to instruct the system that the bean should be inspected to auto-resolve
     * the cardinality. Collections will get a LOOSE setting and single properties will get a SINGLE setting.
     */
    AUTO_RESOLVE
}
