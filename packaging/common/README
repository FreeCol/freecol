            FREECOL

An Open Source Colonization Clone
=================================

I.   Requirements
II.  Compiling FreeCol
III. Running FreeCol
IV.  Troubleshooting
V.   Help, Feedback & Bug Reports
VI.  About


I.   Requirements
=================================
The program is written in Java. It can be ran with any Java
Runtime Environment (JRE) that is at least version 1.8.
The JRE (or JVM) can be downloaded from:
http://java.sun.com/getjava/download.html

In order to compile the game, you'll also need Apache Ant,
get it at:
http://ant.apache.org

This process requires the 'native2ascii' task, which is an optional ant
task. Some Linux distributions place optional tasks in a different
package, so make sure you have installed all necessary packages if you
are a Linux user.

II.  Compiling FreeCol
=================================
NOTE: The compiled files are included with the releases, so this
step is not required in order to play the game. 

Launch a command prompt and go to the directory where you unpacked
the FreeCol package.
Make sure the file named 'build.xml' is in your current directory.
From that directory execute the command 'ant'. In case the command
could not be found, check your Ant installation and make sure that
the ant (*nix) or ant.exe (Windows) file is in your PATH. See the
Ant installation manual for more information.
If the compilation fails, check your CLASSPATH environment variable
and make sure that '.' is a part of it.


III. Running FreeCol
=================================
Launch a command prompt and go to the directory where you unpacked
the FreeCol package (in case you downloaded the binary package) or
the directory where you executed the 'ant' command (in case you
downloaded the source package).
Make sure the file named 'FreeCol.jar' is in your current directory.

Execute the command 'java -Xmx512M -jar FreeCol.jar'

Windows users may need to execute
'java -Xmx512M -cp . -jar FreeCol.jar'

In case the command could not be found, check your Java installation
and make sure that the java (*nix) or java.exe (Windows) file is in
your PATH.

Run 'java -jar FreeCol.jar --usage' to get a list of available
command-line parameters.

You may ignore the message:
  "The music files could not be loaded by FreeCol. Disabling music."
because there is no music available yet for FreeCol.


IV.  Troubleshooting
=================================
Q: I get the following error:
   The data file "data/images/units/Unit0.png" could not be found.
A: Use the command-line option --freecol-data to specify the location
   of the 'data' directory.
   For example: java -jar FreeCol.jar --freecol-data C:\Games\FreeCol\data

Q: I am unable to start the game.
A: Try running the game in windowed mode: 
   'java -Xmx256M -jar FreeCol.jar --windowed'


V.   Help, Feedback & Bug Reports
=================================
Help
----
If you need help with anything related to FreeCol that is not
yet documented somewhere then you can send an e-mail to the
mailing list: freecol-users@lists.sourceforge.net

Feedback
--------
General Feedback is very much appreciated. Send an e-mail to
freecol-users@lists.sourceforge.net if you want to provide feedback.

Bug Reports
-----------
FreeCol is currently still beta software and may contain some bugs.

If the client should hang/freeze/crash then remember
that you can always press <Control>-<Alt>-<Backspace> or
<Control>-<Alt>-<F1 or F2, ...> when using Linux/Unix or
<Control>-<Alt>-<Delete> when using Windows.

If you manage to close FreeCol in this way or if you encounter less
serious problems then you can provide the developers with information
on the problem you've had by entering the following data as a bug
report at FreeCol's Sourceforge page at
http://sourceforge.net/tracker/?func=add&group_id=43225&atid=435578

- Detailed description of the problem you've had
- Contents of the file 'FreeCol.log' which can be found in the
  directory from which you launched the client
- If possible: a stacktrace that you may see when running the
  game from the command line.


VI.  About
=================================
Content is Copyright (C) 2002-2015 The FreeCol Team
The authors of FreeCol can be found on the FreeCol homepage.
FreeCol's homepage can be found at http://www.freecol.org
