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

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Bean class used to store the values from the Record-Element XML tag
 */

class RecordElement implements LineElement {

    @Getter
    @Setter
    private Integer fieldEnd;

    @Getter
    @Setter
    private Integer fieldStart;

    @Getter
    @Setter
    private Integer fieldLength;

    private Integer spacerLength;

    private char fieldType;

    @Setter
    private String beanRef;

    @Getter
    @Setter
    private String type;

    @Getter
    @Setter
    private Map<String, ConversionOption> conversionOptions;

    public RecordElement() {
        fieldEnd = null;
        fieldStart = null;
        fieldLength = null;
        spacerLength = null;
        fieldType = '\0';
        beanRef = null;
        type = null;
        conversionOptions = new HashMap<>();
    }

    public boolean isFieldStartSet() {
        return fieldStart != null;
    }

    public boolean isFieldEndSet() {
        return fieldEnd != null;
    }

    public boolean isFieldLengthSet() {
        return fieldLength != null;
    }

    public void addConversionOption(String name, ConversionOption option) {
        conversionOptions.put(name, option);
    }

    @Override
    public String getBeanRef() {
        return beanRef;
    }
}