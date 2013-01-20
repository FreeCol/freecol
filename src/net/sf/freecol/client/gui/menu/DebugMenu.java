/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.Resource;
import net.sf.freecol.common.resources.ResourceManager;


public class DebugMenu extends JMenu {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(DebugMenu.class.getName());

    private FreeColClient freeColClient;

    private GUI gui;

    private static final String ERROR_MESSAGE =
        "This is a long error message, indicating that some error has occurred. " +
        "This is a long error message, indicating that some error has occurred. " +
        "This is a long error message, indicating that some error has occurred.";


    /**
     * Create the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> to use.
     */
    public DebugMenu(FreeColClient freeColClient) {
        super(Messages.message("menuBar.debug"));

        this.freeColClient = freeColClient;
        buildDebugMenu();
    }

    /**
     * Builds the debug menu.
     */
    private void buildDebugMenu() {
        final Game game = freeColClient.getGame();
        final GUI gui = freeColClient.getGUI();
        final boolean hasServer = freeColClient.getFreeColServer() != null;
        final Player player = freeColClient.getMyPlayer();

        this.setOpaque(false);
        this.setMnemonic(KeyEvent.VK_D);
        add(this);

        final JCheckBoxMenuItem sc = new JCheckBoxMenuItem(
            Messages.message("menuBar.debug.showCoordinates"),
            gui.getMapViewer().displayCoordinates);
        sc.setOpaque(false);
        sc.setMnemonic(KeyEvent.VK_S);
        this.add(sc);
        sc.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.getMapViewer().displayCoordinates
                        = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                    gui.refresh();
                }
            });
        sc.setEnabled(true);

        final JMenuItem reveal = new JCheckBoxMenuItem(
            Messages.message("menuBar.debug.revealEntireMap"));
        reveal.setOpaque(false);
        reveal.setMnemonic(KeyEvent.VK_R);
        this.add(reveal);
        reveal.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.revealMap(freeColClient, true);
                    reveal.setEnabled(false);
                }
            });
        reveal.setEnabled(hasServer);

        final JMenuItem hide = new JCheckBoxMenuItem(
            Messages.message("menuBar.debug.hideEntireMap"));
        hide.setOpaque(false);
        //hide.setMnemonic(KeyEvent.VK_R);
        this.add(hide);
        hide.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.revealMap(freeColClient, false);
                    hide.setEnabled(false);
                }
            });
        hide.setEnabled(hasServer);

        final JMenu cvpMenu = new JMenu(
            Messages.message("menuBar.debug.showColonyValue"));
        cvpMenu.setOpaque(false);
        ButtonGroup bg = new ButtonGroup();
        final JRadioButtonMenuItem cv1
            = new JRadioButtonMenuItem("Do not display",
                !gui.getMapViewer().displayColonyValue);
        cv1.setOpaque(false);
        cv1.setMnemonic(KeyEvent.VK_C);
        cvpMenu.add(cv1);
        bg.add(cv1);
        cv1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.getMapViewer().displayColonyValue = false;
                    gui.getMapViewer().displayColonyValuePlayer = null;
                    gui.refresh();
                }
            });
        this.add(cvpMenu);

        final JRadioButtonMenuItem cv3 = new JRadioButtonMenuItem(
            Messages.message("menuBar.debug.showCommonOutpostValue"),
            gui.getMapViewer().displayColonyValue
            && gui.getMapViewer().displayColonyValuePlayer == null);
        cv3.setOpaque(false);
        //cv3.setMnemonic(KeyEvent.VK_C);
        cvpMenu.add(cv3);
        bg.add(cv3);
        cv3.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.getMapViewer().displayColonyValue = true;
                    gui.getMapViewer().displayColonyValuePlayer = null;
                    gui.refresh();
                }
            });
        this.add(cvpMenu);

        cvpMenu.addSeparator();

        for (Player p : game.getLiveEuropeanPlayers()) {
            final JRadioButtonMenuItem cv2 = new JRadioButtonMenuItem(
                Messages.message(p.getNationName()),
                gui.getMapViewer().displayColonyValue
                && gui.getMapViewer().displayColonyValuePlayer == p);
            cv2.setOpaque(false);
            //cv2.setMnemonic(KeyEvent.VK_C);
            cvpMenu.add(cv2);
            bg.add(cv2);
            final Player fp = p;
            cv2.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        gui.getMapViewer().displayColonyValue = true;
                        gui.getMapViewer().displayColonyValuePlayer = fp;
                        gui.refresh();
                    }
                });
        }

        this.addSeparator();

        final JMenuItem skipTurns = new JMenuItem(
            Messages.message("menuBar.debug.skipTurns"));
        skipTurns.setOpaque(false);
        skipTurns.setMnemonic(KeyEvent.VK_T);
        this.add(skipTurns);
        skipTurns.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.skipTurns(freeColClient);
                }
            });
        DebugUtils.addSkipChangeListener(freeColClient, this, skipTurns);
        skipTurns.setEnabled(hasServer);

        final String buildingTitle
            = Messages.message("menuBar.debug.addBuilding");
        final JMenuItem addBuilding = new JMenuItem(buildingTitle);
        addBuilding.setOpaque(false);
        addBuilding.setMnemonic(KeyEvent.VK_B);
        this.add(addBuilding);
        addBuilding.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.addBuildings(freeColClient, buildingTitle);
                }
            });
        addBuilding.setEnabled(hasServer);

        final String fatherTitle
            = Messages.message("menuBar.debug.addFoundingFather");
        final JMenuItem addFather = new JMenuItem(fatherTitle);
        addFather.setOpaque(false);
        addFather.setMnemonic(KeyEvent.VK_F);
        this.add(addFather);
        addFather.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.addFathers(freeColClient, fatherTitle);
                }
            });
        addFather.setEnabled(hasServer);

        final String monarchTitle
            = Messages.message("menuBar.debug.runMonarch");
        final JMenuItem runMonarch = new JMenuItem(monarchTitle);
        runMonarch.setOpaque(false);
        runMonarch.setMnemonic(KeyEvent.VK_M);
        this.add(runMonarch);
        runMonarch.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.setMonarchAction(freeColClient, monarchTitle);
                }
            });
        runMonarch.setEnabled(hasServer);

        final String goldTitle = Messages.message("menuBar.debug.addGold");
        final JMenuItem addGold = new JMenuItem(goldTitle);
        addGold.setOpaque(false);
        addGold.setMnemonic(KeyEvent.VK_G);
        this.add(addGold);
        addGold.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.addGold(freeColClient);
                }
            });
        addGold.setEnabled(hasServer);

        final String immigrationTitle
            = Messages.message("menuBar.debug.addImmigration");
        final JMenuItem addCrosses = new JMenuItem(immigrationTitle);
        addCrosses.setOpaque(false);
        addCrosses.setMnemonic(KeyEvent.VK_I);
        this.add(addCrosses);
        addCrosses.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.addImmigration(freeColClient);
                }
            });
        addCrosses.setEnabled(hasServer);

        final JMenuItem giveBells
            = new JMenuItem(Messages.message("menuBar.debug.addLiberty"));
        giveBells.setOpaque(false);
        giveBells.setMnemonic(KeyEvent.VK_L);
        this.add(giveBells);
        giveBells.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.addLiberty(freeColClient);
                }
            });
        giveBells.setEnabled(hasServer);

        // random number generator
        final JMenuItem rng = new JMenuItem(
            Messages.message("menuBar.debug.stepRandomNumberGenerator"));
        rng.setOpaque(false);
        rng.setMnemonic(KeyEvent.VK_X);
        this.add(rng);
        rng.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.stepRNG(freeColClient);
                }
            });
        rng.setEnabled(hasServer);

        // Unit display
        final JMenuItem du = new JMenuItem(
            Messages.message("menuBar.debug.displayUnits"));
        du.setOpaque(false);
        this.add(du);
        du.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.displayUnits(freeColClient);
                }
            });
        du.setEnabled(true);

        this.addSeparator();

        final JMenu panelMenu
            = new JMenu(Messages.message("menuBar.debug.displayPanels"));
        panelMenu.setOpaque(false);

        final JMenuItem monarchPanel = new JMenuItem(
            Messages.message("menuBar.debug.displayMonarchPanel"));
        monarchPanel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.showMonarchPanelDialog(
                        Monarch.MonarchAction.RAISE_TAX_WAR, null);
                }
            });
        panelMenu.add(monarchPanel);

        final JMenuItem victoryPanel = new JMenuItem(
            Messages.message("menuBar.debug.displayVictoryPanel"));
        victoryPanel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.showVictoryPanel();
                }
            });
        panelMenu.add(victoryPanel);

        for (final Canvas.EventType eventType : Canvas.EventType.values()) {
            final JMenuItem mItem = new JMenuItem("Display " + eventType
                + " panel");
            mItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        gui.showEventPanel(eventType);
                    }
                });
            panelMenu.add(mItem);
        }

        final JMenuItem errorMessage = new JMenuItem(
            Messages.message("menuBar.debug.displayErrorMessage"));
        errorMessage.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.errorMessage(ERROR_MESSAGE);
                }
            });
        panelMenu.add(errorMessage);

        this.add(panelMenu);

        final JMenuItem europeStatus = new JMenuItem(
            Messages.message("menuBar.debug.displayEuropeStatus"));
        europeStatus.setOpaque(false);
        europeStatus.setMnemonic(KeyEvent.VK_E);
        this.add(europeStatus);
        europeStatus.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DebugUtils.displayEurope(freeColClient);
                }
            });
        europeStatus.setEnabled(hasServer);

        final JCheckBoxMenuItem dam
            = new JCheckBoxMenuItem("Display AI-missions",
                    gui.getMapViewer().debugShowMission);
        final JCheckBoxMenuItem dami
            = new JCheckBoxMenuItem("Additional AI-mission info",
                    gui.getMapViewer().debugShowMissionInfo);
        dam.setOpaque(false);
        dam.setMnemonic(KeyEvent.VK_A);
        this.add(dam);
        dam.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.getMapViewer().debugShowMission
                        = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                    dami.setEnabled(gui.getMapViewer().debugShowMission);
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
                    DebugUtils.useAI(freeColClient);
                }
            });
        useAI.setEnabled(hasServer);

        dami.setOpaque(false);
        dami.setMnemonic(KeyEvent.VK_I);
        this.add(dami);
        dami.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    gui.getMapViewer().debugShowMissionInfo
                        = ((JCheckBoxMenuItem) e.getSource()).isSelected();
                    gui.refresh();
                }
            });
        dami.setEnabled(gui.getMapViewer().debugShowMissionInfo);

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
                    DebugUtils.checkDesyncAction(freeColClient);
                }
            });
        compareMaps.setEnabled(hasServer);

        final JMenuItem showResourceKeys = new JMenuItem(
            Messages.message("menuBar.debug.showResourceKeys"));
        showResourceKeys.setOpaque(false);
        //showResourceKeys.setMnemonic(KeyEvent.VK_R);
        this.add(showResourceKeys);
        showResourceKeys.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Map<String, Resource> resources
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
                    gui.showInformationMessage(builder.toString());
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
                    gui.showStatisticsPanel();
                }
            });
        statistics.setEnabled(true);

        // garbage collector
        final JMenuItem gc = new JMenuItem(
            Messages.message("menuBar.debug.memoryManager.gc"));
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
}
