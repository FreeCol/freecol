package net.sf.freecol.client.gui.panel;

import java.util.logging.Logger;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;

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

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
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
                        if (dc == null || !SwingUtilities.isDescendingFrom(dc, this) || dc.getListeners(MouseListener.class).length == 0) {
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
    */
    public boolean getResponseBoolean() {
        return ((Boolean) getResponse()).booleanValue();
    }


    /**
    * Convenience method for {@link #getResponse}.
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

        informationDialog.setLayout(new FlowLayout(FlowLayout.CENTER, 10,10));
        JLabel l = new JLabel(text);
        int width = l.getMinimumSize().width;

        informationDialog.add(l);
        informationDialog.add(okButton);

        informationDialog.setSize(width + 20, l.getMinimumSize().height + okButton.getMinimumSize().height + 40);

        informationDialog.setCancelComponent(okButton);

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
        JLabel l = new JLabel(text);

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
        choiceDialog.add(objectsPanel, BorderLayout.CENTER);

        choiceDialog.setSize(width, height);
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
        final JButton okButton = new JButton(okText);

        final FreeColDialog confirmDialog = new FreeColDialog() {
            public void requestFocus() {
                okButton.requestFocus();
            }
        };

        confirmDialog.setLayout(new BorderLayout());

        JPanel labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(new JLabel(text));

        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton cancelButton = new JButton(cancelText);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                confirmDialog.setResponse(new Boolean(true));
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                confirmDialog.setResponse(new Boolean(false));
            }
        });

        p1.add(okButton);
        p1.add(cancelButton);

        confirmDialog.add(labelPanel, BorderLayout.CENTER);
        confirmDialog.add(p1, BorderLayout.SOUTH);

        confirmDialog.setSize(confirmDialog.getPreferredSize());

        labelPanel.setOpaque(false);
        p1.setOpaque(false);

        if (cancelButton != null) {
            confirmDialog.setCancelComponent(cancelButton);
        } else {
            confirmDialog.setCancelComponent(okButton);
        }

        return confirmDialog;
    }


    /**
    * Returns an information dialog that shows the given text and an "OK" button.
    * @return An information dialog that shows the given text and an "OK" button.
    */
    public static FreeColDialog createInformationDialog(String text) {
        final JLabel theText = new JLabel("<html><body>" + text + "</body></html>");
        final JButton theButton = new JButton(Messages.message("ok"));
        final FreeColDialog informationDialog = new FreeColDialog() {
            public void requestFocus() {
                theButton.requestFocus();
            }
        };

        int w1[] = {10, 90, 80, 90, 10};
        int h1[] = {10, 100, 10, 20, 10};
        HIGLayout layout = new HIGLayout(w1, h1);
        higConst.clearCorrection();
        layout.setRowWeight(2,1);
        informationDialog.setLayout(layout);

        theButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                informationDialog.setResponse(null);
            }
        });

        informationDialog.add(theText, higConst.rcwh(2, 2, 3, 1));
        informationDialog.add(theButton, higConst.rc(4, 3));

        informationDialog.setSize(informationDialog.getPreferredSize());

        return informationDialog;
    }


    /**
    * Creates a dialog that asks the user what he wants to do with his scout in the indian
    * settlement. Options are: speak with chief, demand tribute, attack or cancel.
    * The possible responses are integers that are defined in this class as finals.
    *
    * @param settlement The indian settlement that is being scouted.
    *
    * @return The FreeColDialog that asks the question to the user.
    */
    public static FreeColDialog createScoutIndianSettlementDialog(IndianSettlement settlement) {
        String mainText;
        int skill = settlement.getLearnableSkill();
        if (skill >= 0) {
            mainText = Messages.message("scoutSettlement.question1").replaceAll("%replace_skill%", Unit.getName(skill).toLowerCase());
        }
        else {
            mainText = Messages.message("scoutSettlement.question2");
        }
        mainText = mainText.replaceAll("%replace_good1%", Goods.getName(settlement.getHighlyWantedGoods()).toLowerCase());
        mainText = mainText.replaceAll("%replace_good2%", Goods.getName(settlement.getWantedGoods1()).toLowerCase());
        mainText = mainText.replaceAll("%replace_good3%", Goods.getName(settlement.getWantedGoods2()).toLowerCase());

        final JLabel question = new JLabel("<html><body>" + mainText + "</body></html>");
        final JButton speak = new JButton(Messages.message("scoutSettlement.speak")),
                demand = new JButton(Messages.message("scoutSettlement.tribute")),
                attack = new JButton(Messages.message("scoutSettlement.attack")),
                cancel = new JButton(Messages.message("scoutSettlement.cancel"));

        final FreeColDialog scoutDialog = new FreeColDialog() {
            public void requestFocus() {
                cancel.requestFocus();
            }
        };

        int w1[] = {10, 30, 200, 30, 10};
        int h1[] = {10, 100, 10, 20, 10, 20, 10, 20, 10, 20, 10};
        HIGLayout layout = new HIGLayout(w1, h1);
        higConst.clearCorrection();
        layout.setRowWeight(2,1);
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

        scoutDialog.add(question, higConst.rcwh(2, 2, 3, 1));
        scoutDialog.add(speak, higConst.rc(4, 3));
        scoutDialog.add(demand, higConst.rc(6, 3));
        scoutDialog.add(attack, higConst.rc(8, 3));
        scoutDialog.add(cancel, higConst.rc(10, 3));

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
    *
    * @return The FreeColDialog that asks the question to the user.
    */
    public static FreeColDialog createUseMissionaryDialog(IndianSettlement settlement) {
        String mainText = Messages.message("missionarySettlement.question");

        final JLabel question = new JLabel("<html><body>" + mainText + "</body></html>");
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

        int w1[] = {10, 30, 200, 30, 10};
        int h1[] = {10, 100, 10, 20, 10, 20, 10, 20, 10};
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

        missionaryDialog.add(question, higConst.rcwh(2, 2, 3, 1));
        missionaryDialog.add(establishOrHeresy, higConst.rc(4, 3));
        missionaryDialog.add(incite, higConst.rc(6, 3));
        missionaryDialog.add(cancel, higConst.rc(8, 3));

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

        final JLabel question = new JLabel("<html><body>" + mainText + "</body></html>");
        final JButton players[] = new JButton[allPlayers.size() - 1],
                cancel = new JButton(Messages.message("missionarySettlement.cancel"));

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

        int w1[] = {10, 30, 200, 30, 10};
        int h1[] = new int[3 + allPlayers.size() * 2];

        // h1 = {10, 100, 10, 20, 10, 20, 10, 20, 10, ...};
        h1[0] = 10;
        h1[1] = 100;
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
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog createLoadDialog(File directory) {
        String dir = System.getProperty("user.home");
        String fileSeparator = System.getProperty("file.separator");
        if (!dir.endsWith(fileSeparator)) {
            dir += fileSeparator;
        }
        dir += ".freecol" + fileSeparator + "save";

        final FreeColDialog loadDialog = new FreeColDialog();
        final JFileChooser fileChooser = new JFileChooser(dir);

        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.addChoosableFileFilter(loadDialog.new SavegameFilter());
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String actionCommand = event.getActionCommand();
                if (actionCommand.equals(JFileChooser.APPROVE_SELECTION)) {
                    loadDialog.setResponse(fileChooser.getSelectedFile());
                }
                else if (actionCommand.equals(JFileChooser.CANCEL_SELECTION)) {
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
    * @return The <code>FreeColDialog</code>.
    */
    public static FreeColDialog createSaveDialog(File directory) {
        String dir = System.getProperty("user.home");
        String fileSeparator = System.getProperty("file.separator");
        if (!dir.endsWith(fileSeparator)) {
            dir += fileSeparator;
        }
        dir += ".freecol" + fileSeparator + "save";

        final FreeColDialog saveDialog = new FreeColDialog();
        final JFileChooser fileChooser = new JFileChooser(dir);

        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.addChoosableFileFilter(saveDialog.new SavegameFilter());
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String actionCommand = event.getActionCommand();
                if (actionCommand.equals(JFileChooser.APPROVE_SELECTION)) {
                    File file = fileChooser.getSelectedFile();
                    if (!file.getName().endsWith(".fsg")) {
                        file = new File(file.getAbsolutePath() + ".fsg");
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
    * Is being used to make the Save- and LoadDialogs more user-friendly.
    */
    final class SavegameFilter extends FileFilter {
        public boolean accept(File f) {
            if (f.getName().endsWith(".fsg") || f.isDirectory()) {
                return true;
            }
            else {
                return false;
            }
        }

        public String getDescription() {
            return "*.fsg";
        }
    }
}
