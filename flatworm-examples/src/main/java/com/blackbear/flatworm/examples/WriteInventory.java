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

package com.blackbear.flatworm.examples;

import com.blackbear.flatworm.FileCreator;
import com.blackbear.flatworm.FileParser;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;
import com.blackbear.flatworm.errors.FlatwormParserException;

import java.io.IOException;

public class WriteInventory {
    public static void main(String[] args) {

        String xmlConfigFileIn = args[0];
        String inputFile = args[1];
        String xmlConfigFileOut = args[2];
        String outputFile = args[3];

        try {
            FileParser parser = new FileParser(xmlConfigFileIn, inputFile);
            FileCreator writer = new FileCreator(xmlConfigFileOut, outputFile);
            writer.setRecordSeparator("\n");

            writer.open();
            parser.open();

            // Instantiate object responsible for handling callbacks
            InventoryHandler handler = new InventoryHandler(writer);

            // set callback methods
            // Args are: bean name (from flatworm xml file), handler object, handler method name
//            parser.setBeanHandler("dvd", handler, "handleDvd");
//            parser.setBeanHandler("videotape", handler, "handleVideotape");
//            parser.setBeanHandler("book", handler, "handleBook");

            // Args are handler object, exception handling method name
//            parser.setExceptionHandler(handler, "handleException");

            parser.read();

            parser.close();
            writer.close();

        }
//        catch (NoSuchMethodException ex)
//        {
//            System.out
//                    .println("NoSuchMethodException: Most likely, you didn't implement (or named incorrectly) the handler methods: "
//                            + ex.getMessage());
//        }
        catch (IOException ex) {
            System.out.println("Something bad happened while opening,reading,closing the input file: " + ex.getMessage());
        } catch (FlatwormParserException ex) {
            System.out.println("Something happened that the parser did not like: " + ex.getMessage());
        } catch (FlatwormConfigurationException ex) {
            System.out.println("Something happened that the creator did not like: " + ex.getMessage());
        }
    }
}
