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

import com.blackbear.flatworm.FileCreator;
import com.blackbear.flatworm.MatchedRecord;
import com.blackbear.flatworm.errors.FlatwormConfigurationException;

import java.io.IOException;

import com.blackbear.flatworm.test.domain.Book;
import com.blackbear.flatworm.test.domain.Dvd;
import com.blackbear.flatworm.test.domain.Film;
import com.blackbear.flatworm.test.domain.Inventory;
import com.blackbear.flatworm.test.domain.Videotape;

/**
 * @author e50633
 *
 *         To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and
 *         Comments
 */
public class InventoryHandler {

    private FileCreator writer;

    public InventoryHandler(FileCreator writer) {
        this.writer = writer;
    }

    public void handleDvd(MatchedRecord results) {

        Dvd dvd = (Dvd) results.getBean("dvd");
        Film film = (Film) results.getBean("film");

        writeInventory(InventoryFactory.getInventoryFor(dvd, film));
        System.out.println("Processing Dvd...");
    }

    public void handleVideotape(MatchedRecord results) {
        Videotape video = (Videotape) results.getBean("video");
        Film film = (Film) results.getBean("film");

        writeInventory(InventoryFactory.getInventoryFor(video, film));
        System.out.println("Handling VideoTape...");
    }

    public void handleBook(MatchedRecord results) {
        Book book = (Book) results.getBean("book");

        writeInventory(InventoryFactory.getInventoryFor(book));
        System.out.println("Handling Book...");
    }

    public void handleException(String exception, String lastLine) {

        System.out.println("HandlingException\n - " + exception + "\n - " + lastLine);
    }

    // TODO - need to update this.
    private void writeInventory(Inventory inventory) {
        try {
            writer.setBean("inventory", inventory);
            writer.write("inventory");
        } catch (IOException ex) {
            System.out.println("Something bad happend while opening,reading,closing the input file: " + ex.getMessage());
        } catch (FlatwormConfigurationException ex) {
            System.out.println("Something happened that the creator did not like: " + ex.getMessage());
        }
    }
}
