import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OverlayVanillaMap<K, V> extends OverlayMap<K, V> {

  public OverlayVanillaMap(Map<K, V> parent, Map<K, V> overlay) {
    super(parent, overlay);
  }

  @Override
  public Set<K> keySet() {
    return Stream.concat(this.parent.keySet().stream(), this.overlay.keySet().stream()).collect(Collectors.toSet());
  }

  @Override
  public Collection<V> values() {
    return Stream.concat(this.parent.values().stream(), this.overlay.values().stream()).collect(Collectors.toList());
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return Stream.concat(this.parent.entrySet().stream(), this.overlay.entrySet().stream()).collect(Collectors.toSet());
  }
}