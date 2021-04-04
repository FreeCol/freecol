/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * A general panel for information display.
 *
 * Panel layout:
 * | ----------------------------|
 * |    skin image with person   |
 * | ----------------------------|
 * | images[i] | texts[i]        |
 * | ----------------------------|
 * |           | button (opt)    |
 * | ----------------------------|
 * |                    okButton |
 * | ----------------------------|
 *
 * Each group of images[i], texts[i] and the accompanying button (if
 * needed) is contained within an inner layout, wrapped in a layout
 * created by {@link #createLayout(FreeColClient)} which contains the
 * skin background image with the person at the top as well as the
 * okButton at the bottom.
 */
public class InformationPanel extends FreeColPanel {

    /** Standard dimensions for the inner panel. */
    private static final int BASE_WIDTH = 470, BASE_HEIGHT = 75;

    /** Number of text columns in the messages. */
    private static final int COLUMNS = 40;
    
    /** The skin for this panel. */
    private BufferedImage skin = null;

    
    /**
     * Creates an information panel that shows the given
     * texts and images, and an "OK" button.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param texts The texts to be displayed in the panel.
     * @param fcos The source {@code FreeColObject}s for the text.
     * @param images The images to be displayed in the panel.
     *
     * @see #createLayout(FreeColClient) For the outer layout
     */
    public InformationPanel(FreeColClient freeColClient, String[] texts,
                            FreeColObject[] fcos, ImageIcon[] images) {
        this(freeColClient, texts, fcos, images,
            ImageLibrary.getInformationPanelSkin(freeColClient.getMyPlayer()));
    }

    /**
     * Creates an information panel that shows the given texts and
     * images, and an "OK" button.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param texts The texts to be displayed in the panel.
     * @param fcos The source {@code FreeColObject}s for the text.
     * @param images The images to be displayed in the panel.
     * @param skin The background skin for the panel.
     */
    private InformationPanel(FreeColClient freeColClient, String[] texts,
                             FreeColObject[] fcos, ImageIcon[] images,
                             BufferedImage skin) {
        super(freeColClient, null, createLayout(skin));

        final GUI gui = getGUI();
        this.skin = skin;
        
        JPanel textPanel = new MigPanel(new MigLayout("wrap 2"));
        textPanel.setOpaque(false);
        for (int i = 0; i < texts.length; i++) {
            JTextArea txt = Utility.getDefaultTextArea(texts[i], COLUMNS);
            if (images[i] != null) {
                textPanel.add(new JLabel(images[i]));
                textPanel.add(txt);
            } else {
                textPanel.add(txt, "skip");
            }
            StringTemplate disp = displayLabel(fcos[i]);
            if (disp != null) {
                JButton button = Utility.localizedButton(StringTemplate
                    .template("informationPanel.display")
                    .addStringTemplate("%object%", disp));
                final FreeColObject fco = fcos[i];
                button.addActionListener((ActionEvent ae) -> {
                        gui.displayObject(fco);
                    });
                /*
                  If there is another text to display, we need to add
                  "gapbottom 25" into the .add(), which gives some
                  cushion between each text block
                */
                if ((i + 1) < texts.length) {
                    textPanel.add(button, "skip, gapbottom 25");
                } else {
                    textPanel.add(button, "skip");
                }
            }
        }

        JScrollPane scrollPane = new JScrollPane(textPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // make scroll pane opaque
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        this.add(scrollPane);
        this.add(okButton, "tag ok");
        this.setPreferredSize(new Dimension(skin.getWidth(), skin.getHeight()));
        this.setBorder(null);
    }

    /**
     * Creates the outer layout of the Information Panel.
     *
     * @param skin The skin to use for the panel.
     * @return A new {@code MigLayout} containing the outer layout.
     */
    private static MigLayout createLayout(BufferedImage skin) {
        // FIXME: 290 is magic
        // insets are the outer gaps top/left/bottom/right
        // h-290 skips over the image at the top of the standard skin
        int h = skin.getHeight();
        return new MigLayout("wrap 1, insets " + (h-290) + " 0 10 0");
    }

    /**
     * A label for an FCO that can meaningfully be displayed.
     *
     * @param fco The {@code FreeColObject} to check.
     * @return A {@code StringTemplate} label, or null if nothing found.
     */
    private StringTemplate displayLabel(FreeColObject fco) {
        return (fco instanceof Tile && ((Tile)fco).hasSettlement())
            ? displayLabel(((Tile)fco).getSettlement())

            : (fco instanceof Unit)
            ? displayLabel((FreeColObject)((Unit)fco).getLocation())

            : (fco instanceof Location)
            ? ((Location)fco).getLocationLabelFor(getMyPlayer())

            : null;
    }


    // Override JComponent

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(this.skin, 0, 0, this);
    }
}
