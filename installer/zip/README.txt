Golf Analyst 4.00 - the "runs anywhere" copy
=============================================

Golf scores, handicap and statistics, by Angelo Mileto.

This copy runs on Windows, Mac and Linux with the Java that is already on the
computer. On Windows the installer (GolfAnalyst-4.0.0...exe) is easier: it
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
When you save something it offers to make your own data folder:

    Windows:  Documents\Golf Analyst
    Mac:      Documents/Golf Analyst (in your home folder)
    Linux:    Documents/Golf Analyst (in your home folder)

Nothing is ever written inside this folder, so you can replace it with a newer
copy at any time without losing your rounds. Deleting this folder never
removes your data.

Credits
-------
ZIP code data from GeoNames (www.geonames.org), used under a Creative Commons
Attribution license. The data is provided as is.
