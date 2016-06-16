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

import com.blackbear.flatworm.errors.FlatwormParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class FTPFileParser extends FileParser {

    private final URL url;

    public FTPFileParser(String config, URL url) throws FlatwormParserException {
        super(config, null);
        this.url = url;
    }

    @Override
    public void open() throws IOException {
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();
        String encoding = fileFormat.getEncoding();
        bufIn = new BufferedReader(new InputStreamReader(in, encoding));

        in.close();
    }

}
