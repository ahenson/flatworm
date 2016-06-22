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

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * BeanBO class used to store the values from the RecordBO-Element XML tag
 */
public class RecordElementBO implements LineElement {

    @Getter
    @Setter
    private Integer fieldEnd;

    @Getter
    @Setter
    private Integer fieldStart;

    @Getter
    @Setter
    private Integer fieldLength;

    @Setter
    @Getter
    private CardinalityBO cardinality;

    @Getter
    @Setter
    private String converterName;

    @Getter
    @Setter
    private LineBO parentLine;

    @Getter
    private Boolean ignoreField;

    @Getter
    @Setter
    private Integer order;

    // The elements are queried, there are just multiple layers of abstraction that the compiler can't see.
    @Getter
    @Setter
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private Map<String, ConversionOptionBO> conversionOptions;

    public RecordElementBO() {
        conversionOptions = new HashMap<>();
        ignoreField = false;
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

    public void addConversionOption(String name, ConversionOptionBO option) {
        conversionOptions.put(name, option);
    }

    public void setIgnoreField(Boolean ignoreField) {
        if(ignoreField != null) {
            this.ignoreField = ignoreField;
        }
        else {
            this.ignoreField = false;
        }
    }

    @Override
    public String toString() {
        return "RecordElementBO{" +
                "fieldEnd=" + fieldEnd +
                ", fieldStart=" + fieldStart +
                ", fieldLength=" + fieldLength +
                ", cardinality='" + cardinality + '\'' +
                ", converterName='" + converterName + '\'' +
                ", ignoreField=" + ignoreField +
                '}';
    }
}