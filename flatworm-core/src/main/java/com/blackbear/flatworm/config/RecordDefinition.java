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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Bean class used to store the values from the Record-Definition XML tag
 */
public class RecordDefinition {
    @Setter
    private Map<String, Bean> beans;

    @Getter
    @Setter
    private List<Line> lines;

    @Getter
    @Setter
    private Record parentRecord;

    public RecordDefinition() {
        this.beans = new HashMap<>();
        this.lines = new ArrayList<>();
    }

    public void addBean(Bean bean) {
        bean.setParentRecordDefinition(this);
        this.beans.put(bean.getBeanName(), bean);
    }

    public Collection<Bean> getBeans() {
        return beans.values();
    }

    public Map<String, Bean> getBeanMap() {
        return Collections.unmodifiableMap(beans);
    }

    public void addLine(Line line) {
        line.setParentRecordDefinition(this);
        lines.add(line);
    }

    @Override
    public String toString() {
        return "RecordDefinition{" +
                "parentRecord=" + parentRecord +
                '}';
    }
}
