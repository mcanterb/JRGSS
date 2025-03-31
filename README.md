JRGSS
=====

## v2.0.0 in progress
I had created many enhancements to JRGSS before the release of Vidar, but never pushed them to github. And then I lost the source code. However, I randomly had the idea to install Vidar from steam and decompile the version shipped with the Linux build. I took the decompiled code and I'm reworking it to work with modern versions of libgdx.

This version does compile, but doesn't work fully. Can boot to main menu of a test game, but then freezes. Work in Progress!

## Introduction

JRGSS is an open source, alternative implementation of [RGSS](http://rmvxace.wikia.com/wiki/RGSS), the game engine that underlies all games developed with RPG Maker VX Ace. It was originally created for the game [Vidar](http://vidarthegame.com/ "Vidar: The RPG Puzzler where Everybody Dies").

The majority of RGSS is implemented in [Ruby](https://www.ruby-lang.org/en/), a high level programming language that can run on virtually any operating system. However, in order to provide low-level graphics and audio functionality, parts of RGSS are implemented in C code tailored to interact with the [Win32 API](http://en.wikipedia.org/wiki/Windows_API). Because this portion of the codebase is written specifically for Windows, it causes the entire game engine to be limited to a single platform.

JRGSS replaces this underlying layer of C with a Java foundation, allowing games to be run on any platform which supports version 8 or higher of the Java Virtual Machine. At the time of this writing, this includes:

* Windows Vista and newer
* Mac OS X 10.8.3+, 10.9+
* Flavors of Linux running a modern kernel version

This substitute foundation is a drop-in replacement for RGSS. JRGSS executes all of the Ruby scripts that comprise the rest of the game engine via [JRuby](http://jruby.org/), a Java implementation of the Ruby runtime. JRGSS also exposes the same APIs as the C implementation; while all of these functions use the original, win32-flavored names, each is implemented in a cross-platform manner. This allows the existing ecosystem of RGSS scripts written by volunteers and enthusiasts to run without modification on Mac OS X and Linux.

Additionally, JRGSS also takes advantage of 3D acceleration where possible, outsourcing intensive graphics work to the computer's GPU. This results in more complex game scenes (such as [Vidar](http://www.vidarthegame.com/ "Vidar: The RPG Puzzler where Everybody Dies")'s bustling town center) being rendered at a smooth 60 frames per second -- a dramatic improvement over the choppy rendering of standard RGSS.

Contributions are welcome!

## Quickstart
TODO: Add quickstart instructions.

## Status
This version does compile, but doesn't work fully. I am able to boot to the menu screen of a barebones RPG Makes VX Ace project, but it freezes trying to render the menus.

## Some Examples:

![Ice Cave in Vidar Demo](/screenshots/vidar1.png?raw=true "Ice Cave in Vidar Demo")
![Always Sometimes Monsters Title Screen](/screenshots/asm1.png?raw=true "Always Sometimes Monsters Title Screen")
![Vanilla RPG Maker Project](/screenshots/example2.png?raw=true "Vanilla RPG Maker Project")
![Example Project](/screenshots/example1.png?raw=true "Example Project")
