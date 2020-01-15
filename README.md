# lwb-gui

A simple GUI for the Logic Workbench lwb.

## Usage

You should have installed Java 9 or newer.<br/>
Just start the app and then open a new session with the Logic Workbench.

### Mac OSX
<code>java -Duser.language=en -Xdock:name=lwb-gui -jar lwb-gui.jar</code>

Unfortunately javapackager in Java 9 doesn't do what it should do to make a proper Mac app, 
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

lwb-gui uses seesaw which has the same license as lwb-gui.

lwb-gui uses RSyntaxTextArea:

Copyright (c) 2019, Robert Futrell
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the author nor the names of its contributors may
      be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## How to build

1. We need a patched version of RSyntaxTextArea, see https://github.com/esb-dev/RSyntaxTextArea

2. And furthermore a patched version of sessaw, see https://github.com/esb-dev/seesaw

3. Leiningen task uberjar





