
package net.sf.freecol.client.gui;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ColopediaPanel;
import net.sf.freecol.client.gui.panel.ReportForeignAffairPanel;
import net.sf.freecol.client.gui.panel.ReportIndianPanel;
import net.sf.freecol.client.gui.panel.ReportLabourPanel;
import net.sf.freecol.client.gui.panel.ReportReligiousPanel;
import net.sf.freecol.client.gui.action.*;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.FreeCol;

import java.awt.geom.*;
import java.awt.*;
import javax.swing.*;

import java.awt.event.*;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;


/**
* The menu bar that is displayed on the top left corner of the <code>Canvas</code>.
* @see Canvas#setJMenuBar
*/
public class FreeColMenuBar extends JMenuBar {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int UNIT_ORDER_WAIT = 0;
    public static final int UNIT_ORDER_FORTIFY = 1;
    public static final int UNIT_ORDER_SENTRY = 2;
    public static final int UNIT_ORDER_CLEAR_ORDERS = 3;
    public static final int UNIT_ORDER_BUILD_COL = 5;
    public static final int UNIT_ORDER_PLOW = 6;
    public static final int UNIT_ORDER_BUILD_ROAD = 7;
    public static final int UNIT_ORDER_SKIP = 9;
    public static final int UNIT_ORDER_DISBAND = 11;
    
    private final FreeColClient freeColClient;
    private final Canvas canvas;
    private final GUI gui;

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

        ActionManager am = f.getActionManager();

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

        gameMenu.addSeparator();

