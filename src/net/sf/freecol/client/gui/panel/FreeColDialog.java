package net.sf.freecol.client.gui.panel;

import java.awt.AWTEvent;
import java.awt.ActiveEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.text.BreakIterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
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

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import cz.autel.dmi.HIGLayout;


/**
* Superclass for all dialogs in FreeCol. This class also contains
* methods to create simple dialogs.
*/
public class FreeColDialog extends FreeColPanel {
    private static final Logger logger = Logger.getLogger(FreeColDialog.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int SCOUT_INDIAN_SETTLEMENT_CANCEL = 0,
                            SCOUT_INDIAN_SETTLEMENT_SPEAK = 1,
                            SCOUT_INDIAN_SETTLEMENT_TRIBUTE = 2,
                            SCOUT_INDIAN_SETTLEMENT_ATTACK = 3;

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
    * Sets the <code>response</code> and wakes up any thread waiting for this information.
    *
    * @param response The object that should be returned by {@link #getResponse}.
    */
    public synchronized void setResponse(Object response) {
        this.response = response;
        responseGiven = true;
        logger.info("Response has been set.");
        notifyAll();
    }


    /**
    * Returns the <code>response</code> when set by <code>setResponse(Object response)</code>.
    * Waits the thread until then.
    *
    * @return The object as set by {@link #setResponse}.
    */
    public synchronized Object getResponse() {
        // Wait the thread until 'response' is availeble. Notice that we have to process
        // the events manually if the current thread is the Event Dispatch Thread (EDT).

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                EventQueue theQueue = getToolkit().getSystemEventQueue();

                while (!responseGiven) {
                    // This is essentially the body of EventDispatchThread
                    AWTEvent event = theQueue.getNextEvent();
                    Object src = event.getSource();

                    // Block 'MouseEvent' beeing sent to other components:
                    if (event instanceof MouseEvent) {
                        MouseEvent me = (MouseEvent) event;
                        Component dc = SwingUtilities.getDeepestComponentAt(((ComponentEvent) event).getComponent(), me.getX(), me.getY());
                        // Don't block MOUSE_RELEASED events out of the dialog, or the text stays selected forever
                        if (event.getID() != MouseEvent.MOUSE_RELEASED &&
                        		(dc == null || !SwingUtilities.isDescendingFrom(dc, this) || dc.getListeners(MouseListener.class).length == 0)) {
                            continue;
                        }
                    }

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

        informationDialog.setLayout(new BorderLayout(10, 10));
        JLabel textLabel = new JLabel(text);
        int width = textLabel.getMinimumSize().width;

        if (image == null) {
            informationDialog.add(textLabel, BorderLayout.CENTER);
        } else {
            JLabel imageLabel = new JLabel(image);
            informationDialog.add(imageLabel, BorderLayout.LINE_START);
            informationDialog.add(textLabel, BorderLayout.LINE_END);
        }
        informationDialog.add(okButton, BorderLayout.PAGE_END);

        informationDialog.setSize(width + 20, textLabel.getMinimumSize().height +
                                  okButton.getMinimumSize().height + 40);

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

        int margin = 10;
        int[] widths = {margin, 0, 10, 0, margin};
        int[] heights = new int[texts.length + 4];
        heights[0] = margin;

        for (int i = 0; i < texts.length; i++) {
            // add one to index because of margin
            heights[i+1] = 0;
        }
        heights[texts.length + 1] = 10;
        heights[texts.length + 2] = 0;
        heights[texts.length + 3] = margin;

        final JButton theButton = new JButton(Messages.message("ok"));
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

        for (int i = 0; i < texts.length; i++) {
            // add two to index because HIGLayout starts at one and
            // there is a margin
            int row = i + 2;
            if (images[i] != null) {
                informationDialog.add(new JLabel(images[i]), higConst.rc(row, 2));
            }
            informationDialog.add(new JLabel("<html><body><p>" + texts[i] + "</body></html>"),
                                  higConst.rc(row, 4));
        }
        int buttonRow = texts.length + 3;
        informationDialog.add(theButton, higConst.rcwh(buttonRow, 2, 3, 1));

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
    public static FreeColDialog createChoiceDialog(String text, String cancelText, Object[] objects) {
        final JButton cancelButton = new JButton(cancelText);

        final FreeColDialog choiceDialog = new FreeColDialog() {
            public void requestFocus() {
                cancelButton.requestFocus();
            }
        };

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                choiceDialog.setResponse(null);
            }
        });

        choiceDialog.setLayout(new BorderLayout());
        JLabel l = new JLabel(text, JLabel.CENTER);

        int height = l.getMinimumSize().height + cancelButton.getMinimumSize().height + 40;

        choiceDialog.add(l, BorderLayout.NORTH);

        JPanel objectsPanel = new JPanel(new GridLayout(objects.length+1, 1, 10,10));
        objectsPanel.setBorder(new CompoundBorder(objectsPanel.getBorder(), new EmptyBorder(10,20,10,20)));
        int width = Math.max(l.getMinimumSize().width, objectsPanel.getPreferredSize().width) + 20;

        for (int i=0; i<objects.length; i++) {
            final Object object = objects[i];
            final JButton objectButton = new JButton(object.toString());
            objectButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    choiceDialog.setResponse(object);
                }
            });
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
        return createConfirmDialog(new String[] {text}, new ImageIcon[] {null}, okText, cancelText);
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

