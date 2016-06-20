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

import com.blackbear.flatworm.config.BeanBO;
import com.blackbear.flatworm.config.ConfigurationReader;
import com.blackbear.flatworm.config.LineBO;
import com.blackbear.flatworm.config.RecordBO;
import com.blackbear.flatworm.config.RecordDefinitionBO;
import com.blackbear.flatworm.config.impl.DefaultConfigurationReaderImpl;
import com.blackbear.flatworm.config.impl.LengthIdentityImpl;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationReaderTest {
    private FileFormat format;

    public void loadFileFormat(String configFile) {
        ConfigurationReader reader = new DefaultConfigurationReaderImpl();
        try {
            format = reader.loadConfigurationFile(configFile);
        } catch (Exception e) {
            e.printStackTrace();
            fail(String.format("Got an %s - %s", e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    @Test
    public void testComplexRecordsRead() {
        loadFileFormat("complex-example.xml");
        assertNotNull(format);
        RecordBO record = format.getRecord("dvd");
        assertNotNull(record);
        assertDvdRecord(record);
        assertNotNull(format.getRecord("videotape"));
        assertNotNull(format.getRecord("book"));
        assertNull(format.getRecord("cd"));
    }

    @Test
    public void testSegmentRecordsRead() {
        loadFileFormat("segment-example.xml");
        assertNotNull(format);
    }

    private void assertDvdRecord(RecordBO dvd) {
        assertNotNull(dvd.getRecordIdentity());
        assertTrue(dvd.getRecordIdentity() instanceof LengthIdentityImpl);

        LengthIdentityImpl identity = LengthIdentityImpl.class.cast(dvd.getRecordIdentity());

        assertNotNull(identity.getMinLength());
        assertNotNull(identity.getMaxLength());
        assertEquals(85, identity.getMinLength().intValue());
        assertEquals(85, identity.getMaxLength().intValue());
        RecordDefinitionBO def = dvd.getRecordDefinition();
        assertNotNull(def);
        Collection<BeanBO> beans = def.getBeans();
        assertEquals(2, beans.size());

        beans.forEach(Assert::assertNotNull);
        assertTrue(beans.stream()
                .filter(bean -> "dvd".endsWith(bean.getBeanName()))
                .findAny().isPresent());
        assertTrue(beans.stream()
                .filter(bean -> "film".endsWith(bean.getBeanName()))
                .findAny().isPresent());

        List<LineBO> lines = def.getLines();
        assertEquals(1, lines.size());
        LineBO line = lines.get(0);
        assertEquals(6, line.getLineElements().size());
    }
}
