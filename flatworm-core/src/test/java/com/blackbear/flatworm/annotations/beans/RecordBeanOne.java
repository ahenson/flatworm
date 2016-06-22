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

import com.blackbear.flatworm.annotations.DataIdentity;
import com.blackbear.flatworm.annotations.FieldIdentity;
import com.blackbear.flatworm.annotations.ForProperty;
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
        identity = @DataIdentity(fieldIdentity = @FieldIdentity(
                ignoreCase = true,
                enabled = true,
                matchIdentities = {"TEST"}
        )),
        lines = @Line(
                delimiter = "|"
        ),
        afterReadRecordScript = @Scriptlet(
                scriptEngine = "nashorn",
                scriptFile = "before_after_script_test.js",
                functionName = "modifyRecord",
                enabled = true
        )
)
public class RecordBeanOne {
    @RecordElement(order = 1)
    private String valueOne;

    @RecordElement(order = 2)
    private String valueTwo;

    public RecordBeanOne() {
        this.beanSevens = new ArrayList<>();
    }
    
    @SegmentElement(
            order = 3,
            fieldIdentity = @FieldIdentity(ignoreCase = true, enabled = true, matchIdentities = {"RB2"}))
    private List<RecordBeanTwoChildToOne> beanTwoList = new ArrayList<>();

    @Line(delimiter = ",", forProperty = @ForProperty(enabled = true))
    private RecordBeanFiveChildToOne beanFive;

    @Line(
            forProperty = @ForProperty(
                    enabled = true,
                    identity = @DataIdentity(
                            fieldIdentity = @FieldIdentity(matchIdentities = {"RB7"}, enabled = true)
                    ))
    )
    private List<RecordBeanSevenChildToOne> beanSevens;

    @Line(
            delimiter = "|",
            forProperty = @ForProperty(
                    enabled = true,
                    identity = @DataIdentity(
                            fieldIdentity = @FieldIdentity(matchIdentities = {"RB9"}, enabled = true)
                    ))
    )
    private RecordBeanNineChildToOne beanNine;
}
