/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.AWTEvent;
import java.awt.ActiveEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.MenuComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;

import net.miginfocom.swing.MigLayout;


/**
* Superclass for all dialogs in FreeCol. This class also contains
* methods to create simple dialogs.
*/
public class FreeColDialog<T> extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(FreeColDialog.class.getName());

    // Stores the response from the user:
    private T response = null;

    // Whether or not the user have made the choice.
    private boolean responseGiven = false;

    /**
    * Default constructor.
    */
    /*
    public FreeColDialog() {
        super();
    }
    */

    /**
     * Constructor.
     */
    public FreeColDialog(Canvas parent) {
        super(parent);
    }

    /**
    * Sets the <code>response</code> and wakes up any thread waiting for this information.
    *
    * @param response The object that should be returned by {@link #getResponse}.
    */
    public synchronized void setResponse(T response) {
        this.response = response;
        responseGiven = true;
        logger.info("Response has been set to " + response);
        notifyAll();
    }


    /**
    * Returns the <code>response</code> when set by <code>setResponse(Object response)</code>.
    * Waits the thread until then.
    *
    * @return The object as set by {@link #setResponse}.
    */
    public synchronized T getResponse() {
        // Wait the thread until 'response' is available. Notice that we have to process
        // the events manually if the current thread is the Event Dispatch Thread (EDT).

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                EventQueue theQueue = getToolkit().getSystemEventQueue();

                while (!responseGiven) {
                    // This is essentially the body of EventDispatchThread
                    AWTEvent event = theQueue.getNextEvent();
                    Object src = event.getSource();

                    // Block 'MouseEvent' being sent to other components:
                    /*
                    if (event instanceof MouseEvent) {
                        MouseEvent me = (MouseEvent) event;
                        Component dc = SwingUtilities.getDeepestComponentAt(((ComponentEvent) event).getComponent(), me.getX(), me.getY());
                        // Block only MOUSE_CLICKED and MOUSE_PRESSED
                        if ((event.getId() == MouseEvent.MOUSE_CLICKED || event.getId() == MouseEvent.MOUSE_PRESSED) &&
                                (dc == null || !SwingUtilities.isDescendingFrom(dc, this) || dc.getListeners(MouseListener.class).length == 0)) {
                            continue;
                        }
                    }
                    */

                    // We cannot call theQueue.dispatchEvent, so I pasted its body here:
                    if (event instanceof ActiveEvent) {
                        ((ActiveEvent) event).dispatch();
                    } else if (src instanceof Component) {
                        ((Component) src).dispatchEvent(event);
                    } else if (src instanceof MenuComponent) {
                        ((MenuComponent) src).dispatchEvent(event);
                    } else {
                        logger.warning("unable to dispatch event: " + event);
                    }
                }
            } else {
                while (!responseGiven) {
                    wait();
                }
            }
        } catch(InterruptedException e){}

        T tempResponse = response;
        response = null;
        responseGiven = false;

        return tempResponse;
    }


    /**
    * Convenience method for {@link #getResponse}.
    * @return The response as a <code>boolean</code>.
    * @exception ClassCastException if the response-object
    *       is not of type <code>Boolean</code>.
    */
    public boolean getResponseBoolean() {
        return ((Boolean) getResponse()).booleanValue();
    }


    /**
    * Convenience method for {@link #getResponse}.
    * @return The response as a <code>int</code>.
    * @exception ClassCastException if the response-object
    *       is not of type <code>Integer</code>.
    */
    public int getResponseInt() {
        return ((Integer) getResponse()).intValue();
    }

    /**
     * Sets that no response has been given.
     */
    public void resetResponse() {
        response = null;
        responseGiven = false;
    }

    /**
    * Creates a new <code>FreeColDialog</code> with a text and a cancel-button,
    * in addition to buttons for each of the objects in the given array.
    *
    * @param text The text that explains the choice for the user.
    * @param cancelText The text displayed on the "cancel"-button.
    * @param objects The objects.
    * @return The <code>FreeColDialog</code>.
    * @see ChoiceItem
    */
    public static <T> FreeColDialog<ChoiceItem<T>> createChoiceDialog(String text, String cancelText, 
                                                                      List<ChoiceItem<T>> choices) {

        if (choices.isEmpty()) {
            throw new IllegalArgumentException("Can not create choice dialog with 0 choices!");
        }

        final JButton firstButton;
        firstButton = new JButton(choices.get(0).toString());

        final FreeColDialog<ChoiceItem<T>> choiceDialog =
            new FreeColDialog<ChoiceItem<T>>(FreeCol.getFreeColClient().getCanvas()) {
            public void requestFocus() {
                firstButton.requestFocus();
            }
        };

        enterPressesWhenFocused(firstButton);

        choiceDialog.setLayout(new MigLayout("fillx, wrap 1", "[align center]", ""));
        JTextArea textArea = getDefaultTextArea(text);

        choiceDialog.add(textArea);

        int columns = 1;
             if ((choices.size() % 4) == 0 && choices.size() > 12) columns = 4;
        else if ((choices.size() % 3) == 0 && choices.size() > 6)  columns = 3;
        else if ((choices.size() % 2) == 0 && choices.size() > 4)  columns = 2;
        
        else if (choices.size() > 21) columns = 4;
        else if (choices.size() > 10) columns = 2;
        
        JPanel choicesPanel = new JPanel(new GridLayout(0, columns, 10, 10));
        choicesPanel.setBorder(new CompoundBorder(choicesPanel.getBorder(), 
                                                  new EmptyBorder(10, 20, 10, 20)));

        final ChoiceItem<T> firstObject = choices.get(0);
        firstButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    choiceDialog.setResponse(firstObject);
                }
            });
        choicesPanel.add(firstButton);
        choices.remove(0);

        for (final ChoiceItem<T> object : choices) {
            final JButton objectButton = new JButton(object.toString());
            objectButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    choiceDialog.setResponse(object);
                }
            });
            enterPressesWhenFocused(objectButton);
            choicesPanel.add(objectButton);
        }
        if (choices.size() > 20) {
            JScrollPane scrollPane = new JScrollPane(choicesPanel,
                                                     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            choiceDialog.add(scrollPane, "newline 20");
        } else {
            choicesPanel.setOpaque(false);
            choiceDialog.add(choicesPanel, "newline 20");
        }

        if (cancelText != null) {
            final JButton cancelButton = new JButton();           
            cancelButton.setText(cancelText);
            cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        choiceDialog.setResponse(null);
                    }
                });
            choiceDialog.add(cancelButton, "newline 20, tag cancel");
            choiceDialog.setCancelComponent(cancelButton);
            enterPressesWhenFocused(cancelButton);            
        }

        choiceDialog.setSize(choiceDialog.getPreferredSize());

        return choiceDialog;
    }


    /**
    * Creates a new <code>FreeColDialog</code> with a text and a ok/cancel option.
    * The "ok"-option calls {@link #setResponse setResponse(new Boolean(true))}
    * and the "cancel"-option calls {@link #setResponse setResponse(new Boolean(false))}.
    *
    * @param text The text that explains the choice for the user.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog<Boolean> createConfirmDialog(String text, String okText, String cancelText) {
        return createConfirmDialog(new String[] {text}, null, okText, cancelText);
    }

    public static FreeColDialog<Boolean> createConfirmDialog(String[] texts, ImageIcon[] icons, String okText, String cancelText) {

        // create the OK button early so that the dialog may refer to it
        final JButton  okButton = new JButton(okText);

        // create the dialog
        final FreeColDialog<Boolean> confirmDialog =
            new FreeColDialog<Boolean>(FreeCol.getFreeColClient().getCanvas()) {
            public void requestFocus() {
                okButton.requestFocus();
            }
        };
        confirmDialog.setLayout(new MigLayout("wrap 2", "", ""));

        okButton.addActionListener(new ActionListener() {
                public void actionPerformed( ActionEvent event ) {
                    confirmDialog.setResponse(Boolean.TRUE);
                }
            });

        JButton cancelButton = new JButton(cancelText);
        cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed( ActionEvent event ) {
                    confirmDialog.setResponse(Boolean.FALSE);
                }
            });

        for (int i = 0; i < texts.length; i++) {
            if (icons != null && icons[i] != null) {
                confirmDialog.add(new JLabel(icons[i]));
            } else {
                confirmDialog.add(new JLabel());
            }
            confirmDialog.add(getDefaultTextArea(texts[i]));
        }

        confirmDialog.add(okButton, "newline 20, span, split 2, tag ok");
        confirmDialog.add(cancelButton, "tag cancel");
        confirmDialog.setCancelComponent(cancelButton);
        enterPressesWhenFocused(okButton);
        enterPressesWhenFocused(cancelButton);

        return confirmDialog;
    }

    /**
    * Creates a new <code>FreeColDialog</code> with a text field and a ok/cancel option.
    * The "ok"-option calls {@link #setResponse setResponse(textField.getText())}
    * and the "cancel"-option calls {@link #setResponse setResponse(null)}.
    *
    * @param text The text that explains the action to the user.
    * @param defaultValue The default value appearing in the text field.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog<String> createInputDialog(String text, String defaultValue, String okText, String cancelText) {

        final JTextField input = new JTextField(defaultValue);
        
        final FreeColDialog<String> inputDialog =
            new FreeColDialog<String>(FreeCol.getFreeColClient().getCanvas())  {
            public void requestFocus() {
                input.requestFocus();
            }
        };

        inputDialog.setLayout(new MigLayout("wrap 1, gapy 20", "", ""));

        JButton okButton = new JButton(okText);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(input.getText());
            }
        });

        JButton cancelButton = null;
        if (cancelText != null) {
            cancelButton = new JButton(cancelText);
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    inputDialog.setResponse(null);
                }
            });
        }

        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(input.getText());
            }
        });
        
        input.selectAll();

        inputDialog.add(getDefaultTextArea(text));
        inputDialog.add(input, "width 180:, growx");

        if (cancelButton == null) {
            inputDialog.add(okButton, "tag ok");
            inputDialog.setCancelComponent(okButton);
        } else {
            inputDialog.add(okButton, "split 2, tag ok");
            inputDialog.add(cancelButton, "tag cancel");
            inputDialog.setCancelComponent(cancelButton);
        }

        inputDialog.setSize(inputDialog.getPreferredSize());

        return inputDialog;
    }


    /**
    * Creates a new <code>FreeColDialog</code> in which the user
    * may choose a savegame to load.
    *
    * @param directory The directory to display when choosing the file.
    * @param fileFilters The available file filters in the
    *       dialog.
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog<File> createLoadDialog(File directory, FileFilter[] fileFilters) {
        final FreeColDialog<File> loadDialog =
            new FreeColDialog<File>(FreeCol.getFreeColClient().getCanvas());
        final JFileChooser fileChooser = new JFileChooser(directory);

        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        if (fileFilters.length > 0) {
            for (int i=0; i<fileFilters.length; i++) {
                fileChooser.addChoosableFileFilter(fileFilters[i]);
            }
            fileChooser.setFileFilter(fileFilters[0]);
            fileChooser.setAcceptAllFileFilterUsed(false);
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String actionCommand = event.getActionCommand();
                if (actionCommand.equals(JFileChooser.APPROVE_SELECTION)) {
                    loadDialog.setResponse(fileChooser.getSelectedFile());
                } else if (actionCommand.equals(JFileChooser.CANCEL_SELECTION)) {
                    loadDialog.setResponse(null);
                }
            }
        });
        fileChooser.setFileHidingEnabled(false);
        loadDialog.setLayout(new BorderLayout());
        loadDialog.add(fileChooser);
        loadDialog.setSize(480, 320);

        return loadDialog;
    }


    /**
    * Creates a new <code>FreeColDialog</code> in which the user
    * may choose the destination of the savegame.
    *
    * @param directory The directory to display when choosing the name.
    * @param standardName This extension will be added to the
    *       specified filename (if not added by the user).
    * @param fileFilters The available file filters in the
    *       dialog.
    * @param defaultName Default filename for the savegame.
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog<File> createSaveDialog(File directory, final String standardName, FileFilter[] fileFilters, String defaultName) {
        final FreeColDialog<File> saveDialog =
            new FreeColDialog<File>(FreeCol.getFreeColClient().getCanvas());
        final JFileChooser fileChooser = new JFileChooser(directory);
        final File defaultFile = new File(defaultName);

        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        if (fileFilters.length > 0) {
            for (int i=0; i<fileFilters.length; i++) {
                fileChooser.addChoosableFileFilter(fileFilters[i]);
            }
            fileChooser.setFileFilter(fileFilters[0]);
            fileChooser.setAcceptAllFileFilterUsed(false);
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String actionCommand = event.getActionCommand();
                if (actionCommand.equals(JFileChooser.APPROVE_SELECTION)) {
                    File file = fileChooser.getSelectedFile();
                    if (standardName != null && !file.getName().endsWith(standardName)) {
                        file = new File(file.getAbsolutePath() + standardName);
                    }
                    saveDialog.setResponse(file);
                }
                else if (actionCommand.equals(JFileChooser.CANCEL_SELECTION)) {
                    saveDialog.setResponse(null);
                }
            }
        });
        fileChooser.setFileHidingEnabled(false);
        fileChooser.setSelectedFile(defaultFile);
        saveDialog.setLayout(new BorderLayout());
        saveDialog.add(fileChooser);
        saveDialog.setSize(480, 320);

        return saveDialog;
    }


    /**
    * Returns a filter accepting "*.fsg".
    * @return The filter.
    */
    public static FileFilter getFSGFileFilter() {

        return new FreeColFileFilter( ".fsg", "filter.savedGames" );
    }
    

    /**
    * Returns a filter accepting "*.fgo".
    * @return The filter.
    */
    public static FileFilter getFGOFileFilter() {

        return new FreeColFileFilter( ".fgo", "filter.gameOptions" );
    }
    
    
    /**
    * Returns a filter accepting all files containing a
    * {@link net.sf.freecol.common.model.GameOptions}.
    * That is; both "*.fgo" and "*.fsg".
    * 
    * @return The filter.
    */
    public static FileFilter getGameOptionsFileFilter() {

        return new FreeColFileFilter( ".fgo", ".fsg", "filter.gameOptionsAndSavedGames" );
    }


    static final class FreeColFileFilter extends FileFilter {

        private final String  extension1;
        private final String  extension2;
        private final String  description;

        FreeColFileFilter( String  extension1,
                           String  extension2,
                           String  descriptionMessage ) {

            this.extension1 = extension1;
            this.extension2 = extension2;
            description = Messages.message(descriptionMessage);
        }

        FreeColFileFilter( String  extension, String  descriptionMessage ) {

            this.extension1 = extension;
            this.extension2 = "....";
            description = Messages.message(descriptionMessage);
        }

        public boolean accept(File f) {

            return f.isDirectory() || f.getName().endsWith(extension1)
                || f.getName().endsWith(extension2);
        }

        public String getDescription() {

            return description;
        }
    }

    /**
     * Used for Polymorphism in Recruit, Purchase, Train Dialogs
     */
    public void initialize() {}

}
