/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.debug.DebugUtils;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.resources.ImageResource;
import net.sf.freecol.common.resources.ResourceManager;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The menu that appears in debug mode.
 */
public class DebugMenu extends JMenu {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(DebugMenu.class.getName());

    private static final String ERROR_MESSAGE =
        "This is a long error message, indicating that some error has occurred. " +
        "This is a long error message, indicating that some error has occurred. " +
        "This is a long error message, indicating that some error has occurred.";

    private final FreeColClient freeColClient;


    /**
     * Create the debug menu.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
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

        final JCheckBoxMenuItem sc
            = Utility.localizedCheckBoxMenuItem("menuBar.debug.showCoordinates",
                FreeColDebugger.debugDisplayCoordinates());
        sc.setOpaque(false);
        sc.setMnemonic(KeyEvent.VK_S);
        this.add(sc);
        sc.addActionListener((ActionEvent ae) -> {
                boolean val = ((JCheckBoxMenuItem)ae.getSource()).isSelected();
                FreeColDebugger.setDebugDisplayCoordinates(val);
                gui.refresh();
            });
        sc.setEnabled(true);

        final JMenuItem reveal
            = Utility.localizedCheckBoxMenuItem("menuBar.debug.revealEntireMap",
                                                false);
        reveal.setOpaque(false);
        reveal.setMnemonic(KeyEvent.VK_R);
        this.add(reveal);
        reveal.addActionListener((ActionEvent ae) -> {
                DebugUtils.revealMap(freeColClient, true);
                reveal.setEnabled(false);
            });
        reveal.setEnabled(hasServer);

        final JMenuItem hide
            = Utility.localizedCheckBoxMenuItem("menuBar.debug.hideEntireMap",
                                                false);
        hide.setOpaque(false);
        //hide.setMnemonic(KeyEvent.VK_R);
        this.add(hide);
        hide.addActionListener((ActionEvent ae) -> {
                DebugUtils.revealMap(freeColClient, false);
                hide.setEnabled(false);
            });
        hide.setEnabled(hasServer);

        // Search tracing
        final JCheckBoxMenuItem searchTrace
            = Utility.localizedCheckBoxMenuItem("menuBar.debug.searchTrace",
                game.getMap().getSearchTrace());
        searchTrace.setOpaque(false);
        this.add(searchTrace);
        searchTrace.addActionListener((ActionEvent ae) -> {
                boolean val = ((JCheckBoxMenuItem)ae.getSource()).isSelected();
                game.getMap().setSearchTrace(val);
            });

        final JMenu cvpMenu
            = Utility.localizedMenu("menuBar.debug.showColonyValue");
        cvpMenu.setOpaque(false);
        ButtonGroup bg = new ButtonGroup();
        final JRadioButtonMenuItem cv1
            = Utility.localizedRadioButtonMenuItem(StringTemplate.template("none"),
                FreeColDebugger.debugDisplayColonyValuePlayer() == null);
        cv1.setOpaque(false);
        cv1.setMnemonic(KeyEvent.VK_C);
        cvpMenu.add(cv1);
        bg.add(cv1);
        cv1.addActionListener((ActionEvent ae) -> {
                FreeColDebugger.setDebugDisplayColonyValuePlayer(null);
                gui.refresh();
            });
        this.add(cvpMenu);
        cvpMenu.addSeparator();
        for (Player p : game.getLiveEuropeanPlayers(null)) {
            final JRadioButtonMenuItem cv2
                = Utility.localizedRadioButtonMenuItem(p.getCountryLabel(),
                    FreeColDebugger.debugDisplayColonyValuePlayer() == p);
            cv2.setOpaque(false);
            //cv2.setMnemonic(KeyEvent.VK_C);
            cvpMenu.add(cv2);
            bg.add(cv2);
            final Player fp = p;
            cv2.addActionListener((ActionEvent ae) -> {
                    FreeColDebugger.setDebugDisplayColonyValuePlayer(fp);
                    gui.refresh();
                });
        }

        this.addSeparator();

        final JMenuItem skipTurns = Utility.localizedMenuItem("menuBar.debug.skipTurns");
        skipTurns.setOpaque(false);
        skipTurns.setMnemonic(KeyEvent.VK_T);
        this.add(skipTurns);
        skipTurns.addActionListener((ActionEvent ae) -> {
                DebugUtils.skipTurns(freeColClient);
            });
        DebugUtils.addSkipChangeListener(freeColClient, this, skipTurns);
        skipTurns.setEnabled(hasServer);

        final JMenuItem addBuilding = Utility.localizedMenuItem("menuBar.debug.addBuilding");
        addBuilding.setOpaque(false);
        addBuilding.setMnemonic(KeyEvent.VK_B);
        this.add(addBuilding);
        addBuilding.addActionListener((ActionEvent ae) -> {
                DebugUtils.addBuildings(freeColClient, addBuilding.getText());
            });
        addBuilding.setEnabled(hasServer);

        final JMenuItem addFather = Utility.localizedMenuItem("menuBar.debug.addFoundingFather");
        addFather.setOpaque(false);
        addFather.setMnemonic(KeyEvent.VK_F);
        this.add(addFather);
        addFather.addActionListener((ActionEvent ae) -> {
                DebugUtils.addFathers(freeColClient, addFather.getText());
            });
        addFather.setEnabled(hasServer);

        final JMenuItem runMonarch = Utility.localizedMenuItem("menuBar.debug.runMonarch");
        runMonarch.setOpaque(false);
        runMonarch.setMnemonic(KeyEvent.VK_M);
        this.add(runMonarch);
        runMonarch.addActionListener((ActionEvent ae) -> {
                DebugUtils.setMonarchAction(freeColClient, runMonarch.getText());
            });
        runMonarch.setEnabled(hasServer);

        final JMenuItem addGold = Utility.localizedMenuItem("menuBar.debug.addGold");
        addGold.setOpaque(false);
        addGold.setMnemonic(KeyEvent.VK_G);
        this.add(addGold);
        addGold.addActionListener((ActionEvent ae) -> {
                DebugUtils.addGold(freeColClient);
            });
        addGold.setEnabled(hasServer);

        final JMenuItem addCrosses = Utility.localizedMenuItem("menuBar.debug.addImmigration");
        addCrosses.setOpaque(false);
        addCrosses.setMnemonic(KeyEvent.VK_I);
        this.add(addCrosses);
        addCrosses.addActionListener((ActionEvent ae) -> {
                DebugUtils.addImmigration(freeColClient);
            });
        addCrosses.setEnabled(hasServer);

        final JMenuItem giveBells = Utility.localizedMenuItem("menuBar.debug.addLiberty");
        giveBells.setOpaque(false);
        giveBells.setMnemonic(KeyEvent.VK_L);
        this.add(giveBells);
        giveBells.addActionListener((ActionEvent ae) -> {
                DebugUtils.addLiberty(freeColClient);
            });
        giveBells.setEnabled(hasServer);

        // random number generator
        final JMenuItem rng = Utility.localizedMenuItem("menuBar.debug.stepRandomNumberGenerator");
        rng.setOpaque(false);
        rng.setMnemonic(KeyEvent.VK_X);
        this.add(rng);
        rng.addActionListener((ActionEvent ae) -> {
                DebugUtils.stepRNG(freeColClient);
            });
        rng.setEnabled(hasServer);

        // Unit display
        final JMenuItem du = Utility.localizedMenuItem("menuBar.debug.displayUnits");
        du.setOpaque(false);
        this.add(du);
        du.addActionListener((ActionEvent ae) -> {
                DebugUtils.displayUnits(freeColClient);
            });
        du.setEnabled(true);

        this.addSeparator();

        final JMenu panelMenu = Utility.localizedMenu("menuBar.debug.displayPanels");
        panelMenu.setOpaque(false);

        final JMenuItem monarchDialog = Utility.localizedMenuItem("menuBar.debug.displayMonarchPanel");
        monarchDialog.addActionListener((ActionEvent ae) -> {
                gui.showMonarchDialog(
                    Monarch.MonarchAction.RAISE_TAX_WAR, null, player.getMonarchKey(),
                    (Boolean b) ->
                        freeColClient.getInGameController().monarchAction(Monarch.MonarchAction.RAISE_TAX_WAR, b));
            });
        panelMenu.add(monarchDialog);

        final JMenuItem errorMessage = Utility.localizedMenuItem("menuBar.debug.displayErrorMessage");
        errorMessage.addActionListener((ActionEvent ae) -> {
                gui.showErrorMessage(ERROR_MESSAGE);
            });
        panelMenu.add(errorMessage);

        this.add(panelMenu);

        final JMenuItem europeStatus = Utility.localizedMenuItem("menuBar.debug.displayEuropeStatus");
        europeStatus.setOpaque(false);
        europeStatus.setMnemonic(KeyEvent.VK_E);
        this.add(europeStatus);
        europeStatus.addActionListener((ActionEvent ae) -> {
                DebugUtils.displayEurope(freeColClient);
            });
        europeStatus.setEnabled(hasServer);

        final JCheckBoxMenuItem dam
            = Utility.localizedCheckBoxMenuItem("menuBar.debug.displayAIMissions",
                FreeColDebugger.debugShowMission());
        final JCheckBoxMenuItem dami
            = Utility.localizedCheckBoxMenuItem("menuBar.debug.displayAdditionalAIMissionInfo",
                FreeColDebugger.debugShowMissionInfo());
        dam.setOpaque(false);
        dam.setMnemonic(KeyEvent.VK_A);
        this.add(dam);
        dam.addActionListener((ActionEvent ae) -> {
                boolean val = ((JCheckBoxMenuItem)ae.getSource()).isSelected();
                FreeColDebugger.setDebugShowMission(val);
                dami.setEnabled(val);
                gui.refresh();
            });
        dam.setEnabled(true);

        final JMenuItem useAI = Utility.localizedMenuItem("menuBar.debug.useAI");
        useAI.setOpaque(false);
        useAI.setMnemonic(KeyEvent.VK_A);
        useAI.setAccelerator(KeyStroke.getKeyStroke('A',
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                | InputEvent.ALT_MASK));
        this.add(useAI);
        useAI.addActionListener((ActionEvent ae) -> {
                DebugUtils.useAI(freeColClient);
            });
        useAI.setEnabled(hasServer);

        dami.setOpaque(false);
        dami.setMnemonic(KeyEvent.VK_I);
        this.add(dami);
        dami.addActionListener((ActionEvent ae) -> {
                boolean val = ((JCheckBoxMenuItem)ae.getSource()).isSelected();
                FreeColDebugger.setDebugShowMissionInfo(val);
                dami.setEnabled(val);
                gui.refresh();
            });
        dami.setEnabled(FreeColDebugger.debugShowMissionInfo());

        this.addSeparator();

        final JMenuItem compareMaps = Utility.localizedMenuItem("menuBar.debug.compareMaps");
        compareMaps.setOpaque(false);
        //compareMaps.setMnemonic(KeyEvent.VK_C);
        compareMaps.setAccelerator(KeyStroke.getKeyStroke('C',
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
                | InputEvent.ALT_MASK));
        this.add(compareMaps);
        compareMaps.addActionListener((ActionEvent ae) -> {
                DebugUtils.checkDesyncAction(freeColClient);
            });
        compareMaps.setEnabled(hasServer);

        final JMenuItem showResourceKeys = Utility.localizedMenuItem("menuBar.debug.showResourceKeys");
        showResourceKeys.setOpaque(false);
        //showResourceKeys.setMnemonic(KeyEvent.VK_R);
        this.add(showResourceKeys);
        showResourceKeys.addActionListener((ActionEvent ae) -> {
                StringBuilder builder = new StringBuilder();
                Map<String, ImageResource> resources
                    = ResourceManager.getImageResources();
                for (Entry<String, ImageResource> en
                         : mapEntriesByKey(resources)) {
                    builder.append(en.getKey());
                    builder.append(" (");
                    builder.append(en.getValue().getCount());
                    builder.append(")");
                    builder.append("\n");
                }
                gui.showInformationMessage(builder.toString());
            });
        showResourceKeys.setEnabled(true);

        // statistics
        final JMenuItem statistics = Utility.localizedMenuItem("statistics");
        statistics.setOpaque(false);
        //statistics.setMnemonic(KeyEvent.VK_I);
        this.add(statistics);
        statistics.addActionListener((ActionEvent ae) -> {
                gui.showStatisticsPanel();
            });
        statistics.setEnabled(true);

        // garbage collector
        final JMenuItem gc = Utility.localizedMenuItem("menuBar.debug.memoryManager.gc");
        gc.setOpaque(false);
        //gc.setMnemonic(KeyEvent.VK_G);
        this.add(gc);
        gc.addActionListener((ActionEvent ae) -> {
                System.gc();
            });
        gc.setEnabled(true);

        this.addSeparator();
    }
}
