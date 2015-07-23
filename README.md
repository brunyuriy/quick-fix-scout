<h1> <font color='red'> Quick Fix Scout: Speculative Analysis of Eclipse Quick Fixes </font> </h1>

Quick Fix Scout Plug-in improves Eclipse’s quick fix dialogs, so that they display not just a set of proposals, but also a prediction about what will happen if you apply each proposal. It also adds better proposals that Eclipse would not have displayed. This helps you to make better and faster decisions about the quick fix proposals.

The plug-in has two features:
  * [Evaluator](Evaluator.md) improves Eclipse’s quick fix dialog.
  * [Observer](Observer.md) collects data that is useful for my research without affecting your user experience.

[Installation](Installation.md) details.

[Troubleshooting](Troubleshooting.md) details.

![http://wiki.quick-fix-scout.googlecode.com/hg/screen_shots/QF.jpg](http://wiki.quick-fix-scout.googlecode.com/hg/screen_shots/QF.jpg)

<h2> <font color='red'> Requirements & Tested On </font> </h2>

Eclipse 3.7.1, 3.7.2 Classic

Operating Systems: OSX Mountain Lion. Since this is a plug-in written in Java, it should be platform independent, however the plug-in is only tested on OSX. If you experience any issues in other operating systems, please [submit a bug report](https://code.google.com/p/quick-fix-scout/issues/entry).

<h2> <font color='red'> Collaborators </font> </h2>
This project is in collaboration among:

  * Yuriy Brun, Assistant Professor at University of Massachusetts, Amherst
  * Michael Ernst, Associate Professor at University of Washington
  * Reid Holmes, Assistant Professor at University of Waterloo
  * Kivanc Muslu, PhD. Student at University of Washington
  * David Notkin, Professor at University of Washington