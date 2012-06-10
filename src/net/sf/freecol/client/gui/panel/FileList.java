/**
 *  Copyright (C) 2002-2012   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.client.gui.panel;

import java.io.File;
import java.io.FileFilter;

import javax.swing.JList;


/**
* A list for displaying files.
*/
public class FileList extends JList {

    /**
    * Creates a <code>FileList</code> displaying the contents
    * of the given directory.
    *
    * @param directory The directory to list files from.
    */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public FileList(File directory) {        
        super();       
        setListData(getEntries(directory, getDefaultFileFilter()));        
    }
    
    
    /**
    * Creates a <code>FileList</code> displaying the contents
    * of the given directory.
    *
    * @param directory The directory to list files from.
    * @param fileFilter The filter to apply when displaying the files.
    */    
    @SuppressWarnings("unchecked") // FIXME in Java7
    public FileList(File directory, FileFilter fileFilter) {
        super();        
        setListData(getEntries(directory, fileFilter));
    }
    
    
    private FileListEntry[] getEntries(File directory, FileFilter fileFilter) {
        if (directory == null) {
            throw new NullPointerException();
        }
        
        if (fileFilter == null) {
            throw new NullPointerException();
        }
                
        File[] files = directory.listFiles(fileFilter);
        FileListEntry[] fileListEntries;
        if (files != null) {
            fileListEntries = new FileListEntry[files.length];
        } else {
            fileListEntries = new FileListEntry[0];
        }
        
        for (int i=0; i<fileListEntries.length; i++) {
            fileListEntries[i] = new FileListEntry(files[i]);
        }
        
        return fileListEntries;
    }

    
    /**
     * Gets the default file filter.
     * @return A filter that accepts any file ending with ".fsg".
     */
    public FileFilter getDefaultFileFilter() {
        FileFilter ff = new FileFilter() {
            public boolean accept(File file) {
                String name = file.getName();
                return (name.length() >= 4 && name.substring(name.length()-4).equals(".fsg"));
            }
        };
        
        return ff;
    }

        
    /**
    * A single entry in the <code>FileList</code>.
    */
    public class FileListEntry {
        private File file;
        
        public FileListEntry(File file) {
            this.file = file;
        }
        
        
        /**
        * Gets a string representation of the file.
        */        
        public String toString() {
            String name = file.getName();
            return name.substring(0, name.length()-4);
        }
        
        
        /**
         * Gets the file.
         * @return The <code>File</code>
         */
        public File getFile() {
            return file;
        }
    }
}
