JRGSS
=====

## Introduction

JRGSS is an open source, alternative implementation of [RGSS](http://rmvxace.wikia.com/wiki/RGSS), the game engine that underlies all games developed with RPG Maker VX Ace. It was originally created for the game [Vidar](http://vidarthegame.com/ "Vidar: The RPG Puzzler where Everybody Dies").

The majority of RGSS is implemented in [Ruby](https://www.ruby-lang.org/en/), a high level programming language that can run on virtually any operating system. However, in order to provide low-level graphics and audio functionality, parts of RGSS are implemented in C code tailored to interact with the [Win32 API](http://en.wikipedia.org/wiki/Windows_API). Because this portion of the codebase is written specifically for Windows, it causes the entire game engine to be limited to a single platform.

JRGSS replaces this underlying layer of C with a Java foundation, allowing games to be run on any platform which supports version 8 or higher of the Java Virtual Machine. At the time of this writing, this includes:

* Windows Vista and newer
* Mac OS X 10.8.3+, 10.9+
* Flavors of Linux running a modern kernel version

This substitute foundation is a drop-in replacement for RGSS. JRGSS executes all of the Ruby scripts that comprise the rest of the game engine via [JRuby](http://jruby.org/), a Java implementation of the Ruby runtime. JRGSS also exposes the same APIs as the C implementation; while all of these functions use the original, win32-flavored names, each is implemented in a cross-platform manner. This allows the existing ecosystem of RGSS scripts written by volunteers and enthusiasts to run without modification on Mac OS X and Linux.

Additionally, JRGSS also takes advantage of 3D acceleration where possible, outsourcing intensive graphics work to the computer's GPU. This results in more complex game scenes (such as Vidar's bustling town center) being rendered at a smooth 60 frames per second -- a dramatic improvement over the choppy rendering of standard RGSS.

Contributions are welcome!

## Quickstart
Using JRGSS:
1. Go to the Release page and download the version that's correct for your OS.
2. Unzip the archive into your desired installation destination.
3. Launch your game from the commandline using the following command:
   1. **Linux/Mac:** ./jrgss --rtp /path/to/rtp /path/to/game/folder
   2. **Windows:** jrgss.exe --rtp "C:\Program Files (x86)\Common Files\Enterbrain\RGSS3\RPGVXAce" "C:\Path\To\Your\Game"

## How do I get the RTP on Mac/Linux
Unfortunately, the RTP is distributed through a Windows installer. The easiest way is to copy it from a Windows PC. If you do not have access to one, it is possible to install the RTP through wine and copying the rtp from the wine prefix.

Link to official [RTP](https://dl.komodo.jp/rpgmakerweb/run-time-packages/RPGVXAce_RTP.zip)

## Controls
Same controls as stock RPG Maker VX Ace Player, mostly. Short reminder of important keys:
* MOVEMENT: Arrow Keys
* CONFIRM: Enter, Space, Z
* CANCEL: Esc, X, Numpad 0
* FULLSCREEN: Alt + Enter

## v1.0.0 Preview Release
I had created many enhancements to JRGSS before the release of Vidar, but never pushed them to github. And then I lost the source code ☹️. However, I randomly had the idea to install Vidar from steam and decompile the version shipped with the Linux build. 

This version is rough around the edges, but fairly functional. Give the preview build a try! Some caveats:
* Only RGSS3 is supported (RPG Maker VX Ace). Older games **MAY** work, but that hasn't been the focus of the project.
* I've only tested this on Linux x64. While there are builds for Windows and Mac, I have no idea if they work. I am working on getting access to different machines for testing these OSes. Let me know if you give JRGSS a try on Windows or Mac.
* Not super user friendly, especially if you need the RTP. I'd like to add the capability to automatically download the RTP, but I'd prefer not to host it myself and dealing with the windows installer format is a bit cumbersome. For now, you can copy it from a windows computer or use wine to install the RTP and copy it from there.
* I have not tested controller support. It may or may not work.
* Not all features of RGSS3 are supported! Please let me know if you find missing features you'd like added.


## Some Examples:

![Fullscreen Example](/screenshots/example-fullscreen.png?raw=true "Fullscreen Example with legible text")
![Ice Cave in Vidar Demo](/screenshots/vidar1.png?raw=true "Ice Cave in Vidar Demo")
![Always Sometimes Monsters Title Screen](/screenshots/asm1.png?raw=true "Always Sometimes Monsters Title Screen")
![Vanilla RPG Maker Project](/screenshots/example2.png?raw=true "Vanilla RPG Maker Project")
![Example Project](/screenshots/example1.png?raw=true "Example Project")
