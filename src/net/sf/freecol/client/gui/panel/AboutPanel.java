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

package net.sf.freecol.client.gui.panel;

import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This is the About panel
 *  
 * Layout:
 *  
 * | ---------------|
 * | apLogoLabel    |
 * | ---------------|
 * | apVersion      |
 * | ---------------|
 * | apRevision     |
 * | ---------------|
 * | apOfficialSite |
 * | ---------------|
 * | apSiteURL      |
 * | ---------------|
 * | apSFProject    |
 * | ---------------|
 * | apProjectURL   |
 * | ---------------|
 * | apLegal        |
 * | ---------------|
 * | apCopyright    |
 * | ---------------|
 * | okButton       |
 * | ---------------|
 * 
 */
public final class AboutPanel extends FreeColPanel {

    private static final Logger logger = Logger.getLogger(AboutPanel.class.getName());

    public static final String SITE_URL
        = "http://www.freecol.org";
    public static final String PROJECT_URL
        = "http://sourceforge.net/projects/freecol/";
    private static final String MANUAL_URL
        = "http://www.freecol.org/documentation/freecol-user-manual.html";


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public AboutPanel(FreeColClient freeColClient) {
        super(freeColClient, new MigLayout("wrap"));

        // Header with image
        Image tempImage = ResourceManager.getImage("image.flavor.Title");
        JLabel apLogoLabel = new JLabel(new ImageIcon(tempImage));
        apLogoLabel.setBorder(
            new CompoundBorder(new EmptyBorder(2,2,2,2),
                               new BevelBorder(BevelBorder.LOWERED)));
        add(apLogoLabel, "center");

        // Create available Font choices
        Font fontBold = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, Font.BOLD,
            getImageLibrary().getScaleFactor());
        Font fontNormal = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.TINY, getImageLibrary().getScaleFactor());
        
        // Version
        JLabel apVersion = Utility.localizedLabel("aboutPanel.version");
        apVersion.setFont(fontBold);
        JLabel apRevision = new JLabel(FreeCol.getRevision());
        apRevision.setFont(fontNormal);
        add(apVersion, "newline 20");
        add(apRevision, "newline");

        // Official Site Link
        JLabel apOfficialSite = new JLabel();
        apOfficialSite = Utility.localizedLabel("aboutPanel.officialSite");
        apOfficialSite.setFont(fontBold);
        add(apOfficialSite, "newline 10");
        JButton apSiteURL = Utility.getLinkButton(SITE_URL, null, SITE_URL);
        apSiteURL.addActionListener(this);
        apSiteURL.setFont(fontNormal);
        add(apSiteURL, "newline");

        // SourceForge Project Site Link
        JLabel apSFProject = new JLabel();
        apSFProject = Utility.localizedLabel("aboutPanel.sfProject");      
        apSFProject.setFont(fontBold);
        add(apSFProject, "newline 10");
        JButton apProjectURL = Utility.getLinkButton(PROJECT_URL, null, PROJECT_URL);
        apProjectURL.addActionListener(this);
        apProjectURL.setFont(fontNormal);
        add(apProjectURL, "newline");

        // Manual
        JLabel apManual = Utility.localizedLabel("aboutPanel.manual");
        apManual.setFont(fontBold);
        add(apManual, "newline 10");
        JButton apManualURL = Utility.getLinkButton(MANUAL_URL, null,
                                                    MANUAL_URL);
        apManualURL.addActionListener(this);
        add(apManualURL, "newline");
        
        // License Disclaimer
        JTextArea apLegal
            = Utility.localizedTextArea("aboutPanel.legalDisclaimer");
        apLegal.setFont(fontNormal);
        add(apLegal, "newline 20, width 300px");

        // Copyright
        JLabel apCopyright = Utility.localizedLabel("aboutPanel.copyright");
        apCopyright.setFont(fontNormal);
        add(apCopyright, "newline 10");

        add(okButton, "newline 20, tag ok");
    }

    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String url = ae.getActionCommand();
        if (SITE_URL.equals(url) || PROJECT_URL.equals(url)
            || MANUAL_URL.equals(url)) {
            String os = System.getProperty("os.name");
            // FIXME: move this to OS utilities
            String[] cmd = null;
            if (os == null) {
                // error, the operating system could not be determined
                return;
            } else if (os.toLowerCase().contains("mac")) {
                // Apple Macintosh, Safari is the main browser
                cmd = new String[] { "open" , "-a", "Safari", url };
            } else if (os.toLowerCase().contains("windows")) {
                // Microsoft Windows, use the default browser
                cmd = new String[] { "rundll32.exe",
                    "url.dll,FileProtocolHandler", url};
            } else if (os.toLowerCase().contains("linux")) {
                // GNU Linux, use xdg-utils to launch the default
                // browser (portland.freedesktop.org)
                cmd = new String[] { "xdg-open", url};
            } else {
                cmd = new String[] { "firefox", url};
            }
            try {
                Runtime.getRuntime().exec(cmd);
            } catch (IOException x) {
                // couldn't start browser
            }
        } else {
            super.actionPerformed(ae);
        }
    }
}
