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

package net.sf.freecol.client.gui.menu;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Iterator;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Position;

public class DebugMenu extends JMenu {

    private FreeColClient freeColClient;

    private final Canvas canvas;

    private final GUI gui;


    public DebugMenu(FreeColClient fcc) {
        super(Messages.message("menuBar.debug"));

        this.freeColClient = fcc;

        gui = freeColClient.getGUI();
        canvas = freeColClient.getCanvas();

        buildDebugMenu();
    }

    private void buildDebugMenu() {

        this.setOpaque(false);
        this.setMnemonic(KeyEvent.VK_D);
        add(this);

        JMenu debugFixMenu = new JMenu("Fixes");
        debugFixMenu.setOpaque(false);
        debugFixMenu.setMnemonic(KeyEvent.VK_F);
        this.add(debugFixMenu);

        final JMenuItem crossBug = new JCheckBoxMenuItem("Fix \"not enough crosses\"-bug");
        crossBug.setOpaque(false);
        crossBug.setMnemonic(KeyEvent.VK_B);
        debugFixMenu.add(crossBug);
        crossBug.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                freeColClient.getMyPlayer().updateCrossesRequired();
                if (freeColClient.getFreeColServer() != null) {
                    Iterator<Player> pi = freeColClient.getFreeColServer().getGame().getPlayerIterator();
                    while (pi.hasNext()) {
                        pi.next().updateCrossesRequired();
                    }
                }
            }
        });

        this.addSeparator();

        JCheckBoxMenuItem sc = new JCheckBoxMenuItem(Messages.message("menuBar.debug.showCoordinates"),
                gui.displayCoordinates);
        sc.setOpaque(false);
        sc.setMnemonic(KeyEvent.VK_S);
        this.add(sc);
        sc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.displayCoordinates = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                canvas.refresh();
            }
        });

        final JCheckBoxMenuItem dami = new JCheckBoxMenuItem("Additional AI-mission info", gui.debugShowMissionInfo);
        dami.setOpaque(false);
        dami.setMnemonic(KeyEvent.VK_I);
        dami.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.debugShowMissionInfo = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                canvas.refresh();
            }
        });
        JCheckBoxMenuItem dam = new JCheckBoxMenuItem("Display AI-missions", gui.debugShowMission);
        dam.setOpaque(false);
        dam.setMnemonic(KeyEvent.VK_M);
        this.add(dam);
        dam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.debugShowMission = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                dami.setEnabled(gui.debugShowMission);
                canvas.refresh();
            }
        });
        this.add(dami);
        dami.setEnabled(gui.debugShowMission);

        final JMenuItem reveal = new JCheckBoxMenuItem(Messages.message("menuBar.debug.revealEntireMap"));
        reveal.setOpaque(false);
        reveal.setMnemonic(KeyEvent.VK_R);
        this.add(reveal);
        reveal.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (freeColClient.getFreeColServer() != null) {
                    freeColClient.getFreeColServer().revealMapForAllPlayers();
                }

                reveal.setEnabled(false);
            }
        });

        JMenu cvpMenu = new JMenu(Messages.message("menuBar.debug.showColonyValue"));
        cvpMenu.setOpaque(false);
        ButtonGroup bg = new ButtonGroup();
        JRadioButtonMenuItem cv1 = new JRadioButtonMenuItem("Do not display", !gui.displayColonyValue);
        cv1.setOpaque(false);
        cv1.setMnemonic(KeyEvent.VK_C);
        cvpMenu.add(cv1);
        bg.add(cv1);
        cv1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.displayColonyValue = false;
                gui.displayColonyValuePlayer = null;
                canvas.refresh();
            }
        });
        add(cvpMenu);
        JRadioButtonMenuItem cv3 = new JRadioButtonMenuItem("Common values", gui.displayColonyValue
                && gui.displayColonyValuePlayer == null);
        cv3.setOpaque(false);
        cv3.setMnemonic(KeyEvent.VK_C);
        cvpMenu.add(cv3);
        bg.add(cv3);
        cv3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gui.displayColonyValue = true;
                gui.displayColonyValuePlayer = null;
                canvas.refresh();
            }
        });
        this.add(cvpMenu);
        cvpMenu.addSeparator();
        Iterator<Player> it = freeColClient.getGame().getPlayerIterator();
        while (it.hasNext()) {
            final Player p = it.next();
            if (p.isEuropean() && p.canBuildColonies()) {
                JRadioButtonMenuItem cv2 = new JRadioButtonMenuItem(p.getNationAsString(),
                        gui.displayColonyValue && gui.displayColonyValuePlayer == p);
                cv2.setOpaque(false);
                cv2.setMnemonic(KeyEvent.VK_C);
                cvpMenu.add(cv2);
                bg.add(cv2);
                cv2.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        gui.displayColonyValue = true;
                        gui.displayColonyValuePlayer = p;
                        canvas.refresh();
                    }
                });
            }
        }

        this.addSeparator();

        final JMenuItem skipTurns = new JMenuItem("Skip turns");
        skipTurns.setOpaque(false);
        skipTurns.setMnemonic(KeyEvent.VK_S);
        this.add(skipTurns);
        skipTurns.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (freeColClient.getFreeColServer() != null) {
                    int skipTurns = Integer.parseInt(freeColClient.getCanvas().showInputDialog(
                            "How many turns should be skipped:", Integer.toString(10), "ok", "cancel"));
                    freeColClient.getFreeColServer().getInGameController().debugOnlyAITurns = skipTurns;
                    freeColClient.getInGameController().endTurn();
                }
            }
        });

        if (freeColClient.getFreeColServer() != null) {
            final JMenuItem giveBells = new JMenuItem("Adds 100 bells to each Colony");
            giveBells.setOpaque(false);
            giveBells.setMnemonic(KeyEvent.VK_B);
            this.add(giveBells);
            giveBells.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Iterator<Colony> ci = freeColClient.getMyPlayer().getColonyIterator();
                    while (ci.hasNext()) {
                        Colony c = ci.next();
                        c.addBells(100);

                        Colony sc = (Colony) freeColClient.getFreeColServer().getGame().getFreeColGameObject(c.getID());
                        sc.addBells(100);
                    }
                }
            });
        }

        this.addSeparator();

        final JMenuItem useAI = new JMenuItem("Use AI");
        useAI.setOpaque(false);
        useAI.setMnemonic(KeyEvent.VK_A);
        useAI.setAccelerator(KeyStroke.getKeyStroke('A', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                | InputEvent.ALT_MASK));
        this.add(useAI);
        useAI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (freeColClient.getFreeColServer() != null) {
                    net.sf.freecol.server.ai.AIMain aiMain = freeColClient.getFreeColServer().getAIMain();
                    net.sf.freecol.server.ai.AIPlayer ap = (net.sf.freecol.server.ai.AIPlayer) aiMain
                            .getAIObject(freeColClient.getMyPlayer().getID());
                    ap.setDebuggingConnection(freeColClient.getClient().getConnection());
                    ap.startWorking();
                    freeColClient.getConnectController().reconnect();
                }
            }
        });

        this.addSeparator();

        final JMenuItem compareMaps = new JMenuItem(Messages.message("menuBar.debug.compareMaps"));
        compareMaps.setOpaque(false);
        compareMaps.setMnemonic(KeyEvent.VK_C);
        compareMaps.setAccelerator(KeyStroke.getKeyStroke('C', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                | InputEvent.ALT_MASK));
        this.add(compareMaps);
        compareMaps.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean problemDetected = false;
                Map serverMap = freeColClient.getFreeColServer().getGame().getMap();
                Player myServerPlayer = (Player) freeColClient.getFreeColServer().getGame().getFreeColGameObject(
                        freeColClient.getMyPlayer().getID());

                Iterator<Position> it = serverMap.getWholeMapIterator();
                while (it.hasNext()) {
                    Tile t = serverMap.getTile(it.next());
                    if (myServerPlayer.canSee(t)) {
                        Iterator<Unit> unitIterator = t.getUnitIterator();
                        while (unitIterator.hasNext()) {
                            Unit u = unitIterator.next();
                            if (u.isVisibleTo(myServerPlayer)) {
                                if (freeColClient.getGame().getFreeColGameObject(u.getID()) == null) {
                                    System.out.println("Unsynchronization detected: Unit missing on client-side");
                                    System.out.println(u.getName() + "(" + u.getID() + "). Position: "
                                            + u.getTile().getPosition());
                                    try {
                                        System.out.println("Possible unit on client-side: "
                                                + freeColClient.getGame().getMap().getTile(u.getTile().getPosition())
                                                        .getFirstUnit().getID());
                                    } catch (NullPointerException npe) {
                                    }
                                    System.out.println();
                                    problemDetected = true;
                                } else {
                                    Unit clientSideUnit = (Unit) freeColClient.getGame()
                                            .getFreeColGameObject(u.getID());
                                    if (clientSideUnit.getTile() != null
                                            && !clientSideUnit.getTile().getID().equals(u.getTile().getID())) {
                                        System.out
                                                .println("Unsynchronization detected: Unit located on different tiles");
                                        System.out.println("Server: " + u.getName() + "(" + u.getID() + "). Position: "
                                                + u.getTile().getPosition());
                                        System.out.println("Client: " + clientSideUnit.getName() + "("
                                                + clientSideUnit.getID() + "). Position: "
                                                + clientSideUnit.getTile().getPosition());
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

        final JMenuItem gc = new JMenuItem("Run the garbage collector");
        gc.setOpaque(false);
        gc.setMnemonic(KeyEvent.VK_G);
        this.add(gc);
        gc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.gc();
            }
        });

        this.addSeparator();

        final JMenuItem loadResource = new JMenuItem("Load resource");
        loadResource.setOpaque(false);
        // loadResource.setMnemonic(KeyEvent.VK_A);
        // loadResource.setAccelerator(KeyStroke.getKeyStroke('A',
        // Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) |
        // InputEvent.ALT_MASK));
        this.add(loadResource);
        loadResource.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    FileFilter ff = new FileFilter() {
                        public boolean accept(File f) {
                            return true;
                        }

                        public String getDescription() {
                            return "resource filter";
                        }

                    };

                    File resourceFile = freeColClient.getCanvas().showLoadDialog(FreeCol.getSaveDirectory(),
                            new FileFilter[] { ff });
                    Messages.loadResources(resourceFile);
                } catch (Exception ex) {
                    System.out.println("Failed to load resource bundle");
                }
            }
        });

    }

}
