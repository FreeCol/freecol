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
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.MenuComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import cz.autel.dmi.HIGLayout;


/**
* Superclass for all dialogs in FreeCol. This class also contains
* methods to create simple dialogs.
*/
public class FreeColDialog extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(FreeColDialog.class.getName());

    // Stores the response from the user:
    private Object response = null;

    // Whether or not the user have made the choice.
    private boolean responseGiven = false;

    /**
    * Default constructor.
    */
    public FreeColDialog() {
        super();
    }

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
    public synchronized void setResponse(Object response) {
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
    public synchronized Object getResponse() {
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

        Object tempResponse = response;
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
    * Creates a new <code>FreeColDialog</code> with a text and an ok-button.
    * The "ok"-option calls {@link #setResponse setResponse(new Boolean(true))}.
    *
    * @param text The text that explains the choice for the user.
    * @param okText The text displayed on the "ok"-button.
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog createInformationDialog(String text, String okText) {
        return createInformationDialog(text, okText, null);
    }

    /**
    * Creates a new <code>FreeColDialog</code> with a text and an ok-button.
    * The "ok"-option calls {@link #setResponse setResponse(new Boolean(true))}.
    *
    * @param text The text that explains the choice for the user.
    * @param okText The text displayed on the "ok"-button.
    * @param image The image to be displayed.
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog createInformationDialog(String text, String okText, ImageIcon image) {
        final JButton okButton = new JButton(okText);
        enterPressesWhenFocused(okButton);

        final FreeColDialog informationDialog = new FreeColDialog() {
            public void requestFocus() {
                okButton.requestFocus();
            }
        };

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                informationDialog.setResponse(new Boolean(true));
            }
        });

        JTextArea textLabel = getDefaultTextArea(text);

        if (image == null) {
            informationDialog.setLayout(new HIGLayout(new int[] {0}, new int[] {0, margin, 0}));
            informationDialog.add(textLabel, higConst.rc(1, 1));
            informationDialog.add(okButton, higConst.rc(3, 1));
        } else {
            informationDialog.setLayout(new HIGLayout(new int[] {0, margin, 0}, new int[] {0, margin, 0}));
            informationDialog.add(new JLabel(image), higConst.rc(1, 1));
            informationDialog.add(textLabel, higConst.rc(1, 3));
            informationDialog.add(okButton, higConst.rcwh(3, 1, 3, 1));
        }

        informationDialog.setSize(informationDialog.getPreferredSize());

        informationDialog.setCancelComponent(okButton);

        return informationDialog;
    }

    /**
    * Returns an information dialog that shows the given 
    * texts and images, and an "OK" button.
    * 
    * @param texts The texts to be displayed in the dialog.
    * @param images The images to be displayed in the dialog.
    * @return An information dialog that shows the given text 
    *       and an "OK" button.
    */
    public static FreeColDialog createInformationDialog(String[] texts, ImageIcon[] images) {

        int[] widths = {0, margin, 0};
        int[] heights = new int[2 * texts.length - 1];
        int imageColumn = 1;
        int textColumn = 3;

        for (int index = 1; index < texts.length; index += 2) {
            heights[index] = margin;
        }

        if (images == null) {
            widths = new int[] {0};
            textColumn = 1;
        }

        final JButton theButton = new JButton(Messages.message("ok"));
        enterPressesWhenFocused(theButton);
        final FreeColDialog informationDialog = new FreeColDialog() {
            public void requestFocus() {
                theButton.requestFocus();
            }
        };

        HIGLayout layout = new HIGLayout(widths, heights);
        JPanel textPanel = new JPanel(layout);
        informationDialog.setLayout(new BorderLayout());

        theButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                informationDialog.setResponse(null);
            }
        });

        int row = 1;
        for (int i = 0; i < texts.length; i++) {
            if (images != null && images[i] != null) {
                textPanel.add(new JLabel(images[i]), higConst.rc(row, imageColumn));
            }
            textPanel.add(getDefaultTextArea(texts[i]), higConst.rc(row, textColumn));
            row += 2;
        }

        JScrollPane scrollPane = new JScrollPane(textPanel,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);

        informationDialog.add(scrollPane, BorderLayout.CENTER);
        informationDialog.add(theButton, BorderLayout.SOUTH);
        informationDialog.setSize(informationDialog.getPreferredSize());

        return informationDialog;
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
    public static FreeColDialog createChoiceDialog(String text, String cancelText, ChoiceItem... objects) {

        if (objects.length == 0) {
            throw new IllegalArgumentException("Can not create choice dialog with 0 choices!");
        }

        final JButton firstButton;
        firstButton = new JButton(objects[0].toString());

        final FreeColDialog choiceDialog = new FreeColDialog() {
            public void requestFocus() {
                firstButton.requestFocus();
            }
        };

        enterPressesWhenFocused(firstButton);

        choiceDialog.setLayout(new BorderLayout(10, 10));
        JTextArea textArea = getDefaultTextArea(text);

        //int height = textArea.getMinimumSize().height + cancelButton.getMinimumSize().height + 40;

        choiceDialog.add(textArea, BorderLayout.NORTH);

        JPanel objectsPanel = new JPanel(new GridLayout(objects.length, 1, 10, 10));
        objectsPanel.setBorder(new CompoundBorder(objectsPanel.getBorder(), 
                                                  new EmptyBorder(10, 20, 10, 20)));

        final Object firstObject = objects[0];
        firstButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    choiceDialog.setResponse(firstObject);
                }
            });
        objectsPanel.add(firstButton);
        //height += firstButton.getMinimumSize().height;

        for (int i = 1; i < objects.length; i++) {
            final Object object = objects[i];
            final JButton objectButton = new JButton(object.toString());
            objectButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    choiceDialog.setResponse(object);
                }
            });
            enterPressesWhenFocused(objectButton);
            objectsPanel.add(objectButton);
            //height += objectButton.getMinimumSize().height;
        }
        if (cancelText != null) {
            final JButton cancelButton = new JButton();
            if (cancelText != null) {
                cancelButton.setText(cancelText);
                enterPressesWhenFocused(cancelButton);
            }
            cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        choiceDialog.setResponse(null);
                    }
                });
            choiceDialog.add(cancelButton, BorderLayout.SOUTH);
            choiceDialog.setCancelComponent(cancelButton);
        }
        JScrollPane scrollPane = new JScrollPane(objectsPanel,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        choiceDialog.add(scrollPane, BorderLayout.CENTER);

        //choiceDialog.setSize(width, height);
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
    public static FreeColDialog createConfirmDialog(String text, String okText, String cancelText) {
        return createConfirmDialog(new String[] {text}, null, okText, cancelText);
    }

    public static FreeColDialog createConfirmDialog(String[] texts, ImageIcon[] images, String okText, String cancelText) {

        // create the OK button early so that the dialog may refer to it
        final JButton  okButton = new JButton();

        // create the dialog
        final FreeColDialog  confirmDialog = new FreeColDialog() {

            public void requestFocus() {

                okButton.requestFocus();
            }
        };
        confirmDialog.setLayout(new HIGLayout(new int[] {0}, new int[] {0, margin, 0}));

        int margin = 10;
        int[] widths = {0, margin, 0};
        int[] heights = new int[2 * texts.length - 1];
        int imageColumn = 1;
        int textColumn = 3;

        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }

        if (images == null) {
            widths = new int[] {0};
            textColumn = 1;
        }

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setColumnWeight(textColumn, 1);
        JPanel mainPanel = new JPanel(layout);

        int row = 1;
        for (int i = 0; i < texts.length; i++) {
            if (images != null && images[i] != null) {
                JLabel image = new JLabel(images[i]);
                mainPanel.add(image, higConst.rc(row, imageColumn));
            }
            mainPanel.add(getDefaultTextArea(texts[i]), higConst.rc(row, textColumn));
            row += 2;
        }

        // decide on some mnemonics for the actions
        char  okButtonMnemonic = '\0';
        char  cancelButtonMnemonic = '\0';
        String  okUpper = okText.toUpperCase();
        String  cancelUpper = cancelText.toUpperCase();
        String  menuMnemonics = "GVORCD";
        for ( int ci = 0, nc = okUpper.length();  ci < nc;  ci ++ ) {
            char  ch = okUpper.charAt(ci);

            // if the character at "ci" in "okText" is not claimed by the menu..
            if ( -1 == menuMnemonics.indexOf(ch) ) {

                okButtonMnemonic = ch;
                break;
            }
        }
        for ( int ci = 0, nc = cancelUpper.length();  ci < nc;  ci ++ ) {
            char  ch = cancelUpper.charAt(ci);

            // if the character at "ci" in "cancelText" is not claimed by the
            // menu nor by okButton..
            if ( -1 == menuMnemonics.indexOf(ch)  &&  ch != okButtonMnemonic ) {

                cancelButtonMnemonic = ch;
                break;
            }
        }

        // build the button actions
        Action  okAction = new AbstractAction( okText ) {

            public void actionPerformed( ActionEvent event ) {

                confirmDialog.setResponse( Boolean.TRUE );
            }
        };
        okAction.putValue( Action.ACCELERATOR_KEY, new Integer(KeyEvent.VK_ENTER) );
        okAction.putValue( Action.MNEMONIC_KEY, new Integer(okButtonMnemonic) );
        okButton.setAction( okAction );

        Action  cancelAction = new AbstractAction( cancelText ) {

            public void actionPerformed( ActionEvent event ) {

                confirmDialog.setResponse( Boolean.FALSE );
            }
        };
        cancelAction.putValue( Action.ACCELERATOR_KEY, new Integer(KeyEvent.VK_ESCAPE) );
        cancelAction.putValue( Action.MNEMONIC_KEY, new Integer(cancelButtonMnemonic) );

        // build the button panel
        JButton cancelButton = new JButton(cancelAction);
        JPanel buttonPanel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
        buttonPanel.setOpaque(false);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        enterPressesWhenFocused(okButton);
        enterPressesWhenFocused(cancelButton);
        
        // finish building the dialog
        confirmDialog.add(mainPanel, higConst.rc(1, 1));
        confirmDialog.add(buttonPanel, higConst.rc(3, 1));
        confirmDialog.setSize(confirmDialog.getPreferredSize());
        confirmDialog.setCancelComponent(cancelButton);

        return confirmDialog;
    }

    /**
    * Creates a dialog that asks the user which player he wants the indians to attack.
    * All the players will be shown as options (not including the current player of course).
    * The possible responses are Player objects that that represent a player that should be
    * attacked.
    *
    * @param allPlayers All players in the game.
    * @param thisUser The current player.
    *
    * @return The FreeColDialog that asks the question to the user.
    */
    public static FreeColDialog createInciteDialog(List<Player> allPlayers, Player thisUser) {
        String mainText = Messages.message("missionarySettlement.inciteQuestion");

        final JTextArea question = getDefaultTextArea(mainText);
        ArrayList<Player> players = new ArrayList<Player>();
        final JButton cancel = new JButton(Messages.message("missionarySettlement.cancel"));

        for (Player p : allPlayers) {
            if (!(p.equals(thisUser) || p.isREF())) {
                players.add(p);
            }
        }

        final FreeColDialog inciteDialog = new FreeColDialog() {
            public void requestFocus() {
                cancel.requestFocus();
            }
        };

        int[] widths = {0};
        int[] heights = new int[2 * players.size() + 3];
        for (int index = 0; index <= players.size(); index++) {
            heights[2 * index + 1] = margin;
        }

        inciteDialog.setLayout(new HIGLayout(widths, heights));

        int textColumn = 1;
        int row = 1;
        inciteDialog.add(question, higConst.rc(row, textColumn));
        row += 2;

        for (final Player p : players) {
            JButton button = new JButton(p.getNationAsString());
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    inciteDialog.setResponse(p);
                }
            });
            inciteDialog.add(button, higConst.rc(row, textColumn));
            row += 2;
        }

        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inciteDialog.setResponse(null);
            }
        });
        inciteDialog.add(cancel, higConst.rc(row, textColumn));

        inciteDialog.setSize(inciteDialog.getPreferredSize());

        return inciteDialog;
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
    public static FreeColDialog createInputDialog(String text, String defaultValue, String okText, String cancelText) {
        final JTextField input = new JTextField(defaultValue);
        
        final FreeColDialog inputDialog = new FreeColDialog()  {
            public void requestFocus() {
                input.requestFocus();
            }
        };

        int[] widths = {0};
        int[] heights = {0, margin, 0, margin, 0};
        inputDialog.setLayout(new HIGLayout(widths, heights));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);

        JButton okButton = new JButton(okText);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(input.getText());
            }
        });
        buttons.add(okButton);
        inputDialog.setCancelComponent(okButton);

        if (cancelText != null) {
            JButton cancelButton = new JButton(cancelText);
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    inputDialog.setResponse(null);
                }
            });
            buttons.add(cancelButton);
            inputDialog.setCancelComponent(cancelButton);
        }

        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(input.getText());
            }
        });
        
        input.selectAll();

        int row = 1;
        int textColumn = 1;

        inputDialog.add(getDefaultTextArea(text), higConst.rc(row, textColumn));
        row += 2;
        inputDialog.add(input, higConst.rc(row, textColumn));
        row += 2;
        inputDialog.add(buttons, higConst.rc(row, textColumn));

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
    public static FreeColDialog createLoadDialog(File directory, FileFilter[] fileFilters) {
        final FreeColDialog loadDialog = new FreeColDialog();
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
    public static FreeColDialog createSaveDialog(File directory, final String standardName, FileFilter[] fileFilters, String defaultName) {
        final FreeColDialog saveDialog = new FreeColDialog();
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
