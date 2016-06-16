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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Bean class used to store the values from the Record-Definition XML tag
 */
@Data
class RecordDefinition {
    private Map<String, Bean> beansUsed;
    private List<Line> lines;

    public RecordDefinition() {
        this.beansUsed = new HashMap<>();
        this.lines = new ArrayList<>();
    }

    public void addBeanUsed(Bean bean) {
        this.beansUsed.put(bean.getBeanName(), bean);
    }

    public void addLine(Line line) {
        lines.add(line);
    }

    public String toString() {
        return super.toString() + "[bean = " + beansUsed + ", lines=" + lines + "]";
    }
}
