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
import com.blackbear.flatworm.config.impl.DefaultConfigurationReaderImpl;
import com.blackbear.flatworm.errors.FlatwormParserException;
import com.blackbear.flatworm.test.domain.Film;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class EncodingTestCase extends TestCase {
    protected FileFormat ff;

    protected BufferedReader reader;

    private static String layout = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<file-format encoding=\"iso-8859-2\">\r\n"
            + "    <converter name=\"char\" class=\"com.blackbear.flatworm.converters.CoreConverters\" method=\"convertChar\"\r\n"
            + "        return-type=\"java.lang.String\" />\r\n" + "    <record name=\"dvd\">\r\n"
            + "       <record-ident>\r\n"
            + "           <length-ident min-length=\"0\" max-length=\"9999\" />\r\n"
            + "       </record-ident>\r\n" + "       <record-definition>\r\n"
            + "           <bean name=\"film\" class=\"com.blackbear.flatworm.test.domain.Film\" />\r\n" + "           <line>\r\n"
            + "               <record-element length=\"30\" beanref=\"film.title\"\r\n"
            + "                   converter-name=\"char\">\r\n"
            + "                   <conversion-option name=\"justify\" value=\"left\" />\r\n"
            + "               </record-element>\r\n" + "           </line>\r\n"
            + "       </record-definition>\r\n" + "   </record>\r\n" + "</file-format>";

    public EncodingTestCase(String name) {
        super(name);
    }

    protected void setContent(byte[] content) throws Exception {
        ConfigurationReader parser = new DefaultConfigurationReaderImpl();
        ff = getFileFormat(parser);
        reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content),
                ff.getEncoding()));
    }

    protected FileFormat getFileFormat(ConfigurationReader parser) throws Exception {
        InputStream is = new ByteArrayInputStream(layout.getBytes());
        return parser.loadConfigurationFile(is);
    }

    protected Object getNextBean() throws FlatwormParserException, IOException {
        MatchedRecord results = ff.nextRecord(reader);
        return results.getBean(getBeanName());
    }

    protected String getBeanName() {
        return "film";
    }

    public void testNormalString() throws Exception {
        prepareContent("foobar                        ");
        Film film = (Film) getNextBean();
        assertNotNull(film);
        assertEquals("foobar", film.getTitle());
    }

    public void testSpecialChar() throws Exception {
        prepareContent("\u0104                             ");
        Film film = (Film) getNextBean();
        assertNotNull(film);
        assertEquals("\u0104", film.getTitle());
    }

    private void prepareContent(String string) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        OutputStreamWriter sw = new OutputStreamWriter(os, "iso-8859-2");
        sw.write(string);
        sw.flush();
        byte[] bytes = os.toByteArray();
        setContent(bytes);
    }
}
