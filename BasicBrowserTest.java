import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.prefs.BackingStoreException;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

class BasicBrowserTest {
    final static String emptyHtml = """
            <html>\r
              <head>\r
            \r  
              </head>\r
              <body>\r
                <p style="margin-top: 0">\r
                 \s\r
                </p>\r
              </body>\r
            </html>\r
            """;

    final static String connectionExceptionStr =
            "java.net.ConnectException: Connection refused: no further information";
    // TODO startup /teardown - create files in temp dir

    @Test
    void testTabs() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                BasicBrowser browser = new BasicBrowser(new MockPreferences(), 2);
                assertEquals(1, browser.tabs.size());
                browser.closeTab();
                assertEquals(1, browser.tabs.size());
                browser.addTab();
                assertEquals(2, browser.tabs.size());
                assertEquals(1, browser.iCurrentTab);
                assertEquals("", browser.urlField.getText());
                BasicBrowser.HtmlTab tab1 = browser.tabs.get(1);
                assertEquals(emptyHtml, tab1.editorPane.getText());
                browser.urlUpdate("");
                assertEquals(emptyHtml, tab1.editorPane.getText());
                // Going to a url consists of filling the urlField and then its action event calls
                // urlUpdate with the contents. Mimic that here.
                String url = "C:\\Users\\Name\\Downloads\\slashdot2.htm";
                browser.urlField.setText(url);
                browser.urlUpdate(url);
                var worker = tab1.worker;
                if (worker != null) {
                    worker.run();
                }
                assertNotEquals(emptyHtml, tab1.editorPane.getText());
                browser.closeTab();
                assertEquals(1, browser.tabs.size());
                assertEquals(0, browser.iCurrentTab);
                assertEquals("", browser.urlField.getText());
                browser.openLastClosedTab();
                worker = browser.tabs.get(1).worker;
                if (worker != null) {
                    worker.run();
                }
                assertNotEquals(emptyHtml, browser.tabs.get(1).editorPane.getText());
                assertEquals(2, browser.tabs.size());
                assertEquals(1, browser.iCurrentTab);
                assertEquals(url, browser.urlField.getText());
                browser.openLastClosedTab();
                assertEquals(BasicBrowser.noLastCloseTabsStr, browser.statusField.getText());
                assertNotEquals("", browser.tabs.get(1).editorPane.getText());
                assertEquals(2, browser.tabs.size());
                assertEquals(1, browser.iCurrentTab);
                browser.closeTab();
                assertEquals(1, browser.tabs.size());
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testFollowingLinks() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                BasicBrowser browser = new BasicBrowser(new MockPreferences(), 2);
                BasicBrowser.HtmlTab tab = browser.tabs.get(0);
                Component source = tab.editorPane;
                Element sourceElement = tab.kit.createDefaultDocument().getDefaultRootElement();
                String url = "http://localhost:8000/abc";
                HyperlinkEvent event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
                var worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertEquals(url, browser.urlField.getText());
                assertEquals(1, browser.tabs.size());
                assertEquals(0, browser.iCurrentTab);
                assertEquals("Untitled", tab.title);
                assertEquals(url, tab.getUrl());
                assertEquals(emptyHtml, tab.editorPane.getText());
                String exceptionStatus = BasicBrowser.exceptionStr.formatted(
                        connectionExceptionStr, url);
                assertEquals(exceptionStatus, browser.statusField.getText());
                event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                KeyEvent.CTRL_DOWN_MASK, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
                worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertEquals(url, browser.urlField.getText());
                assertEquals(2, browser.tabs.size());
                assertEquals(1, browser.iCurrentTab);
                BasicBrowser.HtmlTab tab1 = browser.tabs.get(1);
                assertEquals("Untitled", tab1.title);
                assertEquals(url, tab1.getUrl());
                assertEquals(emptyHtml, tab1.editorPane.getText());
                assertEquals(exceptionStatus, browser.statusField.getText());
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testHoverOverLinks() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                BasicBrowser browser = new BasicBrowser(new MockPreferences(), 2);
                BasicBrowser.HtmlTab tab = browser.tabs.get(0);
                Component source = tab.editorPane;
                Element sourceElement = tab.kit.createDefaultDocument().getDefaultRootElement();
                String url = "http://localhost:8000/abc";
                HyperlinkEvent event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ENTERED,
                        new URL(url), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
                assertEquals(url, browser.statusField.getText());
                event = new HyperlinkEvent(source, HyperlinkEvent.EventType.EXITED,
                        new URL(url), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
                assertEquals("", browser.statusField.getText());
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testLinksHistory() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                BasicBrowser browser = new BasicBrowser(new MockPreferences(), 3);
                BasicBrowser.HtmlTab tab = browser.tabs.get(0);
                Component source = tab.editorPane;
                Element sourceElement = tab.kit.createDefaultDocument().getDefaultRootElement();
                String url1 = "http://localhost:8000/abc1";
                assertLinesMatch(List.of(""), tab.history);
                assertEquals(0, tab.iHistory);
                tab.goBack();
                assertEquals(0, tab.iHistory);
                HyperlinkEvent event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url1), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
                var worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertLinesMatch(List.of("", url1), tab.history);
                assertEquals(url1, browser.urlField.getText());
                assertEquals(1, tab.iHistory);
                tab.goForward();
                assertEquals(1, tab.iHistory);
                String url2 = "http://localhost:8000/abc2";
                event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url2), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
                worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertLinesMatch(List.of("", url1, url2), tab.history);
                assertEquals(2, tab.iHistory);
                assertEquals(url2, browser.urlField.getText());
                String url3 = "http://localhost:8000/ab3";
                event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url3), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
                worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertLinesMatch(List.of(url1, url2, url3), tab.history);
                assertEquals(url3, browser.urlField.getText());
                assertEquals(2, tab.iHistory);
                tab.goBack();
                worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertEquals(url2, browser.urlField.getText());
                assertEquals(1, tab.iHistory);
                tab.goForward();
                worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertEquals(url3, browser.urlField.getText());
                assertEquals(2, tab.iHistory);
                tab.goBack();
                worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertEquals(url2, browser.urlField.getText());
                assertEquals(1, tab.iHistory);
                tab.goBack();
                worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertEquals(url1, browser.urlField.getText());
                assertEquals(0, tab.iHistory);
                event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url3), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
                worker = tab.worker;
                if (worker != null) {
                    worker.run();
                }
                assertLinesMatch(List.of(url1, url3), tab.history);
                assertEquals(url3, browser.urlField.getText());
                assertEquals(1, tab.iHistory);
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testChangeSearch() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                MockPreferences preferences = new MockPreferences();
                BasicBrowser browser = new BasicBrowser(preferences, 3);
                // Add
                browser.urlField.setText("a http://a.com/q=");
                browser.changeSearch();
                assertEquals(BasicBrowser.defaultQuickSearchStr + " a http://a.com/q=",
                        browser.statusField.getText());
                // Delete
                browser.urlField.setText("w");
                browser.changeSearch();
                assertEquals("http://www.google.com/search?q= a http://a.com/q=",
                        browser.statusField.getText());
                // Change default
                browser.urlField.setText("http://b.com/q=");
                browser.changeSearch();
                assertEquals("http://b.com/q= a http://a.com/q=", browser.statusField.getText());
                // Change non-default
                browser.urlField.setText("a http://c.com/q=");
                browser.changeSearch();
                assertEquals("http://b.com/q= a http://c.com/q=", browser.statusField.getText());
                assertEquals("http://b.com/q= a http://c.com/q=",
                        preferences.get(BasicBrowser.quickSearchKey, ""));
                // Try to delete something that doesn't exist
                browser.urlField.setText("z");
                browser.changeSearch();
                assertEquals(BasicBrowser.quickSearchToDeleteNotFoundErrorStr, browser.statusField.getText());
                // Try to pass in badly formatted string.
                browser.urlField.setText("x y z");
                browser.changeSearch();
                assertEquals("x y z" + BasicBrowser.quickSearchUrlFormatErrorStr,
                        browser.statusField.getText());
                // Use default and non-default quick searches.
                browser.urlField.setText("b http://localhost:8000/q=");
                browser.changeSearch();
                browser.urlField.setText("http://localhost:8001/q=");
                browser.changeSearch();
                assertEquals("http://localhost:8001/q= a http://c.com/q= b http://localhost:8000/q=",
                        preferences.get(BasicBrowser.quickSearchKey, ""));
                browser.urlField.setText("b test1");
                browser.urlUpdate(browser.urlField.getText());
                assertEquals("http://localhost:8000/q=test1", browser.urlField.getText());
                browser.urlField.setText("test2");
                browser.urlUpdate(browser.urlField.getText());
                assertEquals("http://localhost:8001/q=test2", browser.urlField.getText());
                browser.urlField.setText("localhost:8000/a");
                browser.urlUpdate(browser.urlField.getText());
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testTabHistory() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                BasicBrowser browser = new BasicBrowser(new MockPreferences(), 2);
                browser.addTab();
                browser.addTab();
                browser.addTab();
                BasicBrowser.HtmlTab tab1 = browser.tabs.get(1);
                BasicBrowser.HtmlTab tab2 = browser.tabs.get(2);
                BasicBrowser.HtmlTab tab3 = browser.tabs.get(3);
                browser.closeTab();
                assertEquals(1, browser.closedTabs.size());
                assertEquals(tab3, browser.closedTabs.peek());
                browser.closeTab();
                assertEquals(2, browser.closedTabs.size());
                assertEquals(tab2, browser.closedTabs.peek());
                assertEquals(tab3, browser.closedTabs.peekLast());
                browser.closeTab();
                assertEquals(2, browser.closedTabs.size());
                assertEquals(tab1, browser.closedTabs.peek());
                assertEquals(tab2, browser.closedTabs.peekLast());
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }


    // Add/Remove on empty url

    @Test
    void testBookmarks() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                BasicBrowser browser = new BasicBrowser(new MockPreferences(), 2);
                // Open all bookmarks when none
                assertEquals(1, browser.tabs.size());
                JComboBox<String> bookmarkBox = browser.bookmarkBoxes.get(0);
                JButton addRemove = browser.addRemove[0];
                String addRemoveStr = BasicBrowser.addRemoveStrings[0];
                bookmarkBox.setSelectedIndex(bookmarkBox.getItemCount() - 1); // will trigger openBookmarkEvent(openAll)
                assertEquals(1, browser.tabs.size());
                // Add bookmarks
                String url1 = "http://localhost:8000/abc1";
                browser.urlField.setText(url1);
                browser.tabsPane.setTitleAt(0, "abc1");
                browser.addOrRemoveEvent(new ActionEvent(addRemove, ActionEvent.ACTION_PERFORMED, addRemoveStr));
                assertEquals(url1, bookmarkBox.getItemAt(0));
                assertEquals(BasicBrowser.openAllStr, bookmarkBox.getItemAt(1));
                assertEquals(1, browser.tabs.size());
                String url2 = "http://localhost:8000/abc2";
                browser.urlField.setText(url2);
                browser.tabsPane.setTitleAt(0, "abc2");
                browser.addOrRemoveEvent(new ActionEvent(addRemove, ActionEvent.ACTION_PERFORMED, addRemoveStr));
                assertEquals(url1, bookmarkBox.getItemAt(0));
                assertEquals(url2, bookmarkBox.getItemAt(1));
                assertEquals(BasicBrowser.openAllStr, bookmarkBox.getItemAt(2));
                assertEquals(1, browser.tabs.size());
                // Open bookmark
                System.out.println("Will open a bookmark");
                bookmarkBox.setSelectedItem(url1); // will trigger openBookmarkEvent(url1)
                var worker = browser.tabs.get(0).worker;
                if (worker != null) {
                    worker.run();
                }
                assertEquals(1, browser.tabs.size());
                assertEquals(url1, browser.urlField.getText());
                assertEquals(BasicBrowser.exceptionStr.formatted(connectionExceptionStr, url1),
                        browser.statusField.getText());
                assertEquals(0, browser.iCurrentTab);
                // Open all bookmarks
                System.out.println("Will open all bookmarks");
                bookmarkBox.setSelectedIndex(bookmarkBox.getItemCount() - 1); // will trigger openBookmarkEvent(openAll)
                assertEquals(BasicBrowser.openAllStr, bookmarkBox.getSelectedItem());
                worker = browser.tabs.get(1).worker;
                if (worker != null) {
                    worker.run();
                }
                worker = browser.tabs.get(2).worker;
                if (worker != null) {
                    worker.run();
                }
                assertEquals(3, browser.tabs.size());
                assertEquals(2, browser.iCurrentTab);
                assertEquals(BasicBrowser.exceptionStr.formatted(connectionExceptionStr, url2),
                        browser.statusField.getText());
                // Close tabs
                browser.closeTab();
                assertEquals(2, browser.tabs.size());
                assertEquals(1, browser.iCurrentTab);
                browser.closeTab();
                assertEquals(1, browser.tabs.size());
                assertEquals(0, browser.iCurrentTab);
                // Remove bookmark
                browser.addOrRemoveEvent(new ActionEvent(addRemove, ActionEvent.ACTION_PERFORMED, addRemoveStr));
                assertEquals(url2, bookmarkBox.getItemAt(0));
                assertEquals(BasicBrowser.openAllStr, bookmarkBox.getItemAt(1));
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testMultipleUrlUpdateError() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                BasicBrowser browser = new BasicBrowser(new MockPreferences(), 2);
                BasicBrowser.HtmlTab tab = browser.tabs.get(0);
                tab.urlUpdate("http://localhost:8000/a", 0);
                String url = "http://localhost:8001/a";
                tab.urlUpdate(url, 0);
                assertEquals(BasicBrowser.workerExistsErrorStr.formatted(url, 0), browser.statusField.getText());
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }

    // The right way to have the SwingWorker threads finish before checking correctness is to have one
    // SwingUtilities.invokeAndWait() set everything up, wait a bit for the threads to finish, and then
    // SwingUtilities.invokeAndWait() to verify that everything is correct. In all the other tests, the
    // alternative of just calling the worker's run() method works. However, for this test, the more
    // robust approach needs to be used.
    volatile BasicBrowser openAllBrowser;
    @Test
    void testOpenAll() throws InvocationTargetException, InterruptedException, IOException {
        int n = 3;
        File[] files = new File[n];
        String[] paths = new String[n];
        String[] bodies = new String[n];
        for (int i = 0; i < n; i++) {
            files[i] = File.createTempFile("file", ".html");
            paths[i] = files[i].getAbsolutePath();
            FileWriter fw = new FileWriter(files[i]);
            bodies[i] = "<html><head><title>title %d</title></head><body>body %d</body></html>\n".formatted(
                    i, i);
            fw.write(bodies[i]);
            fw.close();
        }
        SwingUtilities.invokeAndWait(() -> {
            try {
                openAllBrowser = new BasicBrowser(new MockPreferences(), 2);
                // Open all bookmarks when none
                assertEquals(1, openAllBrowser.tabs.size());
                var box = openAllBrowser.bookmarkBoxes.get(0);
                // Need to use file system files. Since that is not allowed, modify combo box directly.
                openAllBrowser.addRemoveIsRunning[0] = true;
                System.out.println("Remove open all.");
                box.removeItem(BasicBrowser.openAllStr);
                for (int i = 0; i < n; i++) {
                    System.out.printf("Adding file %s at index %d\n", paths[i], i);
                    box.addItem(paths[i]);
                }
                System.out.println("Add open all.");
                box.addItem(BasicBrowser.openAllStr);
                System.out.println("Done adding to box.");
                openAllBrowser.addRemoveIsRunning[0] = false;
                box.setSelectedIndex(box.getItemCount() - 1); // will trigger openBookmarkEvent(openAll)
                assertEquals(BasicBrowser.openAllStr, box.getSelectedItem());
                assertEquals(4, openAllBrowser.tabs.size());
                assertEquals(3, openAllBrowser.iCurrentTab);
            } catch (BackingStoreException | IOException e) {
                e.printStackTrace();
            }
        });
        // Give enough time for the SwingWorkers to complete their work before checking output.
        sleep(2000);
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < n; i++) {
                System.out.println("Checking assertions at index " + i);
                assertEquals("title " + i, openAllBrowser.tabs.get(i + 1).getTitle());
                assertEquals(paths[i], openAllBrowser.tabs.get(i + 1).getUrl());
                String expected =
                        "<html>\n  <head>\n    \n  </head>\n  <body>\n    body %d\n  </body>\n</html>\n".formatted(
                                i);
                assertEquals(expected, openAllBrowser.tabs.get(i + 1).editorPane.getText());
            }
        });
        for (int i = 0; i < n; i++) {
            if (!files[i].delete()) {
                System.out.println("Failed to delete file " + paths[i]);
            }
        }
    }
}