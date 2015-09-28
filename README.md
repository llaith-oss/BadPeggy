Bad Peggy
=========

Bad Peggy scans JPEG images for damage and other blemishes, and shows the
results and image instantly. It allows you to find such broken files quickly,
inspect and then either delete or move them to a different location.

Implemented in Java 8 and SWT. Runs on Windows/OSX/Linux.

Development
-----------

BadPeggy development is done in Eclipse (Mars+). Choose the right SWT project
for your platform, and import it into your workspace. It will show up as
*org.clipse.swt*. On 64bit Linux for instance it would be
*swt/4.5/gtk-linux_x86_64/*. You also need the library CLBaseLib, which you can
clone from GitHub and import its Eclipse project.

You can then run Bad Peggy by debugging the class *coderslagoon.badpeggy.GUI*.

For verification the few test cases can also be executed. Notice though that
they might fail due to slightly different image rendering of the test material.
This does usually not present a problem. Frozen reference test material is not
included, due to the huge size of it (800+ MB).

I18N
-----------

New user-facing strings need to be internationalized, meaning being available
in both English and German. New strings have to be added in the NLS files in
*coderslagoon.badpeggy.NLS...*. Please test for both languages, and watch out
for proper format string rendering.
