Golf Analyst 4.01 - the "runs anywhere" copy
=============================================

Golf scores, handicap and statistics, by Angelo Mileto.

This copy runs on Windows, Mac and Linux with the Java that is already on the
computer. On Windows the installer (GolfAnalyst_v4.01.exe) is easier: it
brings its own Java.

What you need
-------------
Java 17 or newer. To check, open a terminal (Command Prompt on Windows) and type:

    java -version

If it is missing or older than 17, install it free from https://adoptium.net
(choose the latest LTS version).

How to start it
---------------
1. Unzip the whole folder somewhere you like, for example your home folder.
   Keep the files together.
2. Windows:      double-click run.bat
   Mac or Linux: open a terminal in the folder and type:  sh run.sh

Where your data goes
--------------------
The program starts with made-up sample data so there is something to look at.
When you press New Round or New Course it offers to make your own data folder:

    Windows:  Documents\Golf Analyst
    Mac:      Documents/Golf Analyst (in your home folder)
    Linux:    Documents/Golf Analyst (in your home folder)

Nothing is ever written inside this folder, so you can replace it with a newer
copy at any time without losing your rounds. Deleting this folder never
removes your data.

If something goes wrong
-----------------------
Write down what you were doing, take a screenshot, and look at the bar along
the bottom of the window: it says when a data file could not be read. Then
tell Angelo at https://github.com/ShosMeister/GolfAnalyst/issues (New issue).

Credits
-------
ZIP code data from GeoNames (www.geonames.org), used under a Creative Commons
Attribution license. The data is provided as is.

Background photographs courtesy of On The Green Magazine
(www.onthegreenmagazine.com), used with permission.