        JMenuItem preferencesMenuItem = new JMenuItem(Messages.message("menuBar.game.preferences"));
        preferencesMenuItem.setOpaque(false);
        preferencesMenuItem.setMnemonic(KeyEvent.VK_P);
        preferencesMenuItem.setAccelerator(KeyStroke.getKeyStroke('P', InputEvent.CTRL_MASK));
        gameMenu.add(preferencesMenuItem);
        preferencesMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getCanvas().showClientOptionsDialog();
            }
        });

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

        final JMenuItem chatMenuItem = new JMenuItem(am.getFreeColAction(ChatAction.ID));
        chatMenuItem.setOpaque(false);
        gameMenu.add(chatMenuItem);

        final JMenuItem endTurnMenuItem = new JMenuItem(am.getFreeColAction(EndTurnAction.ID));
        endTurnMenuItem.setOpaque(false);
        gameMenu.add(endTurnMenuItem);
        
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

        final JCheckBoxMenuItem mcMenuItem = new JCheckBoxMenuItem(am.getFreeColAction(MapControlsAction.ID));
        mcMenuItem.setOpaque(false);
        mcMenuItem.setSelected(((MapControlsAction) am.getFreeColAction(MapControlsAction.ID)).isSelected());
        viewMenu.add(mcMenuItem);

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

        viewMenu.addSeparator();

        final JMenuItem europeMenuItem = new JMenuItem(am.getFreeColAction(EuropeAction.ID));
        europeMenuItem.setOpaque(false);
        viewMenu.add(europeMenuItem);

        // --> Orders
        JMenu ordersMenu = new JMenu(Messages.message("menuBar.orders"));
        ordersMenu.setOpaque(false);
        ordersMenu.setMnemonic(KeyEvent.VK_O);
        add(ordersMenu);

        final JMenuItem waitMenuItem = new JMenuItem(am.getFreeColAction(WaitAction.ID));
        waitMenuItem.setOpaque(false);
        ordersMenu.add(waitMenuItem);

        final JMenuItem fortifyMenuItem = new JMenuItem(am.getFreeColAction(FortifyAction.ID));
        fortifyMenuItem.setOpaque(false);
        ordersMenu.add(fortifyMenuItem);

        /*
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
        */

        ordersMenu.addSeparator();

        final JMenuItem colonyMenuItem = new JMenuItem(am.getFreeColAction(BuildColonyAction.ID));
        colonyMenuItem.setOpaque(false);
        ordersMenu.add(colonyMenuItem);

        final JMenuItem plowMenuItem = new JMenuItem(am.getFreeColAction(PlowAction.ID));
        plowMenuItem.setOpaque(false);
        ordersMenu.add(plowMenuItem);

        final JMenuItem roadMenuItem = new JMenuItem(am.getFreeColAction(BuildRoadAction.ID));
        roadMenuItem.setOpaque(false);
        ordersMenu.add(roadMenuItem);

        ordersMenu.addSeparator();


        final JMenuItem skipMenuItem = new JMenuItem(am.getFreeColAction(SkipUnitAction.ID));
        skipMenuItem.setOpaque(false);
        ordersMenu.add(skipMenuItem);

        final JMenuItem changeMenuItem = new JMenuItem(am.getFreeColAction(ChangeAction.ID));
        changeMenuItem.setOpaque(false);
        ordersMenu.add(changeMenuItem);

        final JMenuItem clearOrdersMenuItem = new JMenuItem(am.getFreeColAction(ClearOrdersAction.ID));
        clearOrdersMenuItem.setOpaque(false);
        ordersMenu.add(clearOrdersMenuItem);

        ordersMenu.addSeparator();

        final JMenuItem disbandMenuItem = new JMenuItem(am.getFreeColAction(DisbandUnitAction.ID));
        disbandMenuItem.setOpaque(false);
        ordersMenu.add(disbandMenuItem);


        // --> Report

        JMenu reportMenu = new JMenu(Messages.message("menuBar.report"));
        reportMenu.setOpaque(false);
        reportMenu.setMnemonic(KeyEvent.VK_R);
        add(reportMenu);

        JMenuItem religionMenuItem = new JMenuItem(Messages.message("menuBar.report.religion"));
        religionMenuItem.setOpaque(false);
        religionMenuItem.setMnemonic(KeyEvent.VK_R);
        //religionMenuItem.setAccelerator(KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK));
        reportMenu.add(religionMenuItem);
        religionMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showReportPanel(ReportReligiousPanel.class.getName());
            }
        });

        JMenuItem labourMenuItem = new JMenuItem(Messages.message("menuBar.report.labour"));
        labourMenuItem.setOpaque(false);
        labourMenuItem.setMnemonic(KeyEvent.VK_L);
        //labourMenuItem.setAccelerator(KeyStroke.getKeyStroke('L', InputEvent.CTRL_MASK));
        reportMenu.add(labourMenuItem);
        labourMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showReportPanel(ReportLabourPanel.class.getName());
            }
        });

        if (freeColClient.isSingleplayer()) {
            JMenuItem foreignMenuItem = new JMenuItem(Messages.message("menuBar.report.foreign"));
            foreignMenuItem.setOpaque(false);
            foreignMenuItem.setMnemonic(KeyEvent.VK_F);
            //foreignMenuItem.setAccelerator(KeyStroke.getKeyStroke('L', InputEvent.CTRL_MASK));
            reportMenu.add(foreignMenuItem);
            foreignMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canvas.showReportPanel(ReportForeignAffairPanel.class.getName());
                }
            });

            JMenuItem indianMenuItem = new JMenuItem(Messages.message("menuBar.report.indian"));
            indianMenuItem.setOpaque(false);
            indianMenuItem.setMnemonic(KeyEvent.VK_I);
            //indianMenuItem.setAccelerator(KeyStroke.getKeyStroke('I', InputEvent.CTRL_MASK));
            reportMenu.add(indianMenuItem);
            indianMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canvas.showReportPanel(ReportIndianPanel.class.getName());
                }
            });
        }

        // --> Colopedia

        JMenu colopediaMenu = new JMenu(Messages.message("menuBar.colopedia"));
        colopediaMenu.setOpaque(false);
        colopediaMenu.setMnemonic(KeyEvent.VK_C);
        add(colopediaMenu);

        JMenuItem terrainMenuItem = new JMenuItem(Messages.message("menuBar.colopedia.terrain"));
        terrainMenuItem.setOpaque(false);
        terrainMenuItem.setMnemonic(KeyEvent.VK_T);
        //terrainMenuItem.setAccelerator(KeyStroke.getKeyStroke('T', InputEvent.CTRL_MASK));
        colopediaMenu.add(terrainMenuItem);
        terrainMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showColopediaPanel(ColopediaPanel.COLOPEDIA_TERRAIN);
            }
        });

        JMenuItem unitMenuItem = new JMenuItem(Messages.message("menuBar.colopedia.unit"));
        unitMenuItem.setOpaque(false);
        unitMenuItem.setMnemonic(KeyEvent.VK_U);
        //unitMenuItem.setAccelerator(KeyStroke.getKeyStroke('U', InputEvent.CTRL_MASK));
        colopediaMenu.add(unitMenuItem);
        unitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showColopediaPanel(ColopediaPanel.COLOPEDIA_UNIT);
            }
        });

        JMenuItem goodsMenuItem = new JMenuItem(Messages.message("menuBar.colopedia.goods"));
        goodsMenuItem.setOpaque(false);
        goodsMenuItem.setMnemonic(KeyEvent.VK_G);
        //goodsMenuItem.setAccelerator(KeyStroke.getKeyStroke('G', InputEvent.CTRL_MASK));
        colopediaMenu.add(goodsMenuItem);
        goodsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showColopediaPanel(ColopediaPanel.COLOPEDIA_GOODS);
            }
        });

        JMenuItem skillMenuItem = new JMenuItem(Messages.message("menuBar.colopedia.skill"));
        skillMenuItem.setOpaque(false);
        skillMenuItem.setMnemonic(KeyEvent.VK_S);
        //buildingMenuItem.setAccelerator(KeyStroke.getKeyStroke('S', InputEvent.CTRL_MASK));
        colopediaMenu.add(skillMenuItem);
        skillMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showColopediaPanel(ColopediaPanel.COLOPEDIA_SKILLS);
            }
        });

        JMenuItem buildingMenuItem = new JMenuItem(Messages.message("menuBar.colopedia.building"));
        buildingMenuItem.setOpaque(false);
        buildingMenuItem.setMnemonic(KeyEvent.VK_B);
        //buildingMenuItem.setAccelerator(KeyStroke.getKeyStroke('B', InputEvent.CTRL_MASK));
        colopediaMenu.add(buildingMenuItem);
        buildingMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showColopediaPanel(ColopediaPanel.COLOPEDIA_BUILDING);
            }
        });

        JMenuItem fatherMenuItem = new JMenuItem(Messages.message("menuBar.colopedia.father"));
        fatherMenuItem.setOpaque(false);
        fatherMenuItem.setMnemonic(KeyEvent.VK_F);
        //fatherMenuItem.setAccelerator(KeyStroke.getKeyStroke('F', InputEvent.CTRL_MASK));
        colopediaMenu.add(fatherMenuItem);
        fatherMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showColopediaPanel(ColopediaPanel.COLOPEDIA_FATHER);
            }
        });

        // --> Debug
        if (FreeCol.isInDebugMode()) {
            JMenu debugMenu = new JMenu(Messages.message("menuBar.debug"));
            debugMenu.setOpaque(false);
            debugMenu.setMnemonic(KeyEvent.VK_D);
            add(debugMenu);

            JCheckBoxMenuItem sc = new JCheckBoxMenuItem(Messages.message("menuBar.debug.showCoordinates"));
            sc.setOpaque(false);
            sc.setMnemonic(KeyEvent.VK_S);
            debugMenu.add(sc);
            sc.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.displayCoordinates = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                    canvas.refresh();
                }
            });

            JCheckBoxMenuItem cv = new JCheckBoxMenuItem(Messages.message("menuBar.debug.showColonyValue"));
            cv.setOpaque(false);
            cv.setMnemonic(KeyEvent.VK_C);
            debugMenu.add(cv);
            cv.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.displayColonyValue = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                    canvas.refresh();
                }
            });

            final JMenuItem reveal = new JCheckBoxMenuItem(Messages.message("menuBar.debug.revealEntireMap"));
            reveal.setOpaque(false);
            reveal.setMnemonic(KeyEvent.VK_R);
            debugMenu.add(reveal);
            reveal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (freeColClient.getFreeColServer() != null) {
                        freeColClient.getFreeColServer().revealMapForAllPlayers();
                    }

                    reveal.setEnabled(false);
                }
            });

            final JMenuItem crossBug = new JCheckBoxMenuItem("Fix \"not enough crosses\"-bug");
            crossBug.setOpaque(false);
            crossBug.setMnemonic(KeyEvent.VK_B);
            debugMenu.add(crossBug);
            crossBug.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    freeColClient.getMyPlayer().updateCrossesRequired();
                    if (freeColClient.getFreeColServer() != null) {
                        Iterator pi = freeColClient.getFreeColServer().getGame().getPlayerIterator();
                        while (pi.hasNext()) {
                            ((Player) pi.next()).updateCrossesRequired();
                        }
                    }
                }
            });

            debugMenu.addSeparator();

            final JMenuItem useAI = new JMenuItem("Use AI");
            useAI.setOpaque(false);
            useAI.setMnemonic(KeyEvent.VK_A);
            useAI.setAccelerator(KeyStroke.getKeyStroke('A', InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
            debugMenu.add(useAI);
            useAI.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (freeColClient.getFreeColServer() != null) {
                        net.sf.freecol.server.ai.AIMain am = freeColClient.getFreeColServer().getAIMain();
                        net.sf.freecol.server.ai.AIPlayer ap = (net.sf.freecol.server.ai.AIPlayer) am.getAIObject(freeColClient.getMyPlayer().getID());
                        ap.setDebuggingConnection(freeColClient.getClient().getConnection());
                        ap.startWorking();
                        //freeColClient.getConnectController().reconnect();
                    }
                }
            });

            debugMenu.addSeparator();

            final JMenuItem compareMaps = new JMenuItem(Messages.message("menuBar.debug.compareMaps"));
            compareMaps.setOpaque(false);
            compareMaps.setMnemonic(KeyEvent.VK_C);
            compareMaps.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
            debugMenu.add(compareMaps);
            compareMaps.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    boolean problemDetected = false;
                    Map serverMap = freeColClient.getFreeColServer().getGame().getMap();
                    Player myServerPlayer = (Player) freeColClient.getFreeColServer().getGame().getFreeColGameObject(freeColClient.getMyPlayer().getID());
                    
                    Iterator it = serverMap.getWholeMapIterator();
                    while (it.hasNext()) {
                        Tile t = serverMap.getTile((Map.Position) it.next());
                        if (myServerPlayer.canSee(t)) {
                            Iterator unitIterator = t.getUnitIterator();
                            while (unitIterator.hasNext()) {
                                Unit u = (Unit) unitIterator.next();
                                if (u.isVisibleTo(myServerPlayer)) {
                                    if (freeColClient.getGame().getFreeColGameObject(u.getID()) == null) {
                                        System.out.println("Unsynchronization detected: Unit missing on client-side");
                                        System.out.println(u.getName() + "(" + u.getID() + "). Position: " + u.getTile().getPosition());
                                        try {
                                            System.out.println("Possible unit on client-side: " + freeColClient.getGame().getMap().getTile(u.getTile().getPosition()).getFirstUnit().getID());
                                        } catch (NullPointerException npe) {}
                                        System.out.println();
                                        problemDetected = true;
                                    } else {
                                        Unit clientSideUnit = (Unit) freeColClient.getGame().getFreeColGameObject(u.getID());
                                        if (!clientSideUnit.getTile().getID().equals(u.getTile().getID())) {
                                            System.out.println("Unsynchronization detected: Unit located on different tiles");
                                            System.out.println("Server: " + u.getName() + "(" + u.getID() + "). Position: " + u.getTile().getPosition());
                                            System.out.println("Client: " + clientSideUnit.getName() + "(" + clientSideUnit.getID() + "). Position: " + clientSideUnit.getTile().getPosition());
                                            System.out.println();
                                            problemDetected = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (problemDetected) {
                        canvas.showInformationMessage("menuBar.debug.compareMaps.problem");
                    } else {
                        canvas.showInformationMessage("menuBar.debug.compareMaps.checkComplete");
                    }
                }
            });
        }
        
        update();
    }


    /**
    * Updates this <code>FreeColMenuBar</code>.
    */
    public void update() {
        if (!freeColClient.getGUI().isInGame()) {
            return;
        }

        saveMenuItem.setEnabled(freeColClient.getMyPlayer().isAdmin() && freeColClient.getFreeColServer() != null);
    }


    /**
    * When a <code>FreeColMenuBar</code> is disabled, it does
    * not show the "in game options".
    */
    public void setEnabled(boolean enabled) {
        // Not implemented (and possibly not needed).

        update();
    }


    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        String displayString =  "Gold: " + freeColClient.getMyPlayer().getGold() + "    |    Year: " + freeColClient.getGame().getTurn().toString();
        Rectangle2D displayStringBounds = g.getFontMetrics().getStringBounds(displayString, g);
        g.drawString(displayString, getWidth()-10-(int)displayStringBounds.getWidth(), 15);
    }
}
