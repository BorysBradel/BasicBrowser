import java.io.OutputStream;
import java.util.HashMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.NodeChangeListener;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

public class MockPreferences extends Preferences {

    HashMap<String, String> map = new HashMap<>();
    HashMap<String, MockPreferences> nodes = new HashMap<>();

    @Override
    public void put(String key, String value) {
        map.put(key, value);
    }

    @Override
    public String get(String key, String def) {
        return map.getOrDefault(key, def);
    }

    @Override
    public void remove(String key) {
        map.remove(key);
    }

    @Override
    public void clear() throws BackingStoreException {
        throw new BackingStoreException("not implemented");
    }

    @Override
    public void putInt(String key, int value) {

    }

    @Override
    public int getInt(String key, int def) {
        return 0;
    }

    @Override
    public void putLong(String key, long value) {

    }

    @Override
    public long getLong(String key, long def) {
        return 0;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        map.put(key, Boolean.toString(value));
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        if (!map.containsKey(key)) {
            return def;
        }
        return Boolean.parseBoolean(map.get(key));
    }

    @Override
    public void putFloat(String key, float value) {

    }

    @Override
    public float getFloat(String key, float def) {
        return 0;
    }

    @Override
    public void putDouble(String key, double value) {

    }

    @Override
    public double getDouble(String key, double def) {
        return 0;
    }

    @Override
    public void putByteArray(String key, byte[] value) {

    }

    @Override
    public byte[] getByteArray(String key, byte[] def) {
        return new byte[0];
    }

    @Override
    public String[] keys() {
        return map.keySet().toArray(new String[0]);
    }

    @Override
    public String[] childrenNames() throws BackingStoreException {
        throw new BackingStoreException("not implemented");
    }

    @Override
    public Preferences parent() {
        return null;
    }

    @Override
    public Preferences node(String pathName) {
        if (nodes.containsKey(pathName)) {
            return nodes.get(pathName);
        }
        MockPreferences newNode = new MockPreferences();
        nodes.put(pathName, newNode);
        return newNode;
    }

    @Override
    public boolean nodeExists(String pathName) throws BackingStoreException {
        throw new BackingStoreException("not implemented");
    }

    @Override
    public void removeNode() throws BackingStoreException {
        throw new BackingStoreException("not implemented");
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String absolutePath() {
        return null;
    }

    @Override
    public boolean isUserNode() {
        return false;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public void flush() throws BackingStoreException {
        throw new BackingStoreException("not implemented");
    }

    @Override
    public void sync() throws BackingStoreException {
        throw new BackingStoreException("not implemented");
    }

    @Override
    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {

    }

    @Override
    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {

    }

    @Override
    public void addNodeChangeListener(NodeChangeListener ncl) {

    }

    @Override
    public void removeNodeChangeListener(NodeChangeListener ncl) {

    }

    @Override
    public void exportNode(OutputStream os) throws BackingStoreException {
        throw new BackingStoreException("not implemented");
    }

    @Override
    public void exportSubtree(OutputStream os) throws BackingStoreException {
        throw new BackingStoreException("not implemented");
    }
}
