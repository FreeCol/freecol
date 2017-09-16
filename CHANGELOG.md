## FreeCol 0.12.0 (Unreleased) ##

### Developer issues ###

* **Java 8 is required to build FreeCol.**
* Resource keys for same images in different resolution can now be added to support higher resolution images when zooming or using --gui-scale option.
* All client-server interaction now *must* be asynchronous, as askExpecting() has been removed.
* DOM is gone, and there was much rejoicing.

### Common problems ###

* Java 7 is no longer receiving active support, therefore the previous version of FreeCol switched to using Java 8 which is the currently supported release.  This hopefully fixed the problems that many Mac users have been experiencing.  If FreeCol 0.12.0 fails to run, check first if your installed Java libraries are up to date.

#### Mac OS ####
Running FreeCol in Full-screen mode on OSX is known to be problematic and may not work.

### User-visible changes since 0.11.6-release ###

#### New Features ####

* A number of higher resolution images for units, goods and more have been added to the game.
* A unit may now be ordered to go to an unexplored tile, as long as there is an adjacent explored tile that the unit can reach (without requring transport).
* Display of European prices in several dialogs [IR#43](https://sourceforge.net/p/freecol/improvement-requests/43), from Brian Kim, Louise Zhang, Seongmin Park and Michael Jeffers,
* Move all goods with a hot key [IR#199](https://sourceforge.net/p/freecol/improvement-requests/199/) from Brian Kim, Louise Zhang, Seongmin Park and Michael Jeffers.
* Easier river editing in map editor through separate options for adding/removing and changing the river style.

#### Improvements ####

* Added tool tip to explain the last sale price annotation on the native settlement panel
* Added "About FreeCol" button to the opening menu screen [IR#15](https://sourceforge.net/p/freecol/improvement-requests/15/)
* Added option to disable / enable region naming dialog. [IR#222](https://sourceforge.net/p/freecol/improvement-requests/222)
* Images of native converts who become dragoons or scouts retain ethnicity, [BR#3086](https://sourceforge.net/p/freecol/bugs/3086).
* Game options added to adjust trade bonus for native alarm. [BR#3092](https://sourceforge.net/p/freecol/bugs/3092/)

#### Bug Fixes ####

* Fixed scout with goto-orders looping at lost city rumours, and broken messages following exploration of "strange mounds", [BR#2932](https://sourceforge.net/p/freecol/bugs/2932)
* "slowed by" message no longer reveals enemy privateer nation, [BR#2933](https://sourceforge.net/p/freecol/bugs/2933)
* The most recently arrived carrier is selected by default when the Europe panel is displayed, [BR#2941](https://sourceforge.net/p/freecol/bugs/2941)
* Various path finding fixes with middle-click gotos, [BR#2943](https://sourceforge.net/p/freecol/bugs/2943), [BR#2944](https://sourceforge.net/p/freecol/bugs/2944), [BR#2980](https://sourceforge.net/p/freecol/bugs/2980).
* Abandoning an improvement no longer corrupts the work of other pioneers also working on the same improvement, [BR#2946](https://sourceforge.net/p/freecol/bugs/2946).
* Trying to overfill a unit with goods in Europe now just does nothing (like other such cases) and does not provoke the "server can not do that" message [IR#214 but really a bug](https://sourceforge.net/p/freecol/improvement-requests/214/)
* Custom options were broken, again, [BR#2948](https://sourceforge.net/p/freecol/bugs/2948).
* Fixed bad "trade refused" message, [BR#2950](https://sourceforge.net/p/freecol/bugs/2950).
* deSoto and scouts could desynchronize the map, [BR#2953](https://sourceforge.net/p/freecol/bugs/2953).
* Bad turn number when sailing from Europe, [BR#2955](https://sourceforge.net/p/freecol/bugs/2955).
* Suppress bogus payment listing when dumping in Europe, [BR#2956](https://sourceforge.net/p/freecol/bugs/2956).
* Suppress bogus colony abandonment warning when spying, [BR#2957](https://sourceforge.net/p/freecol/bugs/2957).
* Drop carried goods when a brave is captured, [BR#2958](https://sourceforge.net/p/freecol/bugs/2958).
* Fix colony tile ownership inconsistency,  [BR#2963](https://sourceforge.net/p/freecol/bugs/2963).
* Clickable link problems, [BR#2965](https://sourceforge.net/p/freecol/bugs/2965).
* Correctly update after feature change (such as goods party), [BR#2966](https://sourceforge.net/p/freecol/bugs/2966).
* Problems using units that have exhausted their moves, [BR#2968](https://sourceforge.net/p/freecol/bugs/2968).
* Slight clarification of unattended production modifiers, [BR#2973](https://sourceforge.net/p/freecol/bugs/2973).
* Stop accumulating colonies in classic colony report, [BR#2974](https://sourceforge.net/p/freecol/bugs/2974).
* Many Col1-compatibility checks and bugfixes with tile and building production with help from Lone_Wolf, [BR#2978](https://sourceforge.net/p/freecol/bugs/2978), [BR#2979](https://sourceforge.net/p/freecol/bugs/2979), [BR#2981](https://sourceforge.net/p/freecol/bugs/2981).
* New versions of the national anthems and addition of the missing ones, from CalebRW.
* Problems with selecting from multiple wagons in the build queue [BR#2014](https://sourceforge.net/p/freecol/bugs/2014), fixed by Lars Willemsens.
* Problems with auto-save cleanup [BR#1307](https://sourceforge.net/p/freecol/bugs/1307), fixed by Lars Willemsens.
* Various fixes to the map editor including smaller dialog for changing river styles [BR#2926](https://sourceforge.net/p/freecol/bugs/2926).
* Layout of Warehouse Panel got improved to better fit inside the minimum supported resolution [BR#3033](https://sourceforge.net/p/freecol/bugs/3033), contributed by Jonas Stevnsvig.
* Seasoned Scouts no longer disappear at LSR's according to Col1 and no longer increase negative results. [BR#3093](https://sourceforge.net/p/freecol/bugs/3093/)
* Delivering gifts to Natives has a stronger effect on reducing native alarm. [BR#3092](https://sourceforge.net/p/freecol/bugs/3092/)