        int margin = 10;
        int[] widths = {margin, 0, 10, 0, margin};
        int[] heights = new int[texts.length + 4];
        int buttonsWidth = -widths[3];
        heights[0] = margin;

        for (int i = 0; i < texts.length; i++) {
            // add one to index because of margin
            heights[i+1] = 0;
        }
        heights[texts.length + 1] = 10;
        heights[texts.length + 2] = 0;
        heights[texts.length + 3] = margin;

        HIGLayout layout = new HIGLayout(widths, heights);
        confirmDialog.setLayout(layout);

        int maxImageWidth = 0;
        int maxTextWidth = 0;
        for (int i = 0; i < texts.length; i++) {
            // add two to index because HIGLayout starts at one and
            // there is a margin
            int row = i + 2;
            if (images[i] != null) {
                JLabel image = new JLabel(images[i]);
                confirmDialog.add(image, higConst.rc(row, 2));
                maxImageWidth = Math.max(maxImageWidth, image.getPreferredSize().width);
            }
            JLabel text = new JLabel("<html><body><p>" + texts[i] + "</body></html>");
            confirmDialog.add(text, higConst.rc(row, 4));
            maxTextWidth = Math.max(maxTextWidth, text.getPreferredSize().width);
        }
        buttonsWidth -= maxImageWidth;

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
        JPanel buttonPanel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
        buttonPanel.setOpaque(false);
        buttonPanel.add( okButton );
        buttonPanel.add( new JButton(cancelAction) );
        buttonsWidth += buttonPanel.getPreferredSize().width;

        // finish building the dialog
        //confirmDialog.setLayout( new BorderLayout() );
        //confirmDialog.add( labelPanel, BorderLayout.CENTER );
        //confirmDialog.add( buttonPanel, BorderLayout.SOUTH );
        int buttonRow = texts.length + 3;
        confirmDialog.add(buttonPanel, higConst.rcwh(buttonRow, 2, 3, 1));
        layout.setPreferredColumnWidth(4, Math.max(buttonsWidth, maxTextWidth));
        confirmDialog.setSize( confirmDialog.getPreferredSize() );
        confirmDialog.setCancelComponent( new JButton(cancelAction) );

