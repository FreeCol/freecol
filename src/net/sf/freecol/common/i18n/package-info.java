/**
 * <h1>FreeCol Internationalisation package</h1>
 *
 * <p>This package contains the FreeCol support for internationalisation
 *     (translation).
 * <p>The {@link net.sf.freecol.common.i18n.Messages} class provides the API to
 *     the translations. Client code might include:
 * <p style="font-family: monospace; margin-left: 40px;">
 *     <span style="font-weight: bold; color: darkred;">new</span> AbstractAction( Messages.message(<span style="color: blue;">"cancel"</span>) )
 * </p>
 * ..whereupon the Messages class will look for a file called:
 * <p style="font-family: monospace; margin-left: 40px;">
 *     FreeColMessages<span style="color: green;">[_la[_CO]]</span>.properties
 * </p>
 * <p>..where <span style="font-family: monospace;">_la</span> and <span style="font-family: monospace;">_CO</span> are
 * the language and country codes from the locale. The most specific file will be loaded. With a United States locale,
 * <span style="font-family: monospace;">Messages</span> will
 * look for <span style="font-family: monospace;">FreeColMessages_en_US.properties</span> first, then
 * <span style="font-family: monospace;">FreeColMessages_en.properties</span> and finally
 * <span style="font-family: monospace;">FreeColMessages.properties</span> if neither of the other two are found.
 * <p><span style="font-family: monospace;">FreeColMessages.properties</span> contains a line like this:
 * <p style="font-family: monospace; margin-left: 40px;">
 *     cancel=Cancel
 * </p>
 * ..while <span style="font-family: monospace;">FreeColMessages_hu.properties</span> has a corresponding line:
 * <p style="font-family: monospace; margin-left: 40px;">
 *     cancel=M&#xe9;gse
 * </p>
 *
 * <h2>MessageMerge</h2>
 * <p>Only <span style="font-family: monospace;">FreeColMessages.properties</span> is updated as new messages are added
 *     so a tool is required for translators to keep their translations up to date when
 *     new messages are added.
 * <p>At the console, type:
 * <p style="font-family: monospace; margin-left: 40px;">
 *     ant compile-test<br>
 *     java&nbsp;-cp&nbsp;src/classes&nbsp;net.sf.freecol.common.i18n.MessageMerge&nbsp;<span style="color: red; ">data/strings/FreeColMessages.properties</span>&nbsp;data/strings/FreeColMessages_hu.properties
 * </p>
 * ..where the red message file is the one to merge from (typically the one shown
 * above) and the black message file is the one to merge to.
 * <p>Select the added messages from the left and press the [insert in right] button.
 * <p>To remove obsolete or over-zealously inserted messages, select a range from
 *     the right and press the [delete from right] button.
 * <p>Press the [save right] button to write the changes to the message file shown
 *     on the right hand side. Doing so will destroy your translation file without
 *     warning so ensure that the file on the right-hand side is committed to GIT
 *     before pressing the button or you risk losing your work.
 * <div style="text-align: center; font-size: small; color: darkgray;">
 *     $Revision$ $Date$
 * </div>
 *
 * @since 0.2.1 As part of the Client package
 */
package net.sf.freecol.common.i18n;