/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.action.AboutAction;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.action.ContinueAction;
import net.sf.freecol.client.gui.action.MapEditorAction;
import net.sf.freecol.client.gui.action.NewAction;
import net.sf.freecol.client.gui.action.OpenAction;
import net.sf.freecol.client.gui.action.PreferencesAction;
import net.sf.freecol.client.gui.action.QuitAction;
import net.sf.freecol.client.gui.panel.FreeColButton.ButtonStyle;
import net.sf.freecol.common.io.FreeColDirectories;


/**
 * The initial panel where the user chooses from the main modes of operation.
 */
public final class MainPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(MainPanel.class.getName());


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public MainPanel(FreeColClient freeColClient) {
        super(freeColClient, null,
                new MigLayout("wrap 1, insets 32px 32px 32px 32px, gap 0", "[center]"));

        boolean canContinue = FreeColDirectories
            .getLastSaveGameFile() != null;

        ActionManager am = getFreeColClient().getActionManager();
        JButton newButton = createImportantButton(am.getFreeColAction(NewAction.id));
        JButton openButton = createImportantButton(am.getFreeColAction(OpenAction.id));
        JButton mapEditorButton = createImportantButton(am.getFreeColAction(MapEditorAction.id));
        JButton optionsButton = createImportantButton(am.getFreeColAction(PreferencesAction.id));
        JButton aboutButton = createImportantButton(am.getFreeColAction(AboutAction.id));
        JButton quitButton = createImportantButton(am.getFreeColAction(QuitAction.id));

        okButton.setAction(am.getFreeColAction((canContinue)
                ? ContinueAction.id
                : NewAction.id));

        JLabel logoLabel = new JLabel(new ImageIcon(ImageLibrary
                .getUnscaledImage("image.flavor.Title")));

        add(logoLabel);
        
        final JLabel versionLabel = new JLabel(FreeCol.getVersion());
        versionLabel.setForeground(new Color(180, 162, 66)); // b4a242
        add(versionLabel);

        final MigPanel buttons = new MigPanel(new MigLayout("wrap 2"));
        buttons.setOpaque(false);
        buttons.add(okButton, "grow");
        if (canContinue) buttons.add(newButton, "grow");
        buttons.add(openButton, "grow");
        buttons.add(mapEditorButton, "grow");
        buttons.add(optionsButton, "grow");
        buttons.add(aboutButton, "grow");
        buttons.add(quitButton, "grow, span");

        add(buttons, "gapy 16px");
        
        setSize(getPreferredSize());
    }
    
    private JButton createImportantButton(Action action) {
        final FreeColButton button = new FreeColButton(ButtonStyle.IMPORTANT, action);
        return button;
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        // The actions are handled implicitly by the JButton/FreeColActions
        getGUI().removeComponent(this);
    }
}
