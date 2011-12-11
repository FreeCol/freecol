/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Collections;
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

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.MapViewer;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceDialog;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.MonarchPanel;
import net.sf.freecol.client.gui.panel.StatisticsPanel;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.Resource;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerPlayer;


public class DebugMenu extends JMenu {

    private FreeColClient freeColClient;

    private final Canvas canvas;

    private final MapViewer mapViewer;

    private GUI gui;

    private static final String ERROR_MESSAGE =
        "This is a long error message, indicating that some error has occurred. " +
        "This is a long error message, indicating that some error has occurred. " +
        "This is a long error message, indicating that some error has occurred.";


    public DebugMenu(FreeColClient fcc, GUI gui) {
        super(Messages.message("menuBar.debug"));

        this.freeColClient = fcc;
        
        this.gui = gui;

        mapViewer = gui.getMapViewer();
        canvas = gui.getCanvas();

        buildDebugMenu();
    }

    private void buildDebugMenu() {
        final Game game = freeColClient.getGame();
        final FreeColServer server = freeColClient.getFreeColServer();
        final Game serverGame = (server == null) ? null : server.getGame();
        final Player player = freeColClient.getMyPlayer();
        final Player serverPlayer = (server == null) ? null
            : (Player) serverGame.getFreeColGameObject(player.getId());

        this.setOpaque(false);
        this.setMnemonic(KeyEvent.VK_D);
        add(this);

        /*
        final JMenu debugFixMenu = new JMenu("Fixes");
        debugFixMenu.setOpaque(false);
        debugFixMenu.setMnemonic(KeyEvent.VK_F);
        this.add(debugFixMenu);

        // TODO: remove this one day unless someone remembers what this
        // bug was and whether it is still relevant.
        // 201107. Commenting out.

        final JMenuItem crossBug
            = new JCheckBoxMenuItem("Fix \"not enough crosses\"-bug");
        crossBug.setOpaque(false);
        crossBug.setMnemonic(KeyEvent.VK_B);
        debugFixMenu.add(crossBug);
        crossBug.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    player.updateImmigrationRequired();
                    Iterator<Player> pi = serverGame.getPlayerIterator();
                    while (pi.hasNext()) {
                        pi.next().updateImmigrationRequired();
                    }
                }
            });
        crossBug.setEnabled(server != null);

        this.addSeparator();
        */

        final JCheckBoxMenuItem sc
            = new JCheckBoxMenuItem(Messages.message("menuBar.debug.showCoordinates"),
                mapViewer.displayCoordinates);
        sc.setOpaque(false);
        sc.setMnemonic(KeyEvent.VK_S);
        this.add(sc);
        sc.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mapViewer.displayCoordinates
                        = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                    gui.refresh();
                }
            });
        sc.setEnabled(true);

        final JMenuItem reveal
            = new JCheckBoxMenuItem(Messages.message("menuBar.debug.revealEntireMap"));
        reveal.setOpaque(false);
        reveal.setMnemonic(KeyEvent.VK_R);
        this.add(reveal);
        reveal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    server.revealMapForAllPlayers();
                    reveal.setEnabled(false);
                    game.getSpecification()
                        .getBooleanOption(GameOptions.FOG_OF_WAR)
                        .setValue(false);
                }
            });
        reveal.setEnabled(server != null);

        final JMenu cvpMenu
            = new JMenu(Messages.message("menuBar.debug.showColonyValue"));
        cvpMenu.setOpaque(false);
        ButtonGroup bg = new ButtonGroup();
        final JRadioButtonMenuItem cv1
            = new JRadioButtonMenuItem("Do not display",
                !mapViewer.displayColonyValue);
        cv1.setOpaque(false);
        cv1.setMnemonic(KeyEvent.VK_C);
        cvpMenu.add(cv1);
        bg.add(cv1);
        cv1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mapViewer.displayColonyValue = false;
                    mapViewer.displayColonyValuePlayer = null;
                    gui.refresh();
                }
            });
        this.add(cvpMenu);

        final JRadioButtonMenuItem cv3
            = new JRadioButtonMenuItem(Messages.message("menuBar.debug.showCommonOutpostValue"),
                mapViewer.displayColonyValue
                && mapViewer.displayColonyValuePlayer == null);
        cv3.setOpaque(false);
        //cv3.setMnemonic(KeyEvent.VK_C);
        cvpMenu.add(cv3);
        bg.add(cv3);
        cv3.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mapViewer.displayColonyValue = true;
                    mapViewer.displayColonyValuePlayer = null;
                    gui.refresh();
                }
            });
        this.add(cvpMenu);

        cvpMenu.addSeparator();

        Iterator<Player> it = game.getPlayerIterator();
        while (it.hasNext()) {
            final Player p = it.next();
            if (p.isEuropean() && p.canBuildColonies()) {
                final JRadioButtonMenuItem cv2
                    = new JRadioButtonMenuItem(Messages.message(p.getNationName()),
                        mapViewer.displayColonyValue
                        && mapViewer.displayColonyValuePlayer == p);
                cv2.setOpaque(false);
                //cv2.setMnemonic(KeyEvent.VK_C);
                cvpMenu.add(cv2);
                bg.add(cv2);
                cv2.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            mapViewer.displayColonyValue = true;
                            mapViewer.displayColonyValuePlayer = p;
                            gui.refresh();
                        }
                    });
            }
        }

        this.addSeparator();

        final JMenuItem skipTurns
            = new JMenuItem(Messages.message("menuBar.debug.skipTurns"));
        skipTurns.setOpaque(false);
        skipTurns.setMnemonic(KeyEvent.VK_T);
        this.add(skipTurns);
        skipTurns.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    InGameController igc = server.getInGameController();
                    boolean isSkipping = igc.getSkippedTurns() > 0;
                    if (isSkipping) {
                        igc.setSkippedTurns(0);
                        return;
                    }
                    String response = canvas.showInputDialog(null,
                        StringTemplate.key("menuBar.debug.skipTurns"),
                        Integer.toString(10),
                        "ok", "cancel", true);
                    if (response == null) return;
                    int skip;
                    try {
                        skip = Integer.parseInt(response);
                    } catch (NumberFormatException nfe) {
                        skip = -1;
                    }
                    if (skip > 0) freeColClient.skipTurns(skip);
                }
            });
        this.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    boolean skipping = server.getInGameController()
                        .getSkippedTurns() > 0;
                    skipTurns.setText(Messages.message((skipping)
                            ? "menuBar.debug.stopSkippingTurns"
                            : "menuBar.debug.skipTurns"));
                }
            });
        skipTurns.setEnabled(server != null);

        final String buildingTitle
            = Messages.message("menuBar.debug.addBuilding");
        final JMenuItem addBuilding = new JMenuItem(buildingTitle);
        addBuilding.setOpaque(false);
        addBuilding.setMnemonic(KeyEvent.VK_B);
        this.add(addBuilding);
        addBuilding.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addBuildingAction(game, server, serverPlayer,
                        buildingTitle);
                }
            });
        addBuilding.setEnabled(server != null);

        final String fatherTitle
            = Messages.message("menuBar.debug.addFoundingFather");
        final JMenuItem addFather = new JMenuItem(fatherTitle);
        addFather.setOpaque(false);
        addFather.setMnemonic(KeyEvent.VK_F);
        this.add(addFather);
        addFather.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addFatherAction(game, server, serverPlayer, fatherTitle);
                }
            });
        addFather.setEnabled(server != null);

        final String monarchTitle
            = Messages.message("menuBar.debug.runMonarch");
        final JMenuItem runMonarch = new JMenuItem(monarchTitle);
        runMonarch.setOpaque(false);
        runMonarch.setMnemonic(KeyEvent.VK_M);
        this.add(runMonarch);
        runMonarch.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    List<ChoiceItem<MonarchAction>> actions
                        = new ArrayList<ChoiceItem<MonarchAction>>();
                    for (MonarchAction action : MonarchAction.values()) {
                        actions.add(new ChoiceItem<MonarchAction>(action));
                    }
                    ChoiceDialog<MonarchAction> choiceDialog
                        = new ChoiceDialog<MonarchAction>(freeColClient, canvas, monarchTitle,
                                                          "Cancel", actions);
                    MonarchAction action
                        = canvas.showFreeColDialog(choiceDialog);
                    server.getInGameController()
                        .setMonarchAction(serverPlayer, action);
                }
            });
        runMonarch.setEnabled(server != null);

        final JMenuItem addGold
            = new JMenuItem(Messages.message("menuBar.debug.addGold"));
        addGold.setOpaque(false);
        addGold.setMnemonic(KeyEvent.VK_G);
        this.add(addGold);
        addGold.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String response = canvas.showInputDialog(null,
                        StringTemplate.key("menuBar.debug.addGold"),
                        Integer.toString(1000), "ok", "cancel", true);
                    int gold;
                    try {
                        gold = Integer.parseInt(response);
                    } catch (NumberFormatException x) {
                        return;
                    }
                    player.modifyGold(gold);
                    serverPlayer.modifyGold(gold);
                }
            });
        addGold.setEnabled(server != null);

        final String immigrationTitle
            = Messages.message("menuBar.debug.addImmigration");
        final JMenuItem addCrosses = new JMenuItem(immigrationTitle);
        addCrosses.setOpaque(false);
        addCrosses.setMnemonic(KeyEvent.VK_I);
        this.add(addCrosses);
        addCrosses.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String response = canvas.showInputDialog(null,
                        StringTemplate.key("menuBar.debug.addImmigration"),
                        Integer.toString(100), "ok", "cancel", true);
                    int crosses;
                    try {
                        crosses = Integer.parseInt(response);
                    } catch (NumberFormatException x) {
                        return;
                    }
                    player.incrementImmigration(crosses);
                    serverPlayer.incrementImmigration(crosses);
                }
            });
        addCrosses.setEnabled(server != null);

        final JMenuItem giveBells
            = new JMenuItem(Messages.message("menuBar.debug.addLiberty"));
        giveBells.setOpaque(false);
        giveBells.setMnemonic(KeyEvent.VK_L);
        this.add(giveBells);
        giveBells.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String response = canvas.showInputDialog(null,
                        StringTemplate.key("menuBar.debug.addLiberty"),
                        Integer.toString(100), "ok", "cancel", true);
                    int liberty;
                    try {
                        liberty = Integer.parseInt(response);
                    } catch (NumberFormatException x) {
                        return;
                    }
                    for (Colony c : player.getColonies()) {
                        c.addLiberty(liberty);
                        ((Colony) serverGame.getFreeColGameObject(c.getId()))
                            .addLiberty(liberty);
                    }
                }
            });
        giveBells.setEnabled(server != null);

        // random number generator
        final JMenuItem rng
            = new JMenuItem(Messages.message("menuBar.debug.stepRandomNumberGenerator"));
        rng.setOpaque(false);
        rng.setMnemonic(KeyEvent.VK_X);
        this.add(rng);
        rng.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    InGameController igc = server.getInGameController();
                    boolean more = true;
                    while (more) {
                        int val = igc.stepRandom();
                        more = canvas.showConfirmDialog(null,
                            StringTemplate.template("menuBar.debug.randomValue")
                            .addAmount("%value%", val), "more", "ok");
                    }
                }
            });
        rng.setEnabled(server != null);

        this.addSeparator();

        final JMenu panelMenu
            = new JMenu(Messages.message("menuBar.debug.displayPanels"));
        panelMenu.setOpaque(false);

        final JMenuItem monarchPanel
            = new JMenuItem(Messages.message("menuBar.debug.displayMonarchPanel"));
        monarchPanel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canvas.showFreeColDialog(new MonarchPanel(freeColClient, canvas,
                            Monarch.MonarchAction.RAISE_TAX_WAR));
                }
            });
        panelMenu.add(monarchPanel);

        final JMenuItem victoryPanel
            = new JMenuItem(Messages.message("menuBar.debug.displayVictoryPanel"));
        victoryPanel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canvas.showVictoryPanel();
                }
            });
        panelMenu.add(victoryPanel);

        for (final Canvas.EventType eventType : Canvas.EventType.values()) {
            final JMenuItem mItem = new JMenuItem("Display " + eventType
                + " panel");
            mItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        canvas.showEventPanel(eventType);
                    }
                });
            panelMenu.add(mItem);
        }

        final JMenuItem errorMessage =
            new JMenuItem(Messages.message("menuBar.debug.displayErrorMessage"));
        errorMessage.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.errorMessage(ERROR_MESSAGE);
                }
            });
        panelMenu.add(errorMessage);

        this.add(panelMenu);

        final JMenuItem europeStatus
            = new JMenuItem(Messages.message("menuBar.debug.displayEuropeStatus"));
        europeStatus.setOpaque(false);
        europeStatus.setMnemonic(KeyEvent.VK_E);
        this.add(europeStatus);
        europeStatus.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    displayEuropeAction(serverGame, server.getAIMain());
                }
            });
        europeStatus.setEnabled(server != null);

        final JCheckBoxMenuItem dam
            = new JCheckBoxMenuItem("Display AI-missions",
                mapViewer.debugShowMission);
        final JCheckBoxMenuItem dami
            = new JCheckBoxMenuItem("Additional AI-mission info",
                mapViewer.debugShowMissionInfo);
        dam.setOpaque(false);
        dam.setMnemonic(KeyEvent.VK_A);
        this.add(dam);
        dam.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mapViewer.debugShowMission
                        = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                    dami.setEnabled(mapViewer.debugShowMission);
                    gui.refresh();
                }
            });
        dam.setEnabled(true);

        final JMenuItem useAI
            = new JMenuItem(Messages.message("menuBar.debug.useAI"));
        useAI.setOpaque(false);
        useAI.setMnemonic(KeyEvent.VK_A);
        useAI.setAccelerator(KeyStroke.getKeyStroke('A',
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                | InputEvent.ALT_MASK));
        this.add(useAI);
        useAI.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    AIMain aiMain = server.getAIMain();
                    AIPlayer ap = aiMain.getAIPlayer(player);
                    ap.setDebuggingConnection(freeColClient.getClient().getConnection());
                    ap.startWorking();
                    freeColClient.getConnectController().reconnect();
                }
            });
        useAI.setEnabled(server != null);

        dami.setOpaque(false);
        dami.setMnemonic(KeyEvent.VK_I);
        this.add(dami);
        dami.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    mapViewer.debugShowMissionInfo
                        = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                    gui.refresh();
                }
            });
        dami.setEnabled(mapViewer.debugShowMission);

        this.addSeparator();

        final JMenuItem compareMaps
            = new JMenuItem(Messages.message("menuBar.debug.compareMaps"));
        compareMaps.setOpaque(false);
        //compareMaps.setMnemonic(KeyEvent.VK_C);
        compareMaps.setAccelerator(KeyStroke.getKeyStroke('C',
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                | InputEvent.ALT_MASK));
        this.add(compareMaps);
        compareMaps.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    checkDesyncAction(serverGame, serverPlayer);
                }
            });
        compareMaps.setEnabled(server != null);

        final JMenuItem showResourceKeys
            = new JMenuItem(Messages.message("menuBar.debug.showResourceKeys"));
        showResourceKeys.setOpaque(false);
        //showResourceKeys.setMnemonic(KeyEvent.VK_R);
        this.add(showResourceKeys);
        showResourceKeys.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    java.util.Map<String, Resource> resources
                        = ResourceManager.getResources();
                    List<String> keys
                        = new ArrayList<String>(resources.keySet());
                    Collections.sort(keys);
                    StringBuilder builder = new StringBuilder();
                    for (String key : keys) {
                        builder.append(key);
                        Resource resource = resources.get(key);
                        if (resource instanceof ImageResource) {
                            ImageResource ir = (ImageResource) resource;
                            builder.append(" (");
                            builder.append(ir.getCount());
                            builder.append(")");
                        }
                        builder.append("\n");
                    }
                    canvas.showInformationMessage(builder.toString());
                }
            });
        showResourceKeys.setEnabled(true);

        // statistics
        final JMenuItem statistics
            = new JMenuItem(Messages.message("menuBar.debug.statistics"));
        statistics.setOpaque(false);
        //statistics.setMnemonic(KeyEvent.VK_I);
        this.add(statistics);
        statistics.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canvas.showSubPanel(new StatisticsPanel(freeColClient, canvas));
                }
            });
        statistics.setEnabled(true);

        // garbage collector
        final JMenuItem gc
            = new JMenuItem(Messages.message("menuBar.debug.memoryManager.gc"));
        gc.setOpaque(false);
        //gc.setMnemonic(KeyEvent.VK_G);
        this.add(gc);
        gc.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.gc();
                }
            });
        gc.setEnabled(true);

        this.addSeparator();
    }

    private void addBuildingAction(final Game game, final FreeColServer server,
                                   final Player serverPlayer,
                                   String buildingTitle) {
        List<ChoiceItem<BuildingType>> buildings
            = new ArrayList<ChoiceItem<BuildingType>>();
        for (BuildingType b : game.getSpecification().getBuildingTypeList()) {
            buildings.add(new ChoiceItem<BuildingType>(Messages.message(b.toString() + ".name"),
                    b));
        }
        BuildingType buildingType
            = canvas.showChoiceDialog(null, buildingTitle, "Cancel",
                buildings);
        if (buildingType == null) return;
        Game sGame = server.getGame();
        BuildingType sBuildingType = server.getSpecification()
            .getBuildingType(buildingType.getId());
        List<String> results = new ArrayList<String>();
        int fails = 0;
        for (Colony sColony : serverPlayer.getColonies()) {
            Colony.NoBuildReason reason = sColony.getNoBuildReason(sBuildingType);
            results.add(sColony.getName() + ": " + reason.toString());
            if (reason == Colony.NoBuildReason.NONE) {
                Building sBuilding = new ServerBuilding(sGame, sColony,
                    sBuildingType);
                sColony.addBuilding(sBuilding);
            } else {
                fails++;
            }
        }
        canvas.showInformationMessage(Utils.join(", ", results));
        if (fails < serverPlayer.getNumberOfSettlements()) {
            // Brutally resynchronize
            freeColClient.getConnectController().reconnect();
        }
    }

    private void addFatherAction(final Game game, final FreeColServer server,
                                 final Player serverPlayer,
                                 String fatherTitle) {
        List<ChoiceItem<FoundingFather>> fathers
            = new ArrayList<ChoiceItem<FoundingFather>>();
        for (FoundingFather father : game.getSpecification()
                 .getFoundingFathers()) {
            if (!serverPlayer.hasFather(father)) {
                ChoiceItem<FoundingFather> choice
                    = new ChoiceItem<FoundingFather>(Messages.message(father.getNameKey()),
                                                     father);
                fathers.add(choice);
            }
        }
        ChoiceDialog<FoundingFather> choiceDialog
            = new ChoiceDialog<FoundingFather>(freeColClient, canvas, fatherTitle, "Cancel",
                fathers);
        FoundingFather father = canvas.showFreeColDialog(choiceDialog);
        if (father != null) {
            server.getInGameController()
                .addFoundingFather((ServerPlayer) serverPlayer,
                    server.getGame().getSpecification()
                        .getFoundingFather(father.getId()));
        }
    }

    private void checkDesyncAction(final Game serverGame,
                                   final Player serverPlayer) {
        Game game = freeColClient.getGame();
        boolean problemDetected = false;
        Map serverMap = serverGame.getMap();
        for (Tile t : serverMap.getAllTiles()) {
            if (serverPlayer.canSee(t)) {
                Iterator<Unit> unitIterator = t.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit u = unitIterator.next();
                    if (u.isVisibleTo(serverPlayer)) {
                        if (game.getFreeColGameObject(u.getId()) == null) {
                            System.out.println("Desynchronization detected: Unit missing on client-side");
                            System.out.println(Messages.message(Messages.getLabel(u))
                                + "(" + u.getId() + "). Position: "
                                + u.getTile().getPosition());
                            try {
                                System.out.println("Possible unit on client-side: "
                                    + game.getMap().getTile(u.getTile().getPosition())
                                    .getFirstUnit().getId());
                            } catch (NullPointerException npe) {
                            }
                            System.out.println();
                            problemDetected = true;
                        } else {
                            Unit clientSideUnit = (Unit) game.getFreeColGameObject(u.getId());
                            if (clientSideUnit.getTile() != null
                                && !clientSideUnit.getTile().getId().equals(u.getTile().getId())) {
                                System.out.println("Unsynchronization detected: Unit located on different tiles");
                                System.out.println("Server: " + Messages.message(Messages.getLabel(u))
                                    + "(" + u.getId() + "). Position: "
                                    + u.getTile().getPosition());
                                System.out.println("Client: "
                                    + Messages.message(Messages.getLabel(clientSideUnit))
                                    + "(" + clientSideUnit.getId() + "). Position: "
                                    + clientSideUnit.getTile().getPosition());
                                System.out.println();
                                problemDetected = true;
                            }
                        }
                    }
                }
            }
        }

        canvas.showInformationMessage((problemDetected)
            ? "menuBar.debug.compareMaps.problem"
            : "menuBar.debug.compareMaps.checkComplete");
    }

    private void displayEuropeAction(Game serverGame, AIMain aiMain) {
        StringBuilder sb = new StringBuilder();
        for (Player tp : serverGame.getPlayers()) {
            Player p = (Player) serverGame.getFreeColGameObject(tp.getId());
            if (p.getEurope() == null) continue;
            List<Unit> inEurope = new ArrayList<Unit>();
            List<Unit> toEurope = new ArrayList<Unit>();
            List<Unit> toAmerica = new ArrayList<Unit>();
            LinkedHashMap<String,List<Unit>> units
                = new LinkedHashMap<String, List<Unit>>();
            units.put("To Europe", toEurope);
            units.put("In Europe", inEurope);
            units.put("To America", toAmerica);

            sb.append("\n==");
            sb.append(Messages.message(p.getNationName()));
            sb.append("==\n");

            for (Unit u : p.getEurope().getUnitList()) {
                if (u.getDestination() instanceof Map) {
                    toAmerica.add(u);
                    continue;
                }
                if (u.getDestination() instanceof Europe) {
                    toEurope.add(u);
                    continue;
                }
                inEurope.add(u);
            }

            for (String label : units.keySet()) {
                List<Unit> list = units.get(label);
                if (list.size() > 0){
                    sb.append("\n->" + label + "\n");
                    for (Unit u : list) {
                        sb.append('\n');
                        sb.append(Messages.message(Messages.getLabel(u)));
                        if (u.isUnderRepair()) {
                            sb.append(" (Repairing)");
                        } else {
                            sb.append("    ");
                            AIUnit aiu = aiMain.getAIUnit(u);
                            if (aiu.getMission() == null) {
                                sb.append(" (None)");
                            } else {
                                sb.append(aiu.getMission().toString()
                                    .replaceAll("\n", "    \n"));
                            }
                        }
                    }
                    sb.append('\n');
                }
            }
        }
        canvas.showInformationMessage(sb.toString());
    }
}
