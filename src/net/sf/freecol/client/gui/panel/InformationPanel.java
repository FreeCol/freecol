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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Location;
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
 * which contains the skin background image with the person at the top
 * as well as the okButton at the bottom.
 */
public class InformationPanel extends FreeColPanel {
    
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
     */
    public InformationPanel(FreeColClient freeColClient, String[] texts,
                            FreeColObject[] fcos, ImageIcon[] images) {
        super(freeColClient, null, new MigLayout());
        
        final ImageLibrary fixedImageLibrary = freeColClient.getGUI().getFixedImageLibrary();
        
        this.skin = fixedImageLibrary.getInformationPanelSkin(freeColClient.getMyPlayer());
        
        final float scaleFactor = fixedImageLibrary.getScaleFactor();
        final int topInset = fixedImageLibrary.getInformationPanelSkinTopInset(freeColClient.getMyPlayer());
        final int scaledTopInset = (int) (topInset * scaleFactor);
        final int gap = 10;
        
        getMigLayout().setLayoutConstraints("fill, wrap 1, insets 0 0 0 0");
        getMigLayout().setColumnConstraints(gap + "[grow]" + gap);
        getMigLayout().setRowConstraints(scaledTopInset + "px[grow]" + gap + "[]" + gap);

        final JPanel textPanel = createPanelWithAllContent(texts, fcos, images, gap);
        final JScrollPane scrollPane = new JScrollPane(textPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        add(scrollPane, "grow");
        add(okButton, "tag ok");
        setPreferredSize(new Dimension(skin.getWidth(), skin.getHeight()));
        setBorder(null);
        
        setEscapeAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                okButton.doClick();
            }
        });
    }

    private JPanel createPanelWithAllContent(String[] texts, FreeColObject[] fcos, ImageIcon[] images, final int gap) {
        JPanel textPanel = new MigPanel(new MigLayout("fill, wrap 2"));
        textPanel.setOpaque(false);
        for (int i = 0; i < texts.length; i++) {
            JTextArea txt = Utility.getDefaultTextArea(texts[i]);
            if (images[i] != null) {
                textPanel.add(new JLabel(images[i]));
                textPanel.add(txt, "gapleft " + gap + ", growx");
            } else {
                textPanel.add(txt, "skip, growx");
            }
            StringTemplate disp = displayLabel(fcos[i]);
            if (disp != null) {
                JButton button = Utility.localizedButton(StringTemplate
                    .template("informationPanel.display")
                    .addStringTemplate("%object%", disp));
                final FreeColObject fco = fcos[i];
                button.addActionListener((ActionEvent ae) -> {
                        getGUI().displayObject(fco);
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
        return textPanel;
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
