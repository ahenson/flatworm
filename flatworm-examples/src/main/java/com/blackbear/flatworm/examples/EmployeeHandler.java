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

/*
 * Created on Feb 21, 2005
 * 
 * To change the template for this generated file go to Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and
 * Comments
 */
package com.blackbear.flatworm.examples;

import com.blackbear.flatworm.MatchedRecord;

import com.blackbear.flatworm.converters.domain.Employee;

/**
 * @author e50633
 *
 *         To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and
 *         Comments
 */
public class EmployeeHandler {

    public void handleNewhire(MatchedRecord results) {
        Employee employee = (Employee) results.getBean("employee");

        System.out.println("Handling Employee\n - " + employee);
    }

    public void handleException(String exception, String lastLine) {

        System.out.println("HandlingException\n - " + exception + "\n - " + lastLine);
    }

}
