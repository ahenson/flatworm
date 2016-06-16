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

import com.blackbear.flatworm.errors.FlatwormUnsetFieldValueException;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Bean class used to store the values from the Record-Element XML tag
 */

class RecordElement implements LineElement {

    private Integer fieldEnd;

    private Integer fieldStart;

    private Integer fieldLength;

    private Integer spacerLength;

    private char fieldType;

    private String beanRef;

    @Getter
    @Setter
    private String type;

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

    public int getFieldStart() throws FlatwormUnsetFieldValueException {
        if (fieldStart == null)
            throw new FlatwormUnsetFieldValueException("fieldStart is unset");
        else
            return fieldStart;
    }

    public void setFieldStart(int fieldStart) {
        this.fieldStart = fieldStart;
    }

    public int getFieldEnd() throws FlatwormUnsetFieldValueException {
        if (fieldEnd == null)
            throw new FlatwormUnsetFieldValueException("fieldEnd is unset");
        else
            return fieldEnd;
    }

    public void setFieldEnd(int fieldEnd) {
        this.fieldEnd = fieldEnd;
    }

    public int getFieldLength() throws FlatwormUnsetFieldValueException {
        if (fieldLength == null)
            if (!(isFieldStartSet() && isFieldEndSet()))
                throw new FlatwormUnsetFieldValueException("length is unset");
            else
                // Derive length from start and end position
                return fieldEnd - fieldStart;
        else
            return fieldLength;
    }

    public void setFieldLength(int fieldLength) {
        this.fieldLength = fieldLength;
    }

    public Map<String, ConversionOption> getConversionOptions() {
        return conversionOptions;
    }

    public void setConversionOptions(Map<String, ConversionOption> conversionOptions) {
        this.conversionOptions = conversionOptions;
    }

    public void addConversionOption(String name, ConversionOption option) {
        conversionOptions.put(name, option);
    }

    @Override
    public String getBeanRef() {
        return beanRef;
    }

    public void setBeanRef(String beanRef) {
        this.beanRef = beanRef;
    }
}