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

import com.blackbear.flatworm.config.ConfigurationReader;
import com.blackbear.flatworm.config.impl.DefaultConfigurationReader;
import com.blackbear.flatworm.test.domain.Dvd;
import com.blackbear.flatworm.test.domain.Film;
import com.blackbear.flatworm.test.domain.Header;
import com.blackbear.flatworm.test.domain.Videotape;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class UnmappedRecords {
    @Test
    public void testFileRead() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
        ConfigurationReader parser = new DefaultConfigurationReader();
        try {
            FileFormat ff = parser.loadConfigurationFile(getClass().getClassLoader().getResourceAsStream("unmapped-records.xml"));
            InputStream in = getClass().getClassLoader().getResourceAsStream("unmapped_records.txt");
            BufferedReader bufIn = new BufferedReader(new InputStreamReader(in));
            MatchedRecord results;

            results = ff.nextRecord(bufIn);
            assertEquals("header", results.getRecordName());
            Header header = (Header) results.getBean("header");
            assertEquals("IMDB", header.getSource());
            assertEquals(2016.1, header.getVersion(), 0.001);

            results = ff.nextRecord(bufIn);
            assertEquals("dvd", results.getRecordName());
            Dvd dvd = (Dvd) results.getBean("dvd");
            Film film = (Film) results.getBean("film");
            assertEquals("55512121", dvd.getSku());
            assertEquals(49.95, dvd.getPrice(), 0.01);
            assertEquals("Y", dvd.getDualLayer());
            assertEquals("2004/01/15", format.format(film.getReleaseDate()));
            assertEquals("DIAL J FOR JAVA", film.getTitle());
            assertEquals("RUN ANYWHERE STUDIO", film.getStudio());

            results = ff.nextRecord(bufIn);
            assertEquals("videotape", results.getRecordName());
            Videotape tape = (Videotape) results.getBean("video");
            film = (Film) results.getBean("film");
            assertEquals("2346542", tape.getSku());
            assertEquals(23.55, tape.getPrice(), 0.01);
            assertEquals("2003/03/12", format.format(film.getReleaseDate()));
            assertEquals("WHEN A STRANGER IMPLEMENTS", film.getTitle());
            assertEquals("NULL POINTER PRODUCTIONS", film.getStudio());

            // This should not do anything as we are ignoring unmatched lines.
            results = ff.nextRecord(bufIn);
            assertNull(results);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Caught an exception of type " + e.getClass().getSimpleName());
        }
    }

}
