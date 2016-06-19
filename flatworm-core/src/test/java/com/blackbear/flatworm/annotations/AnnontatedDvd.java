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

package com.blackbear.flatworm.annotations;

import lombok.Data;

/**
 * Class description goes here.
 *
 * @author Alan Henson
 */
@Data
@Record(fieldIdentity =
    @FieldIdentity(
        fieldStartPosition = 0,
        fieldLength = 3,
        apply = true,
        stringMatchIdentities = { @
                StringMatch(matchString = "DVD", ignoreCase = true) }))
public class AnnontatedDvd {

    @RecordElement(order = 1, length = 30)
    private String sku;

    @RecordElement(order = 2, length = 30)
    private String dualLayer;

    @RecordElement(order = 3, length = 30)
    private double price;

    public String toString() {
        return super.toString() + "[" + sku + ", " + price + ", " + dualLayer + "]";
    }
}
