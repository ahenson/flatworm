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

import com.blackbear.flatworm.CardinalityMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Data;

@Data
public class SegmentElement implements LineElement {
    private int fieldIdentStart = 0;
    private int fieldIdentLength = 0;
    private List<String> fieldIdentMatchStrings = new ArrayList<>();
    private int minCount;
    private int maxCount;
    private String collectionPropertyName;
    private String beanRef;
    private String parentBeanRef;
    private String addMethod;
    private CardinalityMode cardinalityMode;
    private List<LineElement> elements = new ArrayList<>();

    public void addFieldIdentMatchString(String s) {
        fieldIdentMatchStrings.add(s);
    }

    public boolean matchesId(String id) {
        return fieldIdentMatchStrings.contains(id);
    }

    public char getIdentTypeFlag() {
        return 'F';
    }

    @Override
    public String getBeanRef() {
        return beanRef;
    }

    public void setBeanRef(String beanRef) {
        this.beanRef = beanRef;
    }

    public List<LineElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void addElement(LineElement re) {
        elements.add(re);
    }
}
