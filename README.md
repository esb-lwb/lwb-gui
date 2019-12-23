# lwb-gui

A simple GUI for the Logic Workbench lwb.

## Usage

You should have installed Java 9 or younger.<br/>
Just start the app and then open a new session with the Logic Workbench.

### Mac OSX
<code>java -Duser.language=en -Xdock:name=lwb-gui -jar lwb-gui.jar</code>

Unfortunately javapackager in Java 9 doesn't what it should do to make a proper Mac app, 
at least I didn't manage to do this. But there is a work-around:

Download lwb-gui.jar from the release page and lwb.icns from the code page to a directoy {lwb-dir}.
Create a program in automator for a shell script with the following command

<code>exec java -Duser.language=en -Xdock:name="lwb-gui" -Xdock:icon={lwb-dir}/lwb.icns -jar {lwb-dir}/lwb-gui.jar </code>

Replace {lwb-dir} with your directory path.

Export the automator script under the name lwb-gui.app to the programs directory. You can furthermore
replace the icon of lwb-gui.app with lwb.icns. 

### Linux
<code>java -Duser.language=en -jar lwb-gui.jar</code>

### Windows
<code>java -Duser.language=en -jar lwb-gui.jar</code>

## License

Copyright © 2019 by Burkhardt Renz, THM.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php).
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.


