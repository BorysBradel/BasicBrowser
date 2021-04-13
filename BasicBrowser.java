import org.jsoup.Jsoup;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class BasicBrowser extends JPanel {

    interface RunnableFutureFactory<T> {
        boolean createFuture(Supplier<T> doInBackground, Consumer<T> done, Consumer<String> runFinally);
        boolean runFuture();
        boolean clearFuture();
        void tryCancellingFuture();
    }

    static class SwingWorkerFactory<T, V> implements RunnableFutureFactory<T> {
        SwingWorker<T, V> worker;
        SwingWorkerFactory() {
            worker = null;
        }

        @Override
        public boolean runFuture() {
            if (worker == null) {
                return false;
            }
            worker.execute();
            return true;
        }

        @Override
        public boolean clearFuture() {
            if (worker == null) {
                return false;
            }
            worker = null;
            return true;
        }

        @Override
        public void tryCancellingFuture() {
            if (worker != null) {
                worker.cancel(true);
                worker = null;
            }
        }

        @Override
        public boolean createFuture(Supplier<T> doInBackground, Consumer<T> done, Consumer<String> runFinally) {
            if (worker != null) {
                return false;
            }
            worker = new SwingWorker<>() {
                @Override
                protected T doInBackground() {
                    return doInBackground.get();
                }

                @Override
                public void done() {
                    String error = "";
                    try {
                        done.accept(get());
                    } catch (InterruptedException | ExecutionException e) {
                        error = e.toString();
                    } finally {
                        runFinally.accept(error);
                    }
                }
            };
            return true;
        }
    }

    static final String titleStr = "Basic Browser";
    static final String navigationBarStr = "Navigation Bar";
    static final String bookmarksBarStr = "Bookmarks Bar";
    static final String backStr = "Back";
    static final String forwardStr = "Forward";
    static final String reloadStr = "Reload";
    static final String newTabStr = "New Tab";
    static final String closeTabStr = "Close Tab";
    static final String openLastClosedStr = "Open Last Closed";
    static final String changeSearchStr = "Change Search";
    static final String openAllStr = "Open All";
    static final String untitledStr = "Untitled";
    static final String quickSearchKey = "quickSearch";
    static final String defaultQuickSearchStr =
            "http://www.google.com/search?q= w https://en.wikipedia.org/w/index.php?search=";
    static final String quickSearchUrlFormatErrorStr =
            " is an incorrect search url. The url needs to be one or two strings separated by space.";
    static final String quickSearchToDeleteNotFoundErrorStr =
            "Tried to delete quick search that doesn't exist.";
    static final String needBaseQuickSearchErrorStr =
            "Need to add a base quick search with no prefix before adding a quick search with a prefix.";
    static final String noLastCloseTabsStr = "No closed tabs to open again.";
    static final String cannotCloseLastTabStr = "Can't close last tab.";
    static final String memoryStr = "memory (MB) used %d, total %d, free %d, max %d";
    static final String urlTookStr = "Url |%s| took %s seconds to load.";
    static final String exceptionStr = "Exception |%s| for url |%s|.";
    static final String[] addRemoveStrings = {"Add/Remove A", "Add/Remove B", "Add/Remove C"};
    static final String[] bookmarkKeys = {"bookmarksA", "bookmarksB", "bookmarksC"};
    static final int urlColumns = 80;
    static final int defaultWidth = 720;
    static final int defaultHeight = 480;
    static final int mnemonicCount = 9;
    static final int defaultMaxHistoryCount = 10;
    static final int shiftBytesToMBytes = 20;
    int maxHistoryCount;
    JToolBar navigationBar;
    JToolBar bookmarksBar;
    JTextField urlField;
    JTextField statusField;
    JTabbedPane tabsPane;
    ArrayList<HtmlTab> tabs;
    Deque<HtmlTab> closedTabs;
    Preferences preferences;
    RunnableFutureFactory<String[]> futureFactory;
    LinkedList<Preferences> bookmarkPreferences;
    String[] quickSearches;
    JButton back, forward, reload, newTab, closeTab, openLastClosedTab, changeSearch;
    JButton[] addRemove;
    LinkedList<JComboBox<String>> bookmarkBoxes;
    int iCurrentTab;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                startGui();
            } catch (MalformedURLException | BackingStoreException e) {
                e.printStackTrace();
            }
        });
    }

    static void startGui() throws MalformedURLException, BackingStoreException {
        JFrame frame = new JFrame(titleStr);
        frame.add(new BasicBrowser(Preferences.userRoot().node(BasicBrowser.class.getName()),
                new SwingWorkerFactory<String[], Void>(), defaultMaxHistoryCount));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(defaultWidth, defaultHeight));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static void log(String s) {
        // System.out.println(s);
    }

    BasicBrowser(Preferences prefs, RunnableFutureFactory<String[]> factory, int historyCount)
            throws MalformedURLException, BackingStoreException {
        super(new BorderLayout());

        JPanel top = new JPanel(new GridLayout(0, 1));
        add(top, BorderLayout.PAGE_START);
        navigationBar = newToolBar(navigationBarStr, top);
        back = newButton(backStr, KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK, navigationBar,
                e -> tabs.get(iCurrentTab).goBack());
        forward = newButton(forwardStr, KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK, navigationBar,
                e -> tabs.get(iCurrentTab).goForward());
        reload = newButton(reloadStr, KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK, navigationBar,
                e -> urlUpdate(urlField.getText()));
        newTab = newButton(newTabStr, KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK, navigationBar, e -> addTab());
        closeTab = newButton(closeTabStr, KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK, navigationBar, e -> closeTab());
        openLastClosedTab = newButton(openLastClosedStr, KeyEvent.VK_T,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, navigationBar, e -> openLastClosedTab());
        changeSearch = newButton(changeSearchStr, KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK, navigationBar,
                e -> changeSearch());
        urlField = new JTextField(urlColumns);
        urlField.addActionListener(e -> urlUpdate(e.getActionCommand()));
        urlField.setFocusAccelerator('L');
        KeyStroke urlFocusStroke = KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK);
        urlField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(urlFocusStroke, "requestFocus");
        navigationBar.add(urlField);
        bookmarksBar = newToolBar(bookmarksBarStr, top);
        preferences = prefs;
        futureFactory = factory;
        loadPreferences();
        addRemove = new JButton[addRemoveStrings.length];
        for (int i = 0; i < addRemoveStrings.length; i++) {
            addRemove[i] = newButton(addRemoveStrings[i], KeyEvent.VK_D + i,
                    KeyEvent.CTRL_DOWN_MASK, bookmarksBar, this::addOrRemoveEvent);
            bookmarksBar.add(bookmarkBoxes.get(i));
        }

        statusField = new JTextField();
        statusField.setEditable(false);
        add(statusField, BorderLayout.PAGE_END);
        maxHistoryCount = historyCount;
        tabsPane = new JTabbedPane();
        tabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs = new ArrayList<>();
        closedTabs = new LinkedList<>();
        addTab(); // Uses other member variables, can be called only once those are allocated.
        iCurrentTab = tabsPane.getSelectedIndex();
        add(tabsPane, BorderLayout.CENTER);
        tabsPane.addChangeListener(event -> changeActiveTab());
        KeyStroke pageUpStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_DOWN_MASK);
        KeyStroke pageDownStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_DOWN_MASK);
        tabsPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(pageUpStroke, "navigatePageUp");
        tabsPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(pageDownStroke, "navigatePageDown");
    }

    static JToolBar newToolBar(String label, JPanel panel) {
        JToolBar toolBar = new JToolBar(label);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        panel.add(toolBar);
        return toolBar;
    }

    static JButton newButton(String text, int keyCode, int keyModifiers, JToolBar toolBar, ActionListener listener) {
        JButton button = new JButton(text);
        button.setVerticalTextPosition(AbstractButton.CENTER);
        button.setHorizontalTextPosition(AbstractButton.CENTER);
        button.setMnemonic(keyCode);
        button.setActionCommand(text);
        button.addActionListener(listener);
        KeyStroke press = KeyStroke.getKeyStroke(keyCode, keyModifiers, false);
        KeyStroke release = KeyStroke.getKeyStroke(keyCode, keyModifiers, true);
        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(press, "pressed");
        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(release, "released");
        toolBar.add(button);
        return button;
    }

    String translate(String url) {
        String result = null;
        if (url == null || url.isBlank()) {
            result = "";
        } else if (isFile(url) || url.startsWith("http://") || url.startsWith("https://")) {
            result = url;
        } else if (isUrl(url)) {
                result = "http://" + url;
        } else {
            // quickSearches[0] is the default, after that pairs of tokens and search urls, which are checked first.
            for (int i = 1; i < quickSearches.length; i += 2) {
                String prefix = quickSearches[i] + " ";
                if (url.startsWith(prefix)) {
                    result = quickSearches[i + 1] + url.substring(prefix.length());
                    break;
                }
            }
            if (result == null) {
                result = quickSearches[0] + url;
            }
        }
        return result;
    }

    void urlUpdate(String url) {
        String newUrl = translate(url);
        if (!url.equals(newUrl)) {
            urlField.setText(newUrl);
        }
        if (!newUrl.isBlank()) {
            tabs.get(iCurrentTab).urlUpdate(newUrl, iCurrentTab);
        }
    }

    void changeActiveTab() {
        log("changeActiveTab iCT %d selI %d".formatted(iCurrentTab, tabsPane.getSelectedIndex()));
        tabs.get(iCurrentTab).setUrl(urlField.getText());
        iCurrentTab = tabsPane.getSelectedIndex();
        urlField.setText(tabs.get(iCurrentTab).getUrl());
    }

    void addTab() {
        HtmlTab tab = new HtmlTab(urlField, statusField, tabsPane,
                (URL url) -> { addTab(); urlField.setText(url.toString()); urlUpdate(url.toString()); },
                futureFactory, maxHistoryCount);
        addTab(tab);
    }
    void addTab(HtmlTab tab) {
        int index = tabs.size();
        log("addTab at " + index);
        tabs.add(tab);
        tabsPane.addTab(tab.getTitle(), tab.scrollPane);
        if (index < mnemonicCount) {
            tabsPane.setMnemonicAt(index, KeyEvent.VK_1 + index);
        }
        tabsPane.setSelectedIndex(index);
        log("Called setSelectedIndex " + index);
    }

    void openLastClosedTab() {
        log("openLastClosedTab " + closedTabs.size());
        if (closedTabs.isEmpty()) {
            statusField.setText(noLastCloseTabsStr);
        } else {
            HtmlTab tab = closedTabs.pop();
            addTab(tab);
        }
    }

    void closeTab() {
        if (tabs.size() <= 1) {
            statusField.setText(cannotCloseLastTabStr);
        } else {
            int indexToRemove = iCurrentTab;
            tabs.get(indexToRemove).tryStoppingUpdater();
            tabsPane.remove(indexToRemove); // Needs to be before tabs.remove since accesses tab that will be removed.
            closedTabs.push(tabs.remove(indexToRemove));
            // Calling tabsPane.remove first results in incorrect urlField. Need to fix that here.
            urlField.setText(tabs.get(iCurrentTab).getUrl());
            if (closedTabs.size() > maxHistoryCount) {
                closedTabs.removeLast();
            }
            log("closeTab old iCT %d new iCT %d".formatted(indexToRemove, iCurrentTab));
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() >> shiftBytesToMBytes;
            long freeMemory = runtime.freeMemory() >> shiftBytesToMBytes;
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory() >> shiftBytesToMBytes;
            statusField.setText(String.format(memoryStr, usedMemory, totalMemory, freeMemory, maxMemory));
            // After tabsPane.remove(), the change listener that updates iCurrentTab is called. Use that index now.
            for (int i = iCurrentTab; i < mnemonicCount && i < tabsPane.getTabCount(); i++) {
                tabsPane.setMnemonicAt(i, KeyEvent.VK_1 + i);
            }
        }
    }

    void changeSearch() {
        String url = urlField.getText();
        String[] pair = url.split(" ");
        if (pair.length == 1) {
            if (isUrl(url)) {
                if (quickSearches == null || quickSearches.length == 0) {
                    quickSearches = new String[1];
                }
                quickSearches[0] = url;
            } else { // Need to delete something.
                int i;
                for (i = 1; i < quickSearches.length; i += 2) {
                    if (pair[0].equals(quickSearches[i])) {
                        break;
                    }
                }
                if (i < quickSearches.length) {
                    int iRemove = i;
                    String[] oldSearches = quickSearches;
                    quickSearches = new String[quickSearches.length - 2];
                    for (i = 0; i < iRemove; i++) {
                        quickSearches[i] = oldSearches[i];
                    }
                    for (; i < quickSearches.length; i++) {
                        quickSearches[i] = oldSearches[i + 2];
                    }
                } else {
                    statusField.setText(quickSearchToDeleteNotFoundErrorStr);
                    return;
                }
            }
        } else if (pair.length == 2) {
            if (quickSearches == null || quickSearches.length == 0) {
                statusField.setText(needBaseQuickSearchErrorStr);
                return;
            } else {
                int i;
                for (i = 1; i < quickSearches.length; i += 2) {
                    if (pair[0].equals(quickSearches[i])) {
                        break;
                    }
                }
                if (i >= quickSearches.length) {
                    quickSearches = Arrays.copyOf(quickSearches, quickSearches.length + 2);
                    quickSearches[i] = pair[0];
                }
                quickSearches[i + 1] = pair[1];
            }
        } else {
            statusField.setText(url + quickSearchUrlFormatErrorStr);
            return;
        }
        String searches = String.join(" ", quickSearches);
        preferences.put(quickSearchKey, searches);
        statusField.setText(searches);
    }

    void loadPreferences() throws MalformedURLException, BackingStoreException {
        String quickSearchString = preferences.get(quickSearchKey, defaultQuickSearchStr);
        quickSearches = quickSearchString.split(" ");
        bookmarkBoxes = new LinkedList<>();
        bookmarkPreferences = new LinkedList<>();
        for (String key : bookmarkKeys) {
            JComboBox<String> comboBox = new JComboBox<>();
            Preferences bookmarkNode = preferences.node(key);
            bookmarkPreferences.add(bookmarkNode);
            String[] urls = bookmarkNode.keys();
            for (String url: urls) {
                URL urlObject = new URL(url);
                comboBox.addItem(urlObject.toString());
            }
            comboBox.addItem(openAllStr);
            comboBox.addActionListener(this::openBookmarkEvent);
            bookmarkBoxes.add(comboBox);
        }
    }

    void addOrRemoveEvent(ActionEvent event) {
        String urlString = urlField.getText();
        if (!isUrl(urlString)) {
            log("not a url: " + urlString);
            return;
        }
        String command = event.getActionCommand();
        log("addOrRemoveEvent ac %s param %s".formatted(command, event.paramString()));
        for (int i = 0; i < addRemoveStrings.length; i++) {
            if (command.equals(addRemoveStrings[i])) {
                var box = bookmarkBoxes.get(i);
                Preferences bookmarkNode = bookmarkPreferences.get(i);
                log(i + " match index for " + command);
                try {
                    URL url = new URL(urlString);
                    String match = null;
                    for (int iEntry = 0; iEntry < box.getItemCount(); iEntry++) {
                        String entry = box.getItemAt(iEntry);
                        if (entry.equals(urlString)) {
                            match = entry;
                            break;
                        }
                    }
                    if (match != null) {
                        log(urlString + " url results in removing bookmark ");
                        box.removeItem(urlString);
                        bookmarkNode.remove(url.toString());
                    } else {
                        String title = tabsPane.getTitleAt(iCurrentTab);
                        log(urlString + " url results in adding bookmark " + title);
                        box.removeItemAt(box.getItemCount() - 1); // remove openALlStr and add after to be at end
                        box.addItem(urlString);
                        box.addItem(openAllStr);
                        bookmarkNode.put(url.toString(), title);
                    }
                } catch (MalformedURLException e) {
                    statusField.setText(exceptionStr.formatted(e.toString(), urlString));
                }
            }
        }
    }

    void openBookmarkEvent(ActionEvent event) {
        log("openBookmarkEvent ac %s param %s".formatted(event.getActionCommand(), event.paramString()));
        Object source = event.getSource();
        for (int i = 0; i < addRemoveStrings.length; i++) {
            JComboBox<String> box = bookmarkBoxes.get(i);
            if (source == box) {
                String selected = box.getItemAt(box.getSelectedIndex());
                if (selected == null) {
                    return; // called from addOrRemoveEvent when change by adding/deleting. Don't do anything.
                }
                log(i + " index has selected " + selected);
                if (selected.equals(openAllStr)) {
                    log("Open all");
                    for (int iEntry = 0; iEntry < (box.getItemCount() - 1); iEntry++) {
                        addTab();
                        int index = tabs.size() - 1;
                        tabs.get(index).urlUpdate(box.getItemAt(iEntry), index);
                    }
                } else {
                    log("Open one");
                    urlField.setText(selected);
                    tabs.get(iCurrentTab).urlUpdate(selected, iCurrentTab);
                }
            }
        }
    }

    static boolean isFile(String url) {
        return url.startsWith("C:\\") || url.startsWith("D:\\");
    }
    static boolean isUrl(String url) {
        return url.matches("\\S+[.:]\\w+/?\\S*");
    }

    static class HtmlTab implements HyperlinkListener {
        LinkedList<String> history;
        int iHistory;
        int maxHistoryCount;
        String title;
        JTextField urlField;
        JTextField statusField;
        JTabbedPane tabsPane;
        HTMLEditorKit kit;
        JEditorPane editorPane;
        JScrollPane scrollPane;
        Consumer<URL> addTabWithUrl;
        RunnableFutureFactory<String[]> futureFactory;

        HtmlTab(JTextField uf, JTextField sf, JTabbedPane tp, Consumer<URL> addTabWithUrlLambda,
                RunnableFutureFactory<String[]> factory, int historyCount) {
            history = new LinkedList<>();
            history.add("");
            maxHistoryCount = historyCount;
            title = untitledStr;
            iHistory = 0;
            urlField = uf;
            statusField = sf;
            tabsPane = tp;
            addTabWithUrl = addTabWithUrlLambda;
            futureFactory = factory;
            kit = new HTMLEditorKit();
            kit.setAutoFormSubmission(false);
            editorPane = new JEditorPane("text/html", "");
            editorPane.addHyperlinkListener(this);
            editorPane.setEditable(false);
            editorPane.setEditorKit(kit);
            scrollPane = new JScrollPane(editorPane);
        }

        String getTitle() {
            return title;
        }

        String getUrl() {
            log("getUrl iH %d h %s".formatted(iHistory, Arrays.toString(history.toArray())));
            return history.get(iHistory);
        }

        void setUrl(String newUrl) {
            history.set(iHistory, newUrl);
            log("setUrl iH %d h %s".formatted(iHistory, Arrays.toString(history.toArray())));
        }

        void tryStoppingUpdater() {
            futureFactory.tryCancellingFuture();
        }

        static String[] updaterDoInBackground(String url) {
            String[] result = {"", "", ""};
            long t0 = System.nanoTime();
            try {
                org.jsoup.nodes.Document soupDoc;
                if (isFile(url)) {
                    soupDoc = Jsoup.parse(new File(url), "UTF-8");
                } else {
                    soupDoc = Jsoup.connect(url).get();
                }
                Cleaner cleaner = new Cleaner(Whitelist.relaxed());
                var cleanDoc = cleaner.clean(soupDoc);
                result[0] = soupDoc.title();
                result[1] = cleanDoc.html();
            } catch (IOException e) {
                result[0] = String.format(exceptionStr, e.toString(), url);
            } finally {
                result[2] = String.format("%.2f", (System.nanoTime() - t0) * 1e-9);
            }
            return result;
        }

        void updaterDone(String url, int index, String[] text) {
            String body = text[1];
            if (body.isEmpty()) {
                statusField.setText(text[0]);
            } else {
                title = text[0];
                tabsPane.setTitleAt(index, title);
                editorPane.setText(body);
                log(title + " done SwingWorker " + body.length());
                editorPane.setCaretPosition(0);
                statusField.setText(String.format(urlTookStr, url, text[2]));
            }
        }

        void updaterRunFinally(String url, String error) {
            futureFactory.clearFuture();
            if (error != null && !error.isEmpty()) {
                statusField.setText(String.format(exceptionStr, error, url));
            }
        }

        void urlUpdate(String url, int index) {
            setUrl(url);
            log(url + " urlUpdate " + index);
            futureFactory.createFuture( () -> updaterDoInBackground(url),
                    (text) -> updaterDone(url, index, text),
                    (error) -> updaterRunFinally(url, error));
            futureFactory.runFuture();
        }

        void goBack() {
            if (iHistory <= 0) {
                return;
            }
            iHistory--;
            String url = history.get(iHistory);
            urlField.setText(url);
            urlUpdate(url, tabsPane.getSelectedIndex());
        }

        void goForward() {
            if (iHistory >= (history.size() - 1)) {
                return;
            }
            iHistory++;
            String url = history.get(iHistory);
            urlField.setText(url);
            urlUpdate(url, tabsPane.getSelectedIndex());
        }

        @Override
        public void hyperlinkUpdate(HyperlinkEvent event) {
            if (!(event instanceof FormSubmitEvent)) {
                // else log("data |%s| url |%s| desc |%s| event type |%s|".formatted(((FormSubmitEvent)fse).getData(), fse.getURL(), fse.getDescription(), fse.getEventType()));
                URL url = event.getURL();
                String urlString = url.toString();
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    boolean controlDown = event.getInputEvent().isControlDown();
                    log(controlDown + "controlDown, url: " + url);
                    if (controlDown) {
                        addTabWithUrl.accept(url);
                    } else {
                        urlField.setText(urlString);
                        iHistory++;
                        if (iHistory == history.size()) {
                            history.add(""); // will be populated by urlUpdate
                            if (iHistory >= maxHistoryCount) {
                                history.remove(0);
                                iHistory--;
                            }
                        } else {
                            // Need to clear future info since following link invalidates it.
                            history.subList(iHistory + 1, history.size()).clear();
                        }
                        urlUpdate(urlString, tabsPane.getSelectedIndex());
                    }
                } else if (event.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    statusField.setText(urlString);
                } else { // HyperlinkEvent.EventType.EXITED
                    statusField.setText("");
                }
            }
        }
    }
}