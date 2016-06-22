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

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Used to capture the cardinality configuration of a property within a bean that will be loaded.
 *
 * @author Alan Henson
 */
@Data
@AllArgsConstructor
public class CardinalityBO {
    private String propertyName;
    private String beanRef;
    private String parentBeanRef;
    private String addMethod;

    private Integer minCount;
    private Integer maxCount;
    private CardinalityMode cardinalityMode;
    
    public CardinalityBO() {
        cardinalityMode = CardinalityMode.LOOSE;
        minCount = Integer.MIN_VALUE;
        maxCount = Integer.MAX_VALUE;
    }
    
    
}
