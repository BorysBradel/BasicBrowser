# BasicBrowser

A simple web browser written in Java. The browser uses a regular text editor
pane to show a simplified version of web pages.

To run the browser, you can create a project in an IDE or run the browser from
the command line. To run the tests, its easier to use an IDE.

In an IDE, you can create a project, add the Java files, and then add a
dependency on the jsoup jar file. After that you can compile and run the
browser.

To run the tests, you need to add JUnit in the classpath. For example, in
IntelliJ, open BasicBrowserTest.java, expand the imports, and choose
'add JUnit 5.4 to classpath' from the show context actions (alt-enter or right
mouseclick followed by choosing Show Context Actions). After that you can run
the tests.

For command-line use, you have to put BasicBrowser.java and the jsoup jar file
in one directory. Then compile and run the browser in that directory.

The instructions for operating systems other than Windows are the following:
- /path/to/javac -cp jsoup-1.13.1.jar BasicBrowser.java
- /path/to/java -cp .:jsoup-1.13.1.jar BasicBrowser

The instructions for Windows are the following:
- C:\path\to\javac -cp jsoup-1.13.1.jar BasicBrowser.java
- C:\path\to\java -cp .;jsoup-1.13.1.jar BasicBrowser

The browser has a navigation bar and a bookmarks bar. After that are the
HTML text pane and the status text field. The navigation bar has 
buttons for the typical browser actions of back in history,
forward in history, reload web page, open a new tab, close the current tab,
and open the last closed tab. The history and last closed tabs information is
kept only for the last several traversal and closing operations. The
navigation bar also has a button to change the search options and the url text
field. 

The url text field can be used to enter urls, local files in a Windows
directory, or search terms. The urls either start with http:// or 
https:// or matches the regular expression \S+[.:]\w+/?\S* and will have
http:// prepended. The local file path is recognized when it starts with C:\
or D:\. If the text does not match any of these patterns, the program assumes
that a search should be performed.

The search terms can either have a prefix or not. By default, if the text starts
with "w " then Wikipedia will be used. The text "w Wikipedia" will be translated
to https://en.wikipedia.org/w/index.php?search=Wikipedia and the text "Google"
will be translated to http://www.google.com/search?q=Google. By entering the
appropriate text into the url text field and pressing the Change Search button
will change stored search data. If the text matches a url, the non-prefix search
is changed. If the text is just one of the prefixes, then the corresponding
search is deleted. If the text starts with an existing prefix then the 
corresponding search term will be updated. If the text starts with a prefix
that is not used yet then that search will be added.

For example, using "https://en.wikipedia.org/w/index.php?search=" would result
in changing the non-prefix search, using "w http://www.google.com/search?q="
would change the search starting with "w", using
"a https://en.wikipedia.org/w/index.php?search=" would add a new search starting
with "a", and using "w" would delete the search that starts with the prefix "w".

The bookmarks bar has three sets of bookmarks that can be selected via dropdown
lists. The add/remove buttons compare the contents of the url text field with
the corresponding bookmark list and either add or remove the bookmark.

The following are the browser's keyboard shortcuts:
- Back - alt-left, ctrl-left
- Forward - alt-right, ctrl-right
- Reload - alt-r, ctrl-r
- New Tab - alt-n, ctrl-n
- Close Tab - alt-w, ctrl-w
- Open Last Closed Tab - alt-t, ctrl-shift-t
- Change Search - alt-g, ctrl-g
- Go to url field - alt-l, ctrl-l
- Go to specific tab (for first set of tabs) - alt-0, alt-1, ..., alt-9
- Go to next tab - ctrl-pageDown
- Go to previous tab - ctrl-pageUp
- Add/Remove bookmark in bookmark list A - alt-d, ctrl-d
- Add/Remove bookmark in bookmark list B - alt-e, ctrl-e
- Add/Remove bookmark in bookmark list C - alt-f, ctrl-f
