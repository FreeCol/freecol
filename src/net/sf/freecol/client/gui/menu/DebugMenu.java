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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.MonarchPanel;
import net.sf.freecol.client.gui.panel.StatisticsPanel;
import net.sf.freecol.client.gui.panel.VictoryPanel;
import net.sf.freecol.common.PseudoRandom;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.ai.AIUnit;

public class DebugMenu extends JMenu {

    private FreeColClient freeColClient;

    private final Canvas canvas;

    private final GUI gui;
    
    private JMenuItem skipTurnsMenuItem;


    public DebugMenu(FreeColClient fcc) {
        super(Messages.message("menuBar.debug"));

        this.freeColClient = fcc;

        gui = freeColClient.getGUI();
        canvas = freeColClient.getCanvas();

        buildDebugMenu();
    }

    private void stepRNG() {
        PseudoRandom clientRnd = freeColClient
            .getGame().getModelController().getPseudoRandom();
        PseudoRandom serverRnd = freeColClient.getFreeColServer()
            .getGame().getModelController().getPseudoRandom();
        boolean more = false;
        do {
            int cVal = clientRnd.nextInt(100);
            int sVal = serverRnd.nextInt(100);
            String value = Integer.toString(cVal) + ":" + Integer.toString(sVal);
            more = canvas.showConfirmDialog("menuBar.debug.stepRandomNumberGenerator",
                                            "more", "ok",
                                            "%value%", value);
        } while (more);
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
                freeColClient.getMyPlayer().updateImmigrationRequired();
                if (freeColClient.getFreeColServer() != null) {
                    Iterator<Player> pi = freeColClient.getFreeColServer().getGame().getPlayerIterator();
                    while (pi.hasNext()) {
                        pi.next().updateImmigrationRequired();
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
        JRadioButtonMenuItem cv3 = new JRadioButtonMenuItem("Common outpost value", gui.displayColonyValue
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

        setupSkipTurnsMenuItem();

        if (freeColClient.getFreeColServer() != null) {
            final JMenuItem giveBells = new JMenuItem("Adds 100 bells to each Colony");
            giveBells.setOpaque(false);
            giveBells.setMnemonic(KeyEvent.VK_B);
            this.add(giveBells);
            giveBells.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (Colony c : freeColClient.getMyPlayer().getColonies()) {
                        c.addLiberty(100);
                        Colony sc = (Colony) freeColClient.getFreeColServer().getGame().getFreeColGameObject(c.getId());
                        sc.addLiberty(100);
                    }
                }
            });
        }

        final JMenuItem addFather = new JMenuItem("Add Founding Father");
        addFather.setOpaque(false);
        addFather.setMnemonic(KeyEvent.VK_F);
        this.add(addFather);
        addFather.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Player player = freeColClient.getMyPlayer();
                    List<ChoiceItem<FoundingFather>> fathers = new ArrayList<ChoiceItem<FoundingFather>>();
                    for (FoundingFather father : FreeCol.getSpecification().getFoundingFathers()) {
                        if (!player.hasFather(father)) {
                            fathers.add(new ChoiceItem<FoundingFather>(father.getName(), father));
                        }
                    }
                    FoundingFather fatherToAdd = freeColClient.getCanvas()
                        .showChoiceDialog("Select Founding Father", "cancel", fathers);
                    player.addFather(fatherToAdd);
                    Player serverPlayer = (Player) freeColClient.getFreeColServer().getGame().
                        getFreeColGameObject(player.getId());
                    serverPlayer.addFather(fatherToAdd);
                }
            });

        final JMenuItem addCrosses = new JMenuItem("Add Immigration");
        addCrosses.setOpaque(false);
        // addCrosses.setMnemonic(KeyEvent.VK_????);
        this.add(addCrosses);
        addCrosses.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String response = freeColClient.getCanvas()
                        .showInputDialog("menuBar.debug.addImmigration",
                                         Integer.toString(100),
                                         "ok", "cancel");
                    Player player = freeColClient.getMyPlayer();
                    int crosses = Integer.parseInt(response);
                    Player serverPlayer = (Player) freeColClient.getFreeColServer()
                        .getGame().getFreeColGameObject(player.getId());
                    player.incrementImmigration(crosses);
                    serverPlayer.incrementImmigration(crosses);
                }
            });

        // random number generator
        final JMenuItem rng = new JMenuItem("Step random number generator");
        rng.setOpaque(false);
        rng.setMnemonic(KeyEvent.VK_X);
        this.add(rng);
        rng.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    stepRNG();
                }
            });

        this.addSeparator();

        JMenu panelMenu = new JMenu("Display panels");
        panelMenu.setOpaque(false);
        final JMenuItem monarchPanel = new JMenuItem("Display Monarch panel");
        monarchPanel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canvas.showFreeColDialog(new MonarchPanel(canvas, Monarch.MonarchAction.RAISE_TAX));
                }
            });
        panelMenu.add(monarchPanel);
        final JMenuItem victoryPanel = new JMenuItem("Display Victory panel");
        monarchPanel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canvas.showPanel(new VictoryPanel(canvas));
                }
            });
        panelMenu.add(victoryPanel);
        add(panelMenu);

        final JMenuItem europeStatus = new JMenuItem("Display Europe Status");
        europeStatus.setOpaque(false);
        europeStatus.setMnemonic(KeyEvent.VK_E);
        this.add(europeStatus);
        europeStatus.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (freeColClient.getFreeColServer() != null) {
                    net.sf.freecol.server.ai.AIMain aiMain = freeColClient.getFreeColServer().getAIMain();
                    StringBuilder sb = new StringBuilder();
                    for (Player tp : freeColClient.getGame().getPlayers()) {
                        final Player p = (Player) freeColClient.getFreeColServer().getGame().getFreeColGameObject(tp.getId());                        
                        if (p.getEurope() == null) {
                        	continue;
                        }
                        List<Unit> inEurope = new ArrayList<Unit>();
                        List<Unit> toEurope = new ArrayList<Unit>();
                        List<Unit> toAmerica = new ArrayList<Unit>();
                        LinkedHashMap<String,List<Unit>> units = new LinkedHashMap<String, List<Unit>>();
                        units.put("To Europe", toEurope);
                        units.put("In Europe", inEurope);
                        units.put("To America", toAmerica);
                        
                        sb.append("\n==");
                        sb.append(p.getNationAsString());
                        sb.append("==\n");

                        for(Unit u : p.getEurope().getUnitList()){
                        	if(u.getState() == UnitState.TO_AMERICA){
                        		toAmerica.add(u);
                        		continue;
                        	}
                        	if(u.getState() == UnitState.TO_EUROPE){
                        		toEurope.add(u);
                        		continue;
                        	}
                        	inEurope.add(u);
                        }
                        
                        for(String label : units.keySet()){
                        	List<Unit> list = units.get(label);
                        	if(list.size() > 0){
                        		sb.append("\n->" + label + "\n");
                            	for(Unit u : list){
                            		sb.append('\n');
                                    sb.append(u.getName());
                                    sb.append("    " + ((AIUnit) aiMain.getAIObject(u)).getMission().toString().replaceAll("\n", "    \n"));
                            	}
                            	sb.append('\n');
                        	}
                        }
                    }
                    canvas.showInformationMessage(sb.toString());
                }
            }
        });

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
                            .getAIObject(freeColClient.getMyPlayer().getId());
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
                        freeColClient.getMyPlayer().getId());

                Iterator<Position> it = serverMap.getWholeMapIterator();
                while (it.hasNext()) {
                    Tile t = serverMap.getTile(it.next());
                    if (myServerPlayer.canSee(t)) {
                        Iterator<Unit> unitIterator = t.getUnitIterator();
                        while (unitIterator.hasNext()) {
                            Unit u = unitIterator.next();
                            if (u.isVisibleTo(myServerPlayer)) {
                                if (freeColClient.getGame().getFreeColGameObject(u.getId()) == null) {
                                    System.out.println("Unsynchronization detected: Unit missing on client-side");
                                    System.out.println(u.getName() + "(" + u.getId() + "). Position: "
                                            + u.getTile().getPosition());
                                    try {
                                        System.out.println("Possible unit on client-side: "
                                                + freeColClient.getGame().getMap().getTile(u.getTile().getPosition())
                                                        .getFirstUnit().getId());
                                    } catch (NullPointerException npe) {
                                    }
                                    System.out.println();
                                    problemDetected = true;
                                } else {
                                    Unit clientSideUnit = (Unit) freeColClient.getGame()
                                            .getFreeColGameObject(u.getId());
                                    if (clientSideUnit.getTile() != null
                                            && !clientSideUnit.getTile().getId().equals(u.getTile().getId())) {
                                        System.out
                                                .println("Unsynchronization detected: Unit located on different tiles");
                                        System.out.println("Server: " + u.getName() + "(" + u.getId() + "). Position: "
                                                + u.getTile().getPosition());
                                        System.out.println("Client: " + clientSideUnit.getName() + "("
                                                + clientSideUnit.getId() + "). Position: "
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
        
        
        // statistics
        final JMenuItem statistics = new JMenuItem("Statistics");
        statistics.setOpaque(false);
        statistics.setMnemonic(KeyEvent.VK_I);
        this.add(statistics);
        statistics.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                canvas.showPanel(new StatisticsPanel(canvas));
            }
        });

        // garbage collector
        final JMenuItem gc = new JMenuItem(Messages.message("menuBar.debug.memoryManager.gc"));
        gc.setOpaque(false);
        gc.setMnemonic(KeyEvent.VK_G);
        this.add(gc);
        gc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.gc();
            }
        });

        this.addSeparator();

        final JMenuItem loadResource = new JMenuItem("Reload images");
        loadResource.setOpaque(false);
        this.add(loadResource);
        loadResource.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    canvas.getImageLibrary().init();
                } catch (Exception ex) {
                    System.out.println("Failed to reload images.");
                }
            }
        });

    }

	private void setupSkipTurnsMenuItem() {
		skipTurnsMenuItem = new JMenuItem("Skip turns");        
        skipTurnsMenuItem.setOpaque(false);
        skipTurnsMenuItem.setMnemonic(KeyEvent.VK_S);
        this.add(skipTurnsMenuItem);
        if (freeColClient.getFreeColServer() == null){
        	skipTurnsMenuItem.setEnabled(false);
        	return;
        }
        skipTurnsMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                	boolean isSkipping = freeColClient.getFreeColServer().getInGameController().debugOnlyAITurns != 0;
                    if(isSkipping){
                    	freeColClient.getFreeColServer().getInGameController().debugOnlyAITurns = 0;
                    	return;
                    }
                	
                	int skipTurns = Integer.parseInt(freeColClient.getCanvas().showInputDialog(
                            "How many turns should be skipped:", Integer.toString(10), "ok", "cancel"));
                    freeColClient.getFreeColServer().getInGameController().debugOnlyAITurns = skipTurns;
                    freeColClient.getInGameController().endTurn();
            }
        });
        this.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                boolean skippingTurns = freeColClient.getFreeColServer().getInGameController().debugOnlyAITurns != 0;
                String skipMenuItemStr = (skippingTurns)? "Stop skipping" : "Skip turns";
                skipTurnsMenuItem.setText(skipMenuItemStr);
            }
        });
	}

}
