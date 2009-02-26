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

package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.resources.ResourceManager;
import cz.autel.dmi.HIGLayout;

public class InformationDialog extends FreeColDialog<Boolean> {

    public static final int leftMargin = 10;
    public static final int rightMargin = 10;
    public static final int topMargin = 200;
    public static final int bottomMargin = 10;
    public static final int hGap = 10;
    public static final int vGap = 10;

    private JButton okButton = new JButton(Messages.message("ok"));

    private static Image bgImage = ResourceManager.getImage("InformationDialog.backgroundImage");

    /**
     * Returns an information dialog that shows the given 
     * texts and images, and an "OK" button.
     * 
     * @param text The text to be displayed in the dialog.
     * @param image The image to be displayed in the dialog.
     * @return An information dialog that shows the given text 
     *       and an "OK" button.
     */
    public InformationDialog(String text, ImageIcon image) {
        this(new String[] { text }, new ImageIcon[] { image });
    }

    /**
     * Returns an information dialog that shows the given 
     * texts and images, and an "OK" button.
     * 
     * @param texts The texts to be displayed in the dialog.
     * @param images The images to be displayed in the dialog.
     * @return An information dialog that shows the given text 
     *       and an "OK" button.
     */
    public InformationDialog(String[] texts, ImageIcon[] images) {

        int[] widths = {leftMargin, 510, rightMargin};
        int[] heights = {topMargin, 252, vGap, 30, bottomMargin};
        setLayout(new HIGLayout(widths, heights));

        int[] textWidths = {0, hGap, 0};
        int[] textHeights = new int[2 * texts.length - 1];
        int imageColumn = 1;
        int textColumn = 3;

        for (int index = 1; index < texts.length; index += 2) {
            textHeights[index] = margin;
        }

        if (images == null) {
            textWidths = new int[] {0};
            textColumn = 1;
        }

        enterPressesWhenFocused(okButton);

        HIGLayout layout = new HIGLayout(textWidths, textHeights);
        JPanel textPanel = new JPanel(layout);
        textPanel.setOpaque(false);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setResponse(Boolean.FALSE);
            }
        });

        int row = 1;
        for (int i = 0; i < texts.length; i++) {
            if (images != null && images[i] != null) {
                textPanel.add(new JLabel(images[i]), higConst.rc(row, imageColumn));
            }
            JTextArea textArea = getDefaultTextArea(texts[i]);
            textArea.setColumns(30);
            textPanel.add(textArea, higConst.rc(row, textColumn));
            row += 2;
        }

        JScrollPane scrollPane = new JScrollPane(textPanel,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // correct way to make scroll pane opaque
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        setBorder(null);

        add(scrollPane, higConst.rc(2, 2));
        add(okButton, higConst.rc(4, 2));
        setSize(getPreferredSize());

    }

    public void requestFocus() {
        okButton.requestFocus();
    }

    /**
     * Paints this component.
     * 
     * @param g The graphics context in which to paint.
     */
    public void paintComponent(Graphics g) {
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, this);
        } else {
            int width = getWidth();
            int height = getHeight();
            Image tempImage = ResourceManager.getImage("BackgroundImage");
            if (tempImage != null) {
                for (int x = 0; x < width; x += tempImage.getWidth(null)) {
                    for (int y = 0; y < height; y += tempImage.getHeight(null)) {
                        g.drawImage(tempImage, x, y, null);
                    }
                }
            } else {
                g.setColor(getBackground());
                g.fillRect(0, 0, width, height);
            }
        }
    }

}