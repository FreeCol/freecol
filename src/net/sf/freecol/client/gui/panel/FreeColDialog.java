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
import java.util.Vector;
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
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;
import net.sf.freecol.common.model.UnitType;


/**
* Superclass for all dialogs in FreeCol. This class also contains
* methods to create simple dialogs.
*/
public class FreeColDialog extends FreeColPanel {
    private static final Logger logger = Logger.getLogger(FreeColDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int SCOUT_INDIAN_SETTLEMENT_CANCEL = 0,
                            SCOUT_INDIAN_SETTLEMENT_SPEAK = 1,
                            SCOUT_INDIAN_SETTLEMENT_TRIBUTE = 2,
                            SCOUT_INDIAN_SETTLEMENT_ATTACK = 3;

    public static final int SCOUT_FOREIGN_COLONY_CANCEL = 0,
                            SCOUT_FOREIGN_COLONY_NEGOTIATE = 1,
                            SCOUT_FOREIGN_COLONY_SPY = 2,
                            SCOUT_FOREIGN_COLONY_ATTACK = 3;

    public static final int MISSIONARY_CANCEL = 0,
                            MISSIONARY_ESTABLISH = 1,
                            MISSIONARY_DENOUNCE_AS_HERESY = 2,
                            MISSIONARY_INCITE_INDIANS = 3;

    // Stores the response from the user:
    private Object response = null;

    // Wether or not the user have made the choice.
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

                    // Block 'MouseEvent' beeing sent to other components:
                    /*
                    if (event instanceof MouseEvent) {
                        MouseEvent me = (MouseEvent) event;
                        Component dc = SwingUtilities.getDeepestComponentAt(((ComponentEvent) event).getComponent(), me.getX(), me.getY());
                        // Block only MOUSE_CLICKED and MOUSE_PRESSED
                        if ((event.getID() == MouseEvent.MOUSE_CLICKED || event.getID() == MouseEvent.MOUSE_PRESSED) &&
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
        int[] heights = new int[2 * texts.length + 1];
        int imageColumn = 1;
        int textColumn = 3;

        for (int index = 0; index < texts.length; index++) {
            heights[2 * index + 1] = margin;
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
        informationDialog.setLayout(layout);

        theButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                informationDialog.setResponse(null);
            }
        });

        int row = 1;
        for (int i = 0; i < texts.length; i++) {
            if (images != null && images[i] != null) {
                informationDialog.add(new JLabel(images[i]), higConst.rc(row, imageColumn));
            }
            informationDialog.add(getDefaultTextArea(texts[i]), higConst.rc(row, textColumn));
            row += 2;
        }

        informationDialog.add(theButton, higConst.rcwh(row, imageColumn, widths.length, 1));
        informationDialog.setSize(informationDialog.getPreferredSize());

        return informationDialog;
    }


    public static FreeColDialog createPreCombatDialog(Unit attacker, Unit defender,
                                                      Settlement settlement, Canvas parent) {

        ArrayList<Modifier> offense = Unit.getOffensiveModifiers(attacker, defender);
        ArrayList<Modifier> defense = new ArrayList<Modifier>();
        if (defender == null && settlement != null) {
            Modifier settlementModifier = Unit.getSettlementModifier(attacker, settlement);
            defense.add(new Modifier("modifiers.baseDefense", Float.MIN_VALUE, Modifier.ADDITIVE));
            defense.add(settlementModifier);
            defense.add(new Modifier("modifiers.finalResult", Float.MIN_VALUE, Modifier.ADDITIVE));
        } else {
            defense = Unit.getDefensiveModifiers(attacker, defender);
        }

        int numberOfModifiers = Math.max(offense.size(), defense.size());
        int extraRows = 3; // title, icon, buttons
        int numberOfRows = 2 * (numberOfModifiers + extraRows) - 1;

        int[] widths = {-6, 20, -8, 0, 40, -1, 20, -3, 0};
        int[] heights = new int[numberOfRows];
        int offenseLabelColumn = 1;
        int offenseValueColumn = 3;
        int offensePercentageColumn = 4;
        int defenseLabelColumn = 6;
        int defenseValueColumn = 8;
        int defensePercentageColumn = 9;

        for (int index = 1; index < numberOfRows; index += 2) {
            heights[index] = margin;
        }


        final JButton okButton = new JButton();
        final FreeColDialog preCombatDialog = new FreeColDialog() {
            public void requestFocus() {
                okButton.requestFocus();
            }
        };

        Action okAction = new AbstractAction(Messages.message("ok")) {
            public void actionPerformed( ActionEvent event ) {
                preCombatDialog.setResponse( Boolean.TRUE );
            }
        };
        okButton.setAction(okAction);

        Action  cancelAction = new AbstractAction(Messages.message("cancel")) {
            public void actionPerformed( ActionEvent event ) {
                preCombatDialog.setResponse( Boolean.FALSE );
            }
        };
        final JButton cancelButton = new JButton(cancelAction);

        enterPressesWhenFocused(okButton);
        enterPressesWhenFocused(cancelButton);
        
        preCombatDialog.setLayout(new HIGLayout(widths, heights));

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                preCombatDialog.setResponse(false);
            }
        });

        Modifier modifier;
        int row = 1;

        // left hand side: attacker
        String attackerName = Messages.message("model.unit.nationUnit",
                    "%nation%", attacker.getOwner().getNationAsString(),
                    "%unit%", attacker.getName());
        preCombatDialog.add(new JLabel(attackerName),
                            higConst.rc(row, offenseLabelColumn));
        row += 2;
        preCombatDialog.add(new UnitLabel(attacker, parent, false, true),
                            higConst.rcwh(row, offenseLabelColumn, 3, 1));
        row += 2;
        for (int index = 0; index < offense.size() - 1; index++) {
            modifier = offense.get(index);
            preCombatDialog.add(new JLabel(Messages.message(modifier.getId())), 
                                higConst.rc(row, offenseLabelColumn));
            String value = String.valueOf(modifier.getValue());
            if (modifier.getType() == Modifier.PERCENTAGE) {
                if (modifier.getValue() > 0) {
                    value = "+" + value;
                }
                preCombatDialog.add(new JLabel("%"), higConst.rc(row, offensePercentageColumn));
            }                
            preCombatDialog.add(new JLabel(value), higConst.rc(row, offenseValueColumn, "r"));
            row += 2;
        }
        int finalResultRow = row;

        row = 1;
        // right hand side: defender
        if (defender != null) {
            String defenderName = Messages.message("model.unit.nationUnit",
                        "%nation%", defender.getOwner().getNationAsString(),
                        "%unit%", defender.getName());
            preCombatDialog.add(new JLabel(defenderName),
                                higConst.rc(row, defenseLabelColumn));
            row += 2;
            preCombatDialog.add(new UnitLabel(defender, parent, false, true),
                                higConst.rcwh(row, defenseLabelColumn, 3, 1));
            row += 2;
        } else {
            String defenderName;
            if (settlement instanceof Colony) {
                defenderName = ((Colony) settlement).getName();
            } else {
                defenderName = Messages.message("indianSettlement", 
                            "%nation%", settlement.getOwner().getNationAsString());
            }
            preCombatDialog.add(new JLabel(defenderName),
                                higConst.rc(row, defenseLabelColumn));
            row += 2;
            preCombatDialog.add(new JLabel(parent.getImageIcon(settlement, false)),
                                higConst.rcwh(row, defenseLabelColumn, 3, 1));
            row += 2;
        }

        for (int index = 0; index < defense.size() - 1; index++) {
            modifier = defense.get(index);
            preCombatDialog.add(new JLabel(Messages.message(modifier.getId())), 
                                higConst.rc(row, defenseLabelColumn));
            String value = String.valueOf(modifier.getValue());
            if (modifier.getValue() == Float.MIN_VALUE) {
                value = "?";
            } else if (modifier.getType() == Modifier.PERCENTAGE) {
                if (modifier.getValue() > 0) {
                    value = "+" + value;
                }
                preCombatDialog.add(new JLabel("%"), higConst.rc(row, defensePercentageColumn));
            }                
            preCombatDialog.add(new JLabel(value), higConst.rc(row, defenseValueColumn, "r"));
            row += 2;
        }
        if (row < finalResultRow) {
            row = finalResultRow;
        }

        Font bigFont = preCombatDialog.getFont().deriveFont(Font.BOLD, 20f);
        modifier = offense.get(offense.size() - 1);
        JLabel finalOffenseLabel = new JLabel(Messages.message(modifier.getId()));
        finalOffenseLabel.setFont(bigFont);
        preCombatDialog.add(finalOffenseLabel,
                            higConst.rc(row, offenseLabelColumn));
        JLabel finalOffenseResult = new JLabel(String.valueOf(modifier.getValue()));
        finalOffenseResult.setFont(bigFont);
        preCombatDialog.add(finalOffenseResult,
                            higConst.rc(row, offenseValueColumn, "r"));

        modifier = defense.get(defense.size() - 1);
        JLabel finalDefenseLabel = new JLabel(Messages.message(modifier.getId()));
        finalDefenseLabel.setFont(bigFont);
        preCombatDialog.add(finalDefenseLabel,
                            higConst.rc(row, defenseLabelColumn));
        JLabel finalDefenseResult = new JLabel(String.valueOf(modifier.getValue()));
        if (modifier.getValue() == Float.MIN_VALUE) {
            finalDefenseResult.setText("?");
        }
        finalDefenseResult.setFont(bigFont);
        preCombatDialog.add(finalDefenseResult,
                            higConst.rc(row, defenseValueColumn, "r"));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        preCombatDialog.add(buttonPanel, 
                            higConst.rcwh(heights.length, 1, widths.length, 1));
        preCombatDialog.setSize(preCombatDialog.getPreferredSize());

        return preCombatDialog;
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
    public static FreeColDialog createChoiceDialog(String text, String cancelText, Object[] objects) {
        final JButton cancelButton = new JButton(cancelText);

        final JButton firstButton;
        if (objects.length > 0) {
            firstButton = new JButton(objects[0].toString());
        } else {
            firstButton = cancelButton;
        }
        final FreeColDialog choiceDialog = new FreeColDialog() {
            public void requestFocus() {
                firstButton.requestFocus();
            }
        };

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                choiceDialog.setResponse(null);
            }
        });
        
        enterPressesWhenFocused(firstButton);
        enterPressesWhenFocused(cancelButton);

        choiceDialog.setLayout(new BorderLayout());
        JLabel l = new JLabel(text, JLabel.CENTER);

        int height = l.getMinimumSize().height + cancelButton.getMinimumSize().height + 40;

        choiceDialog.add(l, BorderLayout.NORTH);

        JPanel objectsPanel = new JPanel(new GridLayout(objects.length+1, 1, 10,10));
        objectsPanel.setBorder(new CompoundBorder(objectsPanel.getBorder(), new EmptyBorder(10,20,10,20)));

        if (objects.length > 0) {
            final Object firstObject = objects[0];
            firstButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    choiceDialog.setResponse(firstObject);
                }
            });
            objectsPanel.add(firstButton);
            height += firstButton.getMinimumSize().height;
        }
        for (int i=1; i<objects.length; i++) {
            final Object object = objects[i];
            final JButton objectButton = new JButton(object.toString());
            objectButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    choiceDialog.setResponse(object);
                }
            });
            enterPressesWhenFocused(objectButton);
            objectsPanel.add(objectButton);
            height += objectButton.getMinimumSize().height;
        }
        objectsPanel.add(cancelButton);
        JScrollPane scrollPane = new JScrollPane(objectsPanel,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        choiceDialog.add(scrollPane, BorderLayout.CENTER);

        //choiceDialog.setSize(width, height);
        choiceDialog.setSize(choiceDialog.getPreferredSize());
        choiceDialog.setCancelComponent(cancelButton);

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
        String  okLower = okText.toLowerCase();
        String  cancelLower = cancelText.toLowerCase();
        String  menuMnemonics = "gvorc";
        for ( int ci = 0, nc = okLower.length();  ci < nc;  ci ++ ) {
            char  ch = Character.toLowerCase( okLower.charAt(ci) );

            // if the character at "ci" in "okText" is not claimed by the menu..
            if ( -1 == menuMnemonics.indexOf(ch) ) {

                okButtonMnemonic = ch;
                break;
            }
        }
        for ( int ci = 0, nc = cancelLower.length();  ci < nc;  ci ++ ) {
            char  ch = Character.toLowerCase( cancelLower.charAt(ci) );

            // if the character at "ci" in "cancelText" is not claimed by the
            // menu nor by "okText"..
            if ( -1 == menuMnemonics.indexOf(ch)  &&  -1 == okLower.indexOf(ch) ) {

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
    * Creates a dialog that asks the user what he wants to do with his scout in the foreign
    * colony. Options are: spy the colony, negotiate with foreign power, attack or cancel.
    * The possible responses are integers that are defined in this class as finals.
    *
    * @param colony The foreign colony that is being scouted.
    * @param unit The unit which is scouting
    * @return The FreeColDialog that asks the question to the user.
    */
    public static FreeColDialog createScoutForeignColonyDialog(Colony colony, Unit unit) {
        String mainText = Messages.message("scoutColony.text", 
                "%unit%", unit.getName(), 
                "%colony%", colony.getName());

        final JTextArea question = getDefaultTextArea(mainText);
        final JButton negotiate = new JButton(Messages.message("scoutColony.negotiate")),
                spy = new JButton(Messages.message("scoutColony.spy")),
                attack = new JButton(Messages.message("scoutColony.attack")),
                cancel = new JButton(Messages.message("scoutColony.cancel"));

        final FreeColDialog scoutDialog = new FreeColDialog() {
            public void requestFocus() {
                negotiate.requestFocus();
            }
        };

        int[] widths = {0};
        int[] heights = new int[9];
        for (int index = 0; index < 4; index++) {
            heights[2 * index + 1] = margin;
        }
        int textColumn = 1;

        scoutDialog.setLayout(new HIGLayout(widths, heights));

        negotiate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                scoutDialog.setResponse(new Integer(SCOUT_FOREIGN_COLONY_NEGOTIATE));
            }
        });
        spy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                scoutDialog.setResponse(new Integer(SCOUT_FOREIGN_COLONY_SPY));
            }
        });
        attack.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                scoutDialog.setResponse(new Integer(SCOUT_FOREIGN_COLONY_ATTACK));
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                scoutDialog.setResponse(new Integer(SCOUT_FOREIGN_COLONY_CANCEL));
            }
        });

        int row = 1;
        scoutDialog.add(question, higConst.rc(row, textColumn));
        row += 2;
        scoutDialog.add(negotiate, higConst.rc(row, textColumn));
        row += 2;
        scoutDialog.add(spy, higConst.rc(row, textColumn));
        row += 2;
        scoutDialog.add(attack, higConst.rc(row, textColumn));
        row += 2;
        scoutDialog.add(cancel, higConst.rc(row, textColumn));

        scoutDialog.setSize(scoutDialog.getPreferredSize());
        scoutDialog.setCancelComponent(cancel);

        return scoutDialog;
    }

    /**
    * Creates a dialog that asks the user what he wants to do with his scout in the indian
    * settlement. Options are: speak with chief, demand tribute, attack or cancel.
    * The possible responses are integers that are defined in this class as finals.
    *
    * @param settlement The indian settlement that is being scouted.
    * @param player The player to create the dialog for.
    * @return The FreeColDialog that asks the question to the user.
    */
    public static FreeColDialog createScoutIndianSettlementDialog(IndianSettlement settlement, Player player) {
        String introText = Messages.message(settlement.getAlarmLevelMessage(player),
                "%nation%", settlement.getOwner().getNationAsString());
        UnitType skillType = settlement.getLearnableSkill();
        String messageID;
        String skillName = "";
        if (skillType != null) {
            messageID = "scoutSettlement.question1";
            skillName = Unit.getName(skillType).toLowerCase();
        } else {
            messageID = "scoutSettlement.question2";
        }
        String[] data = new String [] {
            "%replace_skill%", skillName,
            "%replace_good1%", Goods.getName(settlement.getHighlyWantedGoods()),
            "%replace_good2%", Goods.getName(settlement.getWantedGoods1()),
            "%replace_good3%", Goods.getName(settlement.getWantedGoods2())};
    
        String mainText = Messages.message(messageID, data);

        final JTextArea intro = getDefaultTextArea(introText);
        final JTextArea question = getDefaultTextArea(mainText);
        final JButton speak = new JButton(Messages.message("scoutSettlement.speak")),
                demand = new JButton(Messages.message("scoutSettlement.tribute")),
                attack = new JButton(Messages.message("scoutSettlement.attack")),
                cancel = new JButton(Messages.message("scoutSettlement.cancel"));

        final FreeColDialog scoutDialog = new FreeColDialog() {
            public void requestFocus() {
                speak.requestFocus();
            }
        };

        int[] widths = {0};
        int[] heights = new int[11];
        for (int index = 0; index < 5; index++) {
            heights[2 * index + 1] = margin;
        }
        int textColumn = 1;

        scoutDialog.setLayout(new HIGLayout(widths, heights));

        speak.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                scoutDialog.setResponse(new Integer(SCOUT_INDIAN_SETTLEMENT_SPEAK));
            }
        });
        demand.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                scoutDialog.setResponse(new Integer(SCOUT_INDIAN_SETTLEMENT_TRIBUTE));
            }
        });
        attack.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                scoutDialog.setResponse(new Integer(SCOUT_INDIAN_SETTLEMENT_ATTACK));
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                scoutDialog.setResponse(new Integer(SCOUT_INDIAN_SETTLEMENT_CANCEL));
            }
        });

        int row = 1;
        scoutDialog.add(intro, higConst.rc(row, textColumn));
        row += 2;
        scoutDialog.add(question, higConst.rc(row, textColumn));
        row += 2;
        scoutDialog.add(speak, higConst.rc(row, textColumn));
        row += 2;
        scoutDialog.add(demand, higConst.rc(row, textColumn));
        row += 2;
        scoutDialog.add(attack, higConst.rc(row, textColumn));
        row += 2;
        scoutDialog.add(cancel, higConst.rc(row, textColumn));

        scoutDialog.setSize(scoutDialog.getPreferredSize());
        scoutDialog.setCancelComponent(cancel);

        return scoutDialog;
    }

    /**
    * Creates a dialog that asks the user what he wants to do with his scout in the indian
    * settlement. Options are: speak with chief, demand tribute, attack or cancel.
    * The possible responses are integers that are defined in this class as finals.
    *
    * @param settlement The indian settlement that is being scouted.
    * @param player The player to create the dialog for.
    * @return The FreeColDialog that asks the question to the user.
    */
    public static FreeColDialog createArmedUnitIndianSettlementDialog(IndianSettlement settlement, Player player) {
        String introText = Messages.message(settlement.getAlarmLevelMessage(player),
                "%nation%", settlement.getOwner().getNationAsString());
        final JTextArea intro = getDefaultTextArea(introText);
        final JButton attack = new JButton(Messages.message("scoutSettlement.attack")),
                demand = new JButton(Messages.message("scoutSettlement.tribute")),
                cancel = new JButton(Messages.message("scoutSettlement.cancel"));

        final FreeColDialog armedUnitDialog = new FreeColDialog() {
            public void requestFocus() {
                attack.requestFocus();
            }
        };

        int[] widths = {0};
        int[] heights = new int[11];
        for (int index = 0; index < 5; index++) {
            heights[2 * index + 1] = margin;
        }
        int textColumn = 1;

        armedUnitDialog.setLayout(new HIGLayout(widths, heights));

        demand.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                armedUnitDialog.setResponse(new Integer(SCOUT_INDIAN_SETTLEMENT_TRIBUTE));
            }
        });
        attack.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                armedUnitDialog.setResponse(new Integer(SCOUT_INDIAN_SETTLEMENT_ATTACK));
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                armedUnitDialog.setResponse(new Integer(SCOUT_INDIAN_SETTLEMENT_CANCEL));
            }
        });

        int row = 1;
        armedUnitDialog.add(intro, higConst.rc(row, textColumn));
        row += 2;
        armedUnitDialog.add(attack, higConst.rc(row, textColumn));
        row += 2;
        armedUnitDialog.add(demand, higConst.rc(row, textColumn));
        row += 2;
        armedUnitDialog.add(cancel, higConst.rc(row, textColumn));

        armedUnitDialog.setSize(armedUnitDialog.getPreferredSize());
        armedUnitDialog.setCancelComponent(cancel);

        return armedUnitDialog;
    }


    /**
    * Creates a dialog that asks the user what he wants to do with his missionary in the indian
    * settlement. Options are: establish mission, denounce existing (foreign) mission as heresy,
    * incite indians (request them to attack other European player) or cancel.
    * The possible responses are integers that are defined in this class as finals.
    *
    * @param settlement The indian settlement that is being visited.
    * @param player The <code>Player</code> to create the dialog for.
    * @return The FreeColDialog that asks the question to the user.
    */
    public static FreeColDialog createUseMissionaryDialog(IndianSettlement settlement, Player player) {

        String introText = Messages.message(settlement.getAlarmLevelMessage(player),
                                        "%nation%", settlement.getOwner().getNationAsString());
        String mainText = Messages.message("missionarySettlement.question");

        JTextArea intro = getDefaultTextArea(introText);
        JTextArea main = getDefaultTextArea(mainText);

        final JButton establishOrHeresy = new JButton(),
                incite = new JButton(Messages.message("missionarySettlement.incite")),
                cancel = new JButton(Messages.message("missionarySettlement.cancel"));

        if (settlement.getMissionary() == null) {
            establishOrHeresy.setText(Messages.message("missionarySettlement.establish"));
        }
        else {
            establishOrHeresy.setText(Messages.message("missionarySettlement.heresy"));
        }

        final FreeColDialog missionaryDialog = new FreeColDialog() {
            public void requestFocus() {
                establishOrHeresy.requestFocus();
            }
        };

        int[] widths = {0};
        int[] heights = new int[9];
        for (int index = 0; index < 4; index++) {
            heights[2 * index + 1] = margin;
        }
        int textColumn = 1;

        missionaryDialog.setLayout(new HIGLayout(widths, heights));

        if (settlement.getMissionary() == null) {
            establishOrHeresy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    missionaryDialog.setResponse(new Integer(MISSIONARY_ESTABLISH));
                }
            });
        }
        else {
            establishOrHeresy.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    missionaryDialog.setResponse(new Integer(MISSIONARY_DENOUNCE_AS_HERESY));
                }
            });
        }

        incite.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                missionaryDialog.setResponse(new Integer(MISSIONARY_INCITE_INDIANS));
            }
        });
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                missionaryDialog.setResponse(new Integer(MISSIONARY_CANCEL));
            }
        });

        int row = 1;
        missionaryDialog.add(intro, higConst.rc(row, textColumn));
        row += 2;
        missionaryDialog.add(main, higConst.rc(row, textColumn));
        row += 2;
        missionaryDialog.add(establishOrHeresy, higConst.rc(row, textColumn));
        row += 2;
        missionaryDialog.add(incite, higConst.rc(row, textColumn));
        row += 2;
        missionaryDialog.add(cancel, higConst.rc(row, textColumn));

        missionaryDialog.setSize(missionaryDialog.getPreferredSize());
        missionaryDialog.setCancelComponent(cancel);

        return missionaryDialog;
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
    public static FreeColDialog createInciteDialog(Vector<Player> allPlayers, Player thisUser) {
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
