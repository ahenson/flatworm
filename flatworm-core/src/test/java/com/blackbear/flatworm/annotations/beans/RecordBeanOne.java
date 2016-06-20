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

package com.blackbear.flatworm.annotations.beans;

import com.blackbear.flatworm.annotations.FieldIdentity;
import com.blackbear.flatworm.annotations.Line;
import com.blackbear.flatworm.annotations.Record;
import com.blackbear.flatworm.annotations.RecordElement;
import com.blackbear.flatworm.annotations.Scriptlet;
import com.blackbear.flatworm.annotations.SegmentElement;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * A more complex bean used to test the DefaultAnnotationConfigurationReaderImpl instance.
 *
 * @author Alan Henson
 */
@Data
@Record(
        name = "RecordBeanOne",
        fieldIdentity = @FieldIdentity(
                startPosition = 0,
                fieldLength = 4,
                ignoreCase = true,
                apply = true,
                matchIdentities = {"TEST"}
        ),
        lines = {
                @Line(delimiter = "|"),
                @Line(id = "line2")
        },
        afterReadRecordScript = @Scriptlet(
                scriptEngine = "nashorn",
                scriptFile = "before_after_script_test.js",
                functionName = "modifyRecord",
                apply = true
        )
)
public class RecordBeanOne {

    @RecordElement(order = 1)
    private String valueOne;

    @RecordElement(order = 2)
    private String valueTwo;

    @SegmentElement(
            order = 3,
            fieldIdentity = @FieldIdentity(startPosition = 0, fieldLength = 3, ignoreCase = true, apply = true, matchIdentities = {"RB2"}))
    private List<RecordBeanTwoChildToOne> beanTwoList = new ArrayList<>();

    public void addBeanTwo(RecordBeanTwoChildToOne beanTwo) {
        beanTwoList.add(beanTwo);
    }

    @SegmentElement(
            order = 4,
            lineId = "line2"
    )
    private RecordBeanFiveChildToOne beanFive;
}
