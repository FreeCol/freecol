package net.sf.freecol.client.gui;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.UnitButton;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.FreeCol;

import javax.swing.*;

import java.awt.event.*;
import java.util.Iterator;
import java.util.ArrayList;


/**
* The menu bar that is displayed on the top left corner of the <code>Canvas</code>.
* @see Canvas#setJMenuBar
*/
public class FreeColMenuBar extends JMenuBar {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int UNIT_ORDER_WAIT = 0;
    public static final int UNIT_ORDER_FORTIFY = 1;
    public static final int UNIT_ORDER_SENTRY = 2;
    public static final int UNIT_ORDER_BUILD_COL = 4;
    public static final int UNIT_ORDER_PLOW = 5;
    public static final int UNIT_ORDER_BUILD_ROAD = 6;
    public static final int UNIT_ORDER_SKIP = 8;
    public static final int UNIT_ORDER_DISBAND = 10;
    
    private final FreeColClient freeColClient;
    private final Canvas canvas;
    private final GUI gui;
    private ArrayList inGameOptions = new ArrayList();
    private ArrayList mapControlOptions = new ArrayList();
    private JMenuItem saveMenuItem;


    /**
    * Creates a new <code>FreeColMenuBar</code>. This menu bar will include
    * all of the submenus and items.
    *
    * @param f The main controller.
    * @param c The {@link Canvas} that contains methods to show panels e.t.c.
    * @param g The {@link GUI} that has methods to draw the game map.
    */
    public FreeColMenuBar(FreeColClient f, Canvas c, GUI g) {
        super();

        this.freeColClient = f;
        this.canvas = c;
        this.gui = g;

        // --> Game
        JMenu gameMenu = new JMenu(Messages.message("menuBar.game"));
        gameMenu.setOpaque(false);
        gameMenu.setMnemonic(KeyEvent.VK_G);
        add(gameMenu);

        JMenuItem newMenuItem = new JMenuItem(Messages.message("menuBar.game.new"));
        newMenuItem.setOpaque(false);
        newMenuItem.setMnemonic(KeyEvent.VK_N);
        newMenuItem.setAccelerator(KeyStroke.getKeyStroke('N', InputEvent.CTRL_MASK));
        gameMenu.add(newMenuItem);
        newMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.newGame();
            }
        });

        JMenuItem openMenuItem = new JMenuItem(Messages.message("menuBar.game.open"));
        openMenuItem.setOpaque(false);
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke('O', InputEvent.CTRL_MASK));
        gameMenu.add(openMenuItem);
        openMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getInGameController().loadGame();
            }
        });

        saveMenuItem = new JMenuItem(Messages.message("menuBar.game.save"));
        saveMenuItem.setOpaque(false);
        saveMenuItem.setMnemonic(KeyEvent.VK_S);
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK));
        gameMenu.add(saveMenuItem);
        saveMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getInGameController().saveGame();
            }
        });
        inGameOptions.add(saveMenuItem);

        gameMenu.addSeparator();

        JMenuItem reconnectMenuItem = new JMenuItem(Messages.message("menuBar.game.reconnect"));
        reconnectMenuItem.setOpaque(false);
        reconnectMenuItem.setMnemonic(KeyEvent.VK_R);
        reconnectMenuItem.setAccelerator(KeyStroke.getKeyStroke('R', InputEvent.CTRL_MASK));
        gameMenu.add(reconnectMenuItem);
        reconnectMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getConnectController().reconnect();
            }
        });

        gameMenu.addSeparator();

        JMenuItem quitMenuItem = new JMenuItem(Messages.message("menuBar.game.quit"));
        quitMenuItem.setOpaque(false);
        quitMenuItem.setMnemonic(KeyEvent.VK_Q);
        quitMenuItem.setAccelerator(KeyStroke.getKeyStroke('Q', InputEvent.CTRL_MASK));
        gameMenu.add(quitMenuItem);
        quitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.quit();
            }
        });

        // --> View

        JMenu viewMenu = new JMenu(Messages.message("menuBar.view"));
        viewMenu.setOpaque(false);
        viewMenu.setMnemonic(KeyEvent.VK_V);
        add(viewMenu);
        inGameOptions.add(viewMenu);

        final JCheckBoxMenuItem mcMenuItem = new JCheckBoxMenuItem(Messages.message("menuBar.view.mapControls"), true);
        mcMenuItem.setOpaque(false);
        mcMenuItem.setMnemonic(KeyEvent.VK_M);
        mcMenuItem.setAccelerator(KeyStroke.getKeyStroke('M', InputEvent.CTRL_MASK));
        viewMenu.add(mcMenuItem);
        mcMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean showMC = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                if (showMC && !canvas.getMapControls().isShowing()) {
                    canvas.getMapControls().addToComponent(canvas);
                } else if (!showMC && canvas.getMapControls().isShowing()) {
                    canvas.getMapControls().removeFromComponent(canvas);
                } // else: ignore
            }
        });
        inGameOptions.add(mcMenuItem);
        mapControlOptions.add(mcMenuItem);

        final JCheckBoxMenuItem dtnMenuItem = new JCheckBoxMenuItem(Messages.message("menuBar.view.displayTileNames"));
        dtnMenuItem.setOpaque(false);
        dtnMenuItem.setMnemonic(KeyEvent.VK_D);
        dtnMenuItem.setAccelerator(KeyStroke.getKeyStroke('D', InputEvent.CTRL_MASK));
        viewMenu.add(dtnMenuItem);
        dtnMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getGUI().setDisplayTileNames(((JCheckBoxMenuItem) e.getSource()).isSelected());
                freeColClient.getCanvas().refresh();
            }
        });
        inGameOptions.add(dtnMenuItem);

        final JCheckBoxMenuItem dgMenuItem = new JCheckBoxMenuItem(Messages.message("menuBar.view.displayGrid"));
        dgMenuItem.setOpaque(false);
        dgMenuItem.setMnemonic(KeyEvent.VK_G);
        dgMenuItem.setAccelerator(KeyStroke.getKeyStroke('G', InputEvent.CTRL_MASK));
        viewMenu.add(dgMenuItem);
        dgMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getGUI().setDisplayGrid(((JCheckBoxMenuItem) e.getSource()).isSelected());
                freeColClient.getCanvas().refresh();
            }
        });
        inGameOptions.add(dgMenuItem);

        viewMenu.addSeparator();

        final JMenuItem europeMenuItem = new JMenuItem(Messages.message("menuBar.view.europe"));
        europeMenuItem.setOpaque(false);
        europeMenuItem.setMnemonic(KeyEvent.VK_E);
        //europeMenuItem.setAccelerator(KeyStroke.getKeyStroke('E'));
        viewMenu.add(europeMenuItem);
        europeMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showEuropePanel();
            }
        });
        inGameOptions.add(europeMenuItem);

        // --> Orders
        JMenu ordersMenu = new JMenu(Messages.message("menuBar.orders"));
        ordersMenu.setOpaque(false);
        ordersMenu.setMnemonic(KeyEvent.VK_O);
        add(ordersMenu);
        inGameOptions.add(ordersMenu);
        
        final JMenuItem waitMenuItem = new JMenuItem(Messages.message("unit.state.0"));
        waitMenuItem.setOpaque(false);
        waitMenuItem.setMnemonic(KeyEvent.VK_W);
        //waitMenuItem.setAccelerator(KeyStroke.getKeyStroke('W'));
        ordersMenu.add(waitMenuItem);
        waitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getInGameController().nextActiveUnit();
            }
        });
        inGameOptions.add(waitMenuItem);

        final JMenuItem fortifyMenuItem = new JMenuItem(Messages.message("unit.state.2"));
        fortifyMenuItem.setOpaque(false);
        fortifyMenuItem.setMnemonic(KeyEvent.VK_F);
        //fortifyMenuItem.setAccelerator(KeyStroke.getKeyStroke('F'));
        ordersMenu.add(fortifyMenuItem);
        fortifyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().changeState(gui.getActiveUnit(), Unit.FORTIFY);
                }
            }
        });
        inGameOptions.add(fortifyMenuItem);
        
        final JMenuItem sentryMenuItem = new JMenuItem(Messages.message("unit.state.3"));
        sentryMenuItem.setOpaque(false);
        sentryMenuItem.setMnemonic(KeyEvent.VK_S);
        //sentryMenuItem.setAccelerator(KeyStroke.getKeyStroke('S'));
        ordersMenu.add(sentryMenuItem);
        sentryMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().changeState(gui.getActiveUnit(), Unit.SENTRY);
                }
            }
        });
        inGameOptions.add(sentryMenuItem);
        
        ordersMenu.addSeparator();
        
        final JMenuItem colonyMenuItem = new JMenuItem(Messages.message("unit.state.7"));
        colonyMenuItem.setOpaque(false);
        colonyMenuItem.setMnemonic(KeyEvent.VK_B);
        //colonyMenuItem.setAccelerator(KeyStroke.getKeyStroke('B'));
        ordersMenu.add(colonyMenuItem);
        colonyMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getInGameController().buildColony();
            }
        });
        inGameOptions.add(colonyMenuItem);
        
        final JMenuItem plowMenuItem = new JMenuItem(Messages.message("unit.state.5"));
        plowMenuItem.setOpaque(false);
        plowMenuItem.setMnemonic(KeyEvent.VK_P);
        //plowMenuItem.setAccelerator(KeyStroke.getKeyStroke('P'));
        ordersMenu.add(plowMenuItem);
        plowMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().changeState(gui.getActiveUnit(), Unit.PLOW);
                }
            }
        });
        inGameOptions.add(plowMenuItem);
        
        final JMenuItem roadMenuItem = new JMenuItem(Messages.message("unit.state.6"));
        roadMenuItem.setOpaque(false);
        roadMenuItem.setMnemonic(KeyEvent.VK_R);
        //roadMenuItem.setAccelerator(KeyStroke.getKeyStroke('R'));
        ordersMenu.add(roadMenuItem);
        roadMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(gui.getActiveUnit() != null) {
                    freeColClient.getInGameController().changeState(gui.getActiveUnit(), Unit.BUILD_ROAD);
                }
            }
        });
        inGameOptions.add(roadMenuItem);
        
        ordersMenu.addSeparator();
        
        final JMenuItem skipMenuItem = new JMenuItem(Messages.message("unit.state.1"));
        skipMenuItem.setOpaque(false);
        skipMenuItem.setMnemonic(KeyEvent.VK_SPACE);
        //skipMenuItem.setAccelerator(KeyStroke.getKeyStroke(' '));
        ordersMenu.add(skipMenuItem);
        skipMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getInGameController().skipActiveUnit();
            }
        });
        inGameOptions.add(skipMenuItem);
        
        ordersMenu.addSeparator();
        
        final JMenuItem disbandMenuItem = new JMenuItem(Messages.message("unit.state.8"));
        disbandMenuItem.setOpaque(false);
        disbandMenuItem.setMnemonic(KeyEvent.VK_D);
        //disbandMenuItem.setAccelerator(KeyStroke.getKeyStroke('D'));
        ordersMenu.add(disbandMenuItem);
        disbandMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO
            }
        });
        inGameOptions.add(disbandMenuItem);        
        
        // --> Debug
        if (FreeCol.isInDebugMode()) {
            JMenu debugMenu = new JMenu(Messages.message("menuBar.debug"));
            debugMenu.setOpaque(false);
            debugMenu.setMnemonic(KeyEvent.VK_D);
            add(debugMenu);
            inGameOptions.add(debugMenu);

            JCheckBoxMenuItem sc = new JCheckBoxMenuItem(Messages.message("menuBar.debug.showCoordinates"));
            sc.setOpaque(false);
            sc.setMnemonic(KeyEvent.VK_S);
            debugMenu.add(sc);
            inGameOptions.add(sc);

            sc.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.displayCoordinates = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                    canvas.refresh();
                }
            });

            final JMenuItem reveal = new JCheckBoxMenuItem(Messages.message("menuBar.debug.revealEntireMap"));
            reveal.setOpaque(false);
            reveal.setMnemonic(KeyEvent.VK_R);
            debugMenu.add(reveal);
            inGameOptions.add(reveal);

            reveal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (freeColClient.getFreeColServer() != null) {
                        freeColClient.getFreeColServer().revealMapForAllPlayers();
                    }

                    reveal.setEnabled(false);
                }
            });
        }
    }


    /**
    * Updates this <code>FreeColMenuBar</code>.
    */
    public void update() {
        if (!freeColClient.getGUI().isInGame()) {
            return;
        }

        boolean enabled = (freeColClient.getGUI().getActiveUnit() != null)
                          && !canvas.getColonyPanel().isShowing()
                          && !canvas.getEuropePanel().isShowing();

        Iterator componentIterator = mapControlOptions.iterator();
        while (componentIterator.hasNext()) {
            ((JComponent) componentIterator.next()).setEnabled(enabled);
        }

        saveMenuItem.setEnabled(freeColClient.getMyPlayer().isAdmin() && freeColClient.getFreeColServer() != null);
        
        // Update Orders menu.
        Unit selectedOne = freeColClient.getGUI().getActiveUnit();
        JMenu ordersMenu = getMenu(2);
        if(selectedOne == null) {
            for (int t=0; t < ordersMenu.getItemCount(); t++) {
                if(ordersMenu.getItem(t) != null)
                    ordersMenu.getItem(t).setEnabled(false);
            }
            return;
        }
        
        int unitType = selectedOne.getType();
        ordersMenu.getItem(UNIT_ORDER_WAIT).setEnabled(true); // All units can wait
        ordersMenu.getItem(UNIT_ORDER_FORTIFY).setEnabled(true); // All units can fortify
        ordersMenu.getItem(UNIT_ORDER_SENTRY).setEnabled(true); // All units can sentry
        ordersMenu.getItem(UNIT_ORDER_SKIP).setEnabled(true); // All units can be skipped
        ordersMenu.getItem(UNIT_ORDER_DISBAND).setEnabled(true); // All units can be disbanded
        
        /* Clear Forest / Plow Fields
         *  Only colonists can do this, only if they have at least 20 tools, and only if they are
         *  in a square that can be improved
         */
         if (selectedOne.getTile() != null) {
             Tile tile = selectedOne.getTile();
             if(tile.isLand() && tile.isForested()) {
                 ordersMenu.getItem(UNIT_ORDER_PLOW).setText(Messages.message("unit.state.4"));
                 ordersMenu.getItem(UNIT_ORDER_PLOW).setEnabled(selectedOne.isPioneer());
             } else if (tile.isLand() && !tile.isForested() && !tile.isPlowed()) {
                 ordersMenu.getItem(UNIT_ORDER_PLOW).setText(Messages.message("unit.state.5"));
                 ordersMenu.getItem(UNIT_ORDER_PLOW).setEnabled(selectedOne.isPioneer());
             } else if (tile.isLand() && !tile.isForested() && tile.isPlowed()) {
                 ordersMenu.getItem(UNIT_ORDER_PLOW).setText(Messages.message("unit.state.5"));
                 ordersMenu.getItem(UNIT_ORDER_PLOW).setEnabled(false);
             } else {
                 ordersMenu.getItem(UNIT_ORDER_PLOW).setText(Messages.message("unit.state.5"));
                 ordersMenu.getItem(UNIT_ORDER_PLOW).setEnabled(false);
             }
         } else {
             ordersMenu.getItem(UNIT_ORDER_PLOW).setText(Messages.message("unit.state.5"));
             ordersMenu.getItem(UNIT_ORDER_PLOW).setEnabled(false);
         }
         
         /* Build roads
         *  Only colonists can do this, only if they have at least 20 tools, and only if they are
         *  in a land square that does not already have roads
         */
         if (selectedOne.getTile() != null && selectedOne.isPioneer()) {
             Tile tile = selectedOne.getTile();
             if(tile.isLand() && !tile.hasRoad()) {
                 ordersMenu.getItem(UNIT_ORDER_BUILD_ROAD).setEnabled(true);
             } else {
                 ordersMenu.getItem(UNIT_ORDER_BUILD_ROAD).setEnabled(false);
             }
         } else {
             ordersMenu.getItem(UNIT_ORDER_BUILD_ROAD).setEnabled(false);
         }
         
         /* Build a new colony
         *  Only colonists can do this, and only if they are on a 'colonizeable' tile
         */
         if (selectedOne.getTile() != null && selectedOne.canBuildColony()) {
             ordersMenu.getItem(UNIT_ORDER_BUILD_COL).setEnabled(true);
         } else {
             ordersMenu.getItem(UNIT_ORDER_BUILD_COL).setEnabled(false);
         }
    }


    /**
    * When a <code>FreeColMenuBar</code> is disabled, it does
    * not show the "in game options".
    */
    public void setEnabled(boolean enabled) {
        Iterator componentIterator = inGameOptions.iterator();
        while (componentIterator.hasNext()) {
            ((JComponent) componentIterator.next()).setEnabled(enabled);
        }

        update();
    }
}
