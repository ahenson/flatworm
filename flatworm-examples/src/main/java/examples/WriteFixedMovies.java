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

package examples;

import com.blackbear.flatworm.FileCreator;
import com.blackbear.flatworm.errors.FlatwormCreatorException;

import java.io.IOException;
import java.util.Date;

import domain.Book;
import domain.Dvd;
import domain.Film;
import domain.Videotape;

public class WriteFixedMovies {

    public static void main(String[] args) {

        String xmlConfigFile = args[0];
        String outputFile = args[1];

        try {
            FileCreator creator = new FileCreator(xmlConfigFile, outputFile);
            creator.setRecordSeparator("\n");
            creator.open();

            // -----------------------------------------------
            // Create a dvd record - Consists of dvd/film bean
            // -----------------------------------------------

            Dvd dvd = new Dvd();
            Film film = new Film();

            // Populate
            dvd.setDualLayer("true");
            dvd.setPrice(19.95);
            dvd.setSku("873246872");
            film.setReleaseDate(new Date());
            film.setStudio("Studio Ghibli");
            film.setTitle("Spirited Away");

            // Set & write
            creator.setBean("dvd", dvd);
            creator.setBean("film", film);
            creator.write("dvd");

            // -----------------------------------------------
            // Create a videotape record - Consists of video/film bean
            // -----------------------------------------------

            Videotape video = new Videotape();

            // Populate
            video.setPrice(12.50);
            video.setSku("8726347862");

            // Set & write
            // Creator still knows about film object, no need to set it again
            creator.setBean("video", video);
            creator.write("videotape");

            // -----------------------------------------------
            // Create a book record - Just a book bean
            // -----------------------------------------------

            Book book = new Book();

            // Populate
            book.setAuthor("Hayao Miyazaki");
            book.setPrice(24.95);
            book.setReleaseDate(new Date());
            book.setSku("87263487655");
            book.setTitle("Why I made 'Spirited Away'");

            // Set & write
            creator.setBean("book", book);
            creator.write("book");

            // Close buffered output to write contents
            creator.close();

        } catch (FlatwormCreatorException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();

        }
    }

}
