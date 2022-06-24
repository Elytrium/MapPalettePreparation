import java.util.Map;

public abstract class OverlayMap<K, V> implements Map<K, V> {

  protected final Map<K, V> parent;
  protected final Map<K, V> overlay;

  public OverlayMap(Map<K, V> parent, Map<K, V> overlay) {
    this.parent = parent;
    this.overlay = overlay;
  }

  @Override
  public int size() {
    return this.parent.size() + this.overlay.size();
  }

  @Override
  public boolean isEmpty() {
    return this.parent.isEmpty() && this.overlay.isEmpty();
  }

  @Override
  public boolean containsKey(Object o) {
    return this.parent.containsKey(o) || this.overlay.containsKey(o);
  }

  @Override
  public boolean containsValue(Object o) {
    return this.parent.containsValue(o) || this.overlay.containsValue(o);
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public V get(Object o) {
    if (this.overlay.containsKey(o)) {
      return this.overlay.get(o);
    }

    return this.parent.get(o);
  }

  @Override
  public V put(K k, V v) {
    return this.overlay.put(k, v);
  }

  @Override
  public V remove(Object o) {
    return this.overlay.remove(o);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    this.overlay.putAll(map);
  }

  @Override
  public void clear() {
    this.overlay.clear();
  }
}
