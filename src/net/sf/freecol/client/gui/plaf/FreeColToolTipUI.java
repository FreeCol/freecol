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

package net.sf.freecol.client.gui.plaf;

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.UIManager;

import net.sf.freecol.client.gui.ImageLibrary;


/**
 * Draw the "background.FreeColToolTip" resource as a tiled background
 * image on tool tip popups.
 */
public class FreeColToolTipUI extends BasicToolTipUI {

    private static FreeColToolTipUI sharedInstance = new FreeColToolTipUI();

    private CellRendererPane rendererPane;
    private static JTextArea textArea;
    private static Border textAreaBorder
        = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    private static int maximumWidth = 300;


    public static void initialize() {
        String name = FreeColToolTipUI.class.getName();
        UIManager.put("ToolTipUI", name);
        UIManager.put(name, FreeColToolTipUI.class);
    }

    private FreeColToolTipUI() {
        super();
    }

    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        rendererPane = new CellRendererPane();
        c.add(rendererPane);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        c.remove(rendererPane);
        rendererPane = null;
    }


    /**
     * Describe <code>setMaximumWidth</code> method here.
     *
     * @param width an <code>int</code> value
     */
    public static void setMaximumWidth(int width){
        maximumWidth = width;
    }

    /**
     * Describe <code>setInsets</code> method here.
     *
     * @param width an <code>int</code> value
     */
    public static void setInsets(int width){
        textAreaBorder = BorderFactory.createEmptyBorder(width, width, width, width);
    }


    public void paint(Graphics g, JComponent c) {
        Dimension size = c.getSize();
        if (c.isOpaque()) {
            ImageLibrary.drawTiledImage("background.FreeColToolTip", g, c, null);
        }
        rendererPane.paintComponent(g, textArea, c, 1, 1,
                                    size.width - 1, size.height - 1, true);
    }

    public Dimension getPreferredSize(JComponent c) {
        String tipText = ((JToolTip)c).getTipText();
        if (tipText == null){
            return new Dimension(0,0);
        }

        textArea = new JTextArea(tipText);
        textArea.setBorder(textAreaBorder);
        textArea.setFont(UIManager.getFont("ToolTip"));
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(false);

        rendererPane.removeAll();
        rendererPane.add(textArea);

        if (maximumWidth > 0 && maximumWidth < textArea.getPreferredSize().getWidth()) {
            textArea.setLineWrap(true);
            textArea.setSize(maximumWidth, textArea.getPreferredSize().height);
        }
        return textArea.getPreferredSize();
    }

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }
}
