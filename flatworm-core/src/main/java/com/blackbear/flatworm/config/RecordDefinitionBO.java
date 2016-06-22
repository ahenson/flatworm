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

import com.blackbear.flatworm.FileFormat;
import com.blackbear.flatworm.errors.FlatwormParserException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * BeanBO class used to store the values from the RecordBO-Definition XML tag
 */
public class RecordDefinitionBO {
    @Setter
    private Map<String, BeanBO> beans;

    @Getter
    @Setter
    private List<LineBO> lines;

    @Getter
    @Setter
    private List<LineBO> linesWithIdentities;

    @Getter
    @Setter
    private RecordBO parentRecord;

    public RecordDefinitionBO() {
        this.beans = new HashMap<>();
        this.lines = new ArrayList<>();
        this.linesWithIdentities = new ArrayList<>();
    }

    public RecordDefinitionBO(RecordBO parentRecord) {
        this();
        this.parentRecord = parentRecord;
    }

    public void addBean(BeanBO bean) {
        bean.setParentRecordDefinition(this);
        this.beans.put(bean.getBeanName(), bean);
    }

    public Collection<BeanBO> getBeans() {
        return beans.values();
    }

    public Map<String, BeanBO> getBeanMap() {
        return Collections.unmodifiableMap(beans);
    }

    public void addLine(LineBO line) {
        if (line.getLineIdentity() == null) {
            line.setParentRecordDefinition(this);
            
            if(line.getIndex() == -1) {
                line.setIndex(lines.size() + 1);
            }
            
            lines.add(line);
        } else {
            addLineWithIdentity(line);
        }
    }

    public void addLineWithIdentity(LineBO line) {
        if (line.getLineIdentity() != null) {
            line.setParentRecordDefinition(this);
            linesWithIdentities.add(line);
        } else {
            addLine(line);
        }
    }

    /**
     * Determine if one of the {@code LineBO} instances within this {@link RecordDefinitionBO} matches the given line.
     *
     * @param fileFormat the parent {@link FileFormat} instance that contains the master configuration.
     * @param line       the input line from the file being parsed.
     * @return {@code true} if the {@link RecordBO} instance identifies the line and can parse it and {@code false} if not.
     * @throws FlatwormParserException should the matching logic fail for any reason.
     */
    public boolean matchesLine(FileFormat fileFormat, String line) throws FlatwormParserException {
        boolean matchesLine = true;
        for (LineBO lineBO : lines) {
            if (lineBO.getLineIdentity() != null) {
                matchesLine = lineBO.getLineIdentity().matchesIdentity(lineBO, fileFormat, line);
                if (matchesLine) {
                    break;
                }
            }
        }
        return matchesLine;
    }


    @Override
    public String toString() {
        return "RecordDefinitionBO{" +
                "linesCount=" + lines.size() +
                ", linesWithIdentitiesCount=" + linesWithIdentities.size() +
                '}';
    }
}