        return confirmDialog;
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
                                            new String [][] {{"%nation%", settlement.getOwner().getNationAsString()}});
        int skill = settlement.getLearnableSkill();
        String messageID;
        String skillName = "";
        if (skill >= 0) {
            messageID = "scoutSettlement.question1";
            skillName = Unit.getName(skill).toLowerCase();
        } else {
            messageID = "scoutSettlement.question2";
        }
        String [][] data = new String [][] {
            {"%replace_skill%", skillName},
            {"%replace_good1%", Goods.getName(settlement.getHighlyWantedGoods()).toLowerCase()},
            {"%replace_good2%", Goods.getName(settlement.getWantedGoods1()).toLowerCase()},
            {"%replace_good3%", Goods.getName(settlement.getWantedGoods2()).toLowerCase()}};
    
        String mainText = Messages.message(messageID, data);

        final JLabel intro = new WrapLabel(introText, 300);
        final JLabel question = new WrapLabel(mainText, 300);
        final JButton speak = new JButton(Messages.message("scoutSettlement.speak")),
                demand = new JButton(Messages.message("scoutSettlement.tribute")),
                attack = new JButton(Messages.message("scoutSettlement.attack")),
                cancel = new JButton(Messages.message("scoutSettlement.cancel"));

        final FreeColDialog scoutDialog = new FreeColDialog() {
            public void requestFocus() {
                cancel.requestFocus();
            }
        };

        int[] w1 = {10, 300, 10};
        int[] h1 = {10, 0, 10, 0, 10, 20, 10, 20, 10, 20, 10, 20, 10};

        HIGLayout layout = new HIGLayout(w1, h1);
        //higConst.clearCorrection();
        //layout.setRowWeight(2,1);
        scoutDialog.setLayout(layout);

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

        scoutDialog.add(intro, higConst.rc(2, 2));
        scoutDialog.add(question, higConst.rc(4, 2));
        scoutDialog.add(speak, higConst.rc(6, 2));
        scoutDialog.add(demand, higConst.rc(8, 2));
        scoutDialog.add(attack, higConst.rc(10, 2));
        scoutDialog.add(cancel, higConst.rc(12, 2));

        scoutDialog.setSize(scoutDialog.getPreferredSize());

        return scoutDialog;
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
                                        new String [][] {{"%nation%", settlement.getOwner().getNationAsString()}});
        String mainText = Messages.message("missionarySettlement.question");

        final JLabel intro = new WrapLabel(introText, 260);
        final JLabel main = new WrapLabel(mainText, 260);
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
                cancel.requestFocus();
            }
        };

        int[] w1 = {10, 30, 200, 30, 10};
        int[] h1 = {10, 0, 10, 0, 10, 20, 10, 20, 10, 20, 10};
        HIGLayout layout = new HIGLayout(w1, h1);
        higConst.clearCorrection();
        layout.setRowWeight(2,1);
        missionaryDialog.setLayout(layout);

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

        missionaryDialog.add(intro, higConst.rcwh(2, 2, 3, 1));
        missionaryDialog.add(main, higConst.rcwh(4, 2, 3, 1));
        missionaryDialog.add(establishOrHeresy, higConst.rc(6, 3));
        missionaryDialog.add(incite, higConst.rc(8, 3));
        missionaryDialog.add(cancel, higConst.rc(10, 3));

        missionaryDialog.setSize(missionaryDialog.getPreferredSize());

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
    public static FreeColDialog createInciteDialog(Vector allPlayers, Player thisUser) {
        String mainText = Messages.message("missionarySettlement.inciteQuestion");

        final JLabel question = new WrapLabel(mainText, 260);
        final JButton[] players = new JButton[allPlayers.size() - 1];
        final JButton cancel = new JButton(Messages.message("missionarySettlement.cancel"));

        int arrayIndex = 0;
        for (int i = 0; i < allPlayers.size(); i++) {
            Player p = (Player)allPlayers.get(i);
            if (p.equals(thisUser)) {
                continue;
            }
            players[arrayIndex] = new JButton(p.getName() + " (" + p.getNationAsString() + ")");
            arrayIndex++;
        }

        final FreeColDialog inciteDialog = new FreeColDialog() {
            public void requestFocus() {
                cancel.requestFocus();
            }
        };

        int[] w1 = {10, 30, 200, 30, 10};
        int[] h1 = new int[3 + allPlayers.size() * 2];

        // h1 = {10, 0, 10, 20, 10, 20, 10, 20, 10, ...};
        h1[0] = 10;
        h1[1] = 0;
        h1[2] = 10;
        for (int i = 3; i < h1.length; i += 2) {
            h1[i] = 20;
            h1[i + 1] = 10;
        }

        HIGLayout layout = new HIGLayout(w1, h1);
        higConst.clearCorrection();
        layout.setRowWeight(2,1);
        inciteDialog.setLayout(layout);

        arrayIndex = 0;
        for (int i = 0; i < allPlayers.size(); i++) {
            final Player p = (Player)allPlayers.get(i);
            if (p.equals(thisUser)) {
                continue;
            }
            players[arrayIndex].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    inciteDialog.setResponse(p);
                }
            });
            arrayIndex++;
        }
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inciteDialog.setResponse(null);
            }
        });

        inciteDialog.add(question, higConst.rcwh(2, 2, 3, 1));
        for (int i = 0; i < players.length; i++) {
            inciteDialog.add(players[i], higConst.rc(4 + i * 2, 3));
        }
        inciteDialog.add(cancel, higConst.rc(4 + players.length * 2, 3));

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

        inputDialog.setLayout(new GridLayout(3, 1, 10,10));

        inputDialog.setBorder(new CompoundBorder(inputDialog.getBorder(), new EmptyBorder(3,10,10,10)));

        JPanel buttons = new JPanel(new GridLayout(1, 2,10,10));

        JButton okButton = new JButton(okText);

        JButton cancelButton = null;
        if (cancelText != null) {
            cancelButton = new JButton(cancelText);
        }

        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(input.getText());
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(input.getText());
            }
        });

        if (cancelButton != null) {
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    inputDialog.setResponse(null);
                }
            });
        }

        buttons.add(okButton);

        if (cancelButton != null) {
            buttons.add(cancelButton);
        }

        inputDialog.add(new JLabel(text));
        inputDialog.add(input);
        inputDialog.add(buttons);

        inputDialog.setSize(inputDialog.getPreferredSize());

        buttons.setOpaque(false);

        if (cancelButton != null) {
            inputDialog.setCancelComponent(cancelButton);
        } else {
            inputDialog.setCancelComponent(okButton);
        }

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
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog createSaveDialog(File directory, final String standardName, FileFilter[] fileFilters) {
        final FreeColDialog saveDialog = new FreeColDialog();
        final JFileChooser fileChooser = new JFileChooser(directory);

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

    static final class WrapLabel extends JLabel {
        public WrapLabel() {
            super();
        }
        
        public WrapLabel(Icon image) {
            super(image);
        }
        
        public WrapLabel(Icon image, int horizontalAlignment) {
            super(image, horizontalAlignment);
        }
            
        public WrapLabel(String text, int maxWidth) {
            setText(text, maxWidth);
        }
        
        public WrapLabel(String text, int maxWidth, Icon icon, int horizontalAlignment) {
            super(icon, horizontalAlignment);
            setText(text, maxWidth);
        }
        
        public WrapLabel(String text, int maxWidth, int horizontalAlignment) {
            setText(text, maxWidth);
            setHorizontalAlignment(horizontalAlignment);
        }
            
        public void setText(String text, int maxWidth) {
            FontMetrics fm = getFontMetrics(getFont());

            BreakIterator boundary = BreakIterator.getWordInstance();
            boundary.setText(text);

            StringBuffer trial = new StringBuffer();
            StringBuffer real = new StringBuffer("<html><body><p>");

            int lines = 1;
            int start = boundary.first();
            for (int end = boundary.next(); end != BreakIterator.DONE;
                    start = end, end = boundary.next()) {
                    String word = text.substring(start,end);
                    trial.append(word);
                    int trialWidth = fm.stringWidth(trial.toString());
                    if (trialWidth > maxWidth) {
                            trial = new StringBuffer(word);
                            lines++;
                    }
                    real.append(word);
            }
            real.append("</p></body></html>");

            setText(real.toString());
            setPreferredSize(new Dimension(maxWidth, fm.getHeight() * lines));
        }
    }
}
