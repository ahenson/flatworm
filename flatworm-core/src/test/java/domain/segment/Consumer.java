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

package domain.segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Consumer {
    private Integer consumerNumber;
    private String lastName;
    private String firstName;
    private String middleName;
    private String gender;

    private List<Identity> id = new ArrayList<Identity>();
    private List<Phone> phone = new ArrayList<Phone>();

    public Integer getConsumerNumber() {
        return consumerNumber;
    }

    public void setConsumerNumber(Integer consumerNumber) {
        this.consumerNumber = consumerNumber;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<Identity> getId() {
        return Collections.unmodifiableList(id);
    }

    public void setId(List<Identity> id) {
        this.id.clear();
        this.id.addAll(id);
    }

    public void addId(Identity id) {
        this.id.add(id);
    }

    public List<Phone> getPhone() {
        return Collections.unmodifiableList(phone);
    }

    public void setPhone(List<Phone> phone) {
        this.phone.clear();
        this.phone.addAll(phone);
    }

    public void addPhone(Phone phone) {
        this.phone.add(phone);
    }
}
