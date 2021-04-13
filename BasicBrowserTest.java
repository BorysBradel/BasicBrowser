import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;

import static org.junit.jupiter.api.Assertions.*;

class BasicBrowserTest {
    static class ImmediateRunFactory<T> implements BasicBrowser.RunnableFutureFactory<T> {
        boolean hasMethods;
        Supplier<T> doInBackground;
        Consumer<T> done;
        Consumer<String> runFinally;

        ImmediateRunFactory() {
            hasMethods = false;
        }

        @Override
        public boolean createFuture(Supplier<T> doInBackground, Consumer<T> done, Consumer<String> runFinally) {
            System.out.println("createFuture " + hasMethods);
            assertFalse(hasMethods);
            hasMethods = true;
            this.doInBackground = doInBackground;
            this.done = done;
            this.runFinally = runFinally;
            return true;
        }

        @Override
        public boolean runFuture() {
            System.out.println("runFuture " + hasMethods);
            assertTrue(hasMethods);
            done.accept(doInBackground.get());
            runFinally.accept("");
            return true;
        }

        @Override
        public boolean clearFuture() {
            System.out.println("clearFuture " + hasMethods);
            assertTrue(hasMethods);
            hasMethods = false;
            return true;
        }

        @Override
        public void tryCancellingFuture() {
        }
    }

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
                BasicBrowser browser = new BasicBrowser(
                        new MockPreferences(), new ImmediateRunFactory<>(), 2);
                assertEquals(1, browser.tabs.size());
                browser.closeTab();
                assertEquals(1, browser.tabs.size());
                browser.addTab();
                assertEquals(2, browser.tabs.size());
                assertEquals(1, browser.iCurrentTab);
                assertEquals("", browser.urlField.getText());
                assertEquals(emptyHtml, browser.tabs.get(1).editorPane.getText());
                browser.urlUpdate("");
                assertEquals(emptyHtml, browser.tabs.get(1).editorPane.getText());
                // Going to a url consists of filling the urlField and then its action event calls
                // urlUpdate with the contents. Mimic that here.
                String url = "C:\\Users\\Name\\Downloads\\slashdot2.htm";
                browser.urlField.setText(url);
                browser.urlUpdate(url);
                assertNotEquals(emptyHtml, browser.tabs.get(1).editorPane.getText());
                browser.closeTab();
                assertEquals(1, browser.tabs.size());
                assertEquals(0, browser.iCurrentTab);
                assertEquals("", browser.urlField.getText());
                browser.openLastClosedTab();
                assertEquals(2, browser.tabs.size());
                assertEquals(1, browser.iCurrentTab);
                assertEquals(url, browser.urlField.getText());
                browser.openLastClosedTab();
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
                BasicBrowser browser = new BasicBrowser(
                        new MockPreferences(), new ImmediateRunFactory<>(), 2);
                BasicBrowser.HtmlTab tab = browser.tabs.get(0);
                Component source = tab.editorPane;
                Element sourceElement = tab.kit.createDefaultDocument().getDefaultRootElement();
                String url = "http://localhost:8000/abc";
                HyperlinkEvent event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
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
                BasicBrowser browser = new BasicBrowser(
                        new MockPreferences(), new ImmediateRunFactory<>(), 2);
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
                BasicBrowser browser = new BasicBrowser(
                        new MockPreferences(), new ImmediateRunFactory<>(), 3);
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
                assertLinesMatch(List.of("", url1, url2), tab.history);
                assertEquals(2, tab.iHistory);
                assertEquals(url2, browser.urlField.getText());
                String url3 = "http://localhost:8000/ab3";
                event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url3), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
                assertLinesMatch(List.of(url1, url2, url3), tab.history);
                assertEquals(url3, browser.urlField.getText());
                assertEquals(2, tab.iHistory);
                tab.goBack();
                assertEquals(url2, browser.urlField.getText());
                assertEquals(1, tab.iHistory);
                tab.goForward();
                assertEquals(url3, browser.urlField.getText());
                assertEquals(2, tab.iHistory);
                tab.goBack();
                assertEquals(url2, browser.urlField.getText());
                assertEquals(1, tab.iHistory);
                tab.goBack();
                assertEquals(url1, browser.urlField.getText());
                assertEquals(0, tab.iHistory);
                event = new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED,
                        new URL(url3), "event", sourceElement,
                        new MouseEvent(source, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                                0, 50, 50, 1, false));
                tab.hyperlinkUpdate(event);
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
                BasicBrowser browser = new BasicBrowser(
                        preferences, new ImmediateRunFactory<>(), 3);
                // Add
                browser.urlField.setText("a http://a.com/q=");
                browser.changeSearch();
                assertEquals(BasicBrowser.defaultQuickSearchStr + " a http://a.com/q=", browser.statusField.getText());
                // Delete
                browser.urlField.setText("w");
                browser.changeSearch();
                assertEquals("http://www.google.com/search?q= a http://a.com/q=", browser.statusField.getText());
                // Change default
                browser.urlField.setText("http://b.com/q=");
                browser.changeSearch();
                assertEquals("http://b.com/q= a http://a.com/q=", browser.statusField.getText());
                // Change non-default
                browser.urlField.setText("a http://c.com/q=");
                browser.changeSearch();
                assertEquals("http://b.com/q= a http://c.com/q=", browser.statusField.getText());
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
                browser.urlField.setText("b test1");
                browser.urlUpdate(browser.urlField.getText());
                assertEquals("http://localhost:8000/q=test1", browser.urlField.getText());
                browser.urlField.setText("test2");
                browser.urlUpdate(browser.urlField.getText());
                assertEquals("http://localhost:8001/q=test2", browser.urlField.getText());
                browser.urlField.setText("localhost:8000/a");
                browser.urlUpdate(browser.urlField.getText());
                assertEquals("http://localhost:8000/a", browser.urlField.getText());
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    void testTabHistory() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            try {
                BasicBrowser browser = new BasicBrowser(
                        new MockPreferences(), new ImmediateRunFactory<>(), 2);
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
                BasicBrowser browser = new BasicBrowser(
                        new MockPreferences(), new ImmediateRunFactory<>(), 2);
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
                bookmarkBox.setSelectedItem(url1); // will trigger openBookmarkEvent(url1)
                assertEquals(1, browser.tabs.size());
                assertEquals(url1, browser.urlField.getText());
                assertEquals(BasicBrowser.exceptionStr.formatted(connectionExceptionStr, url1),
                        browser.statusField.getText());
                assertEquals(0, browser.iCurrentTab);
                // Open all bookmarks
                bookmarkBox.setSelectedIndex(bookmarkBox.getItemCount() - 1); // will trigger openBookmarkEvent(openAll)
                assertEquals(BasicBrowser.openAllStr, bookmarkBox.getSelectedItem());
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
}