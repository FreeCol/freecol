
package net.sf.freecol.client.gui.panel;

import javax.swing.*;
import java.io.*;


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
    * This filter accepts any file ending with ".fsg".
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
        */
        public File getFile() {
            return file;
        }
    }
}
