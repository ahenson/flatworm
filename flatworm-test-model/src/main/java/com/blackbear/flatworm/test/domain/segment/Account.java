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

package com.blackbear.flatworm.test.domain.segment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Account {
    private Date reportingDate;
    private String accountCode;
    private Integer accountType;
    private String accountNumber;
    private String serviceType;
    private String companyId;

    private List<Consumer> consumers = new ArrayList<Consumer>();
    private List<Address> addresses = new ArrayList<Address>();

    public Date getReportingDate() {
        return reportingDate;
    }

    public void setReportingDate(Date reportingDate) {
        this.reportingDate = reportingDate;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public Integer getAccountType() {
        return accountType;
    }

    public void setAccountType(Integer accountType) {
        this.accountType = accountType;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public List<Consumer> getConsumers() {
        return Collections.unmodifiableList(consumers);
    }

    public void setConsumers(List<Consumer> consumers) {
        this.consumers.clear();
        this.consumers.addAll(consumers);
    }

    public void addConsumer(Consumer consumer) {
        consumers.add(consumer);
    }

    public List<Address> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses.clear();
    }

    public void addAddress(Address addr) {
        addresses.add(addr);
    }
}
