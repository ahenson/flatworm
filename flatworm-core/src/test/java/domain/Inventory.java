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
 * Created on Apr 5, 2005
 * 
 * To change the template for this generated file go to Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and
 * Comments
 */
package domain;

/**
 * @author e50633
 *
 *         To change the template for this generated type comment go to Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and
 *         Comments
 */
public class Inventory {

    private String sku;

    private double price;

    private int quantity;

    private String description;

    /**
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return
     */
    public double getPrice() {
        return price;
    }

    /**
     * @return
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * @return
     */
    public String getSku() {
        return sku;
    }

    /**
     * @param string
     */
    public void setDescription(String string) {
        description = string;
    }

    /**
     * @param d
     */
    public void setPrice(double d) {
        price = d;
    }

    /**
     * @param i
     */
    public void setQuantity(int i) {
        quantity = i;
    }

    /**
     * @param string
     */
    public void setSku(String string) {
        sku = string;
    }

}
