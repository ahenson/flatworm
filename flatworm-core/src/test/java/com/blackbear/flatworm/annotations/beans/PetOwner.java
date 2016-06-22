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
import com.blackbear.flatworm.annotations.LengthIdentity;
import com.blackbear.flatworm.annotations.Line;
import com.blackbear.flatworm.annotations.Record;
import com.blackbear.flatworm.annotations.RecordElement;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Simple bean for capturing an owner of a pet.
 *
 * @author Alan Henson
 */
@Data
@Record(
        name = "PetOwner",
        lines = {@Line},
        identity = @DataIdentity(
                lengthIdentity = @LengthIdentity(enabled = true, minLength = 60, maxLength = 60)
        )
)
public class PetOwner {

    @RecordElement(order = 1, length = 20)
    private String firstName;

    @RecordElement(order = 2, length = 20)
    private String lastName;

    @RecordElement(order = 3, length = 20)
    private String city;

    @Line(forProperty = @ForProperty(
            enabled = true,
            identity = @DataIdentity(fieldIdentity = @FieldIdentity(enabled = true, matchIdentities = {"DOG"}))))
    private List<Pet> dogs = new ArrayList<>();

    @Line(forProperty = @ForProperty(
            enabled = true,
            identity = @DataIdentity(fieldIdentity = @FieldIdentity(enabled = true, matchIdentities = {"CAT"}))))
    private List<Pet> cats = new ArrayList<>();
    
    @Line(forProperty = @ForProperty(
            enabled = true,
            isRecordEndLine = true,
            identity = @DataIdentity(fieldIdentity = @FieldIdentity(enabled = true, matchIdentities = {"END"}))))
    private Object end;

}
