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

/**
 * Contract that extends the {@link Identity} interface to indicate that the identity method is specific to line tokens (i.e. fields
 * found within the line of data being parsed vs. characteristics of the entire line itself)..
 *
 * @author Alan Henson
 * @since 2016.1.0.0
 */
public interface LineTokenIdentity extends Identity {

    /**
     * Given a {@link LineToken} determine if it matches the {@link LineTokenIdentity} configuration.
     * @param lineToken The {@link LineToken} instance to evaluate.
     * @return {@code true} if it matches and {@code false} if not.
     */
    boolean matchesIdentity(LineToken lineToken);
}
