import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

  private static final Color[] colors = new Color[] {
      null,
      color(127, 178, 56),
      color(247, 233, 163),
      color(199, 199, 199),
      color(255, 0, 0),
      color(160, 160, 255),
      color(167, 167, 167),
      color(0, 124, 0),
      color(255, 255, 255),
      color(164, 168, 184),
      color(151, 109, 77),
      color(112, 112, 112),
      color(64, 64, 255),
      color(143, 119, 72),
      color(255, 252, 245),
      color(216, 127, 51),
      color(178, 76, 216),
      color(102, 153, 216),
      color(229, 229, 51),
      color(127, 204, 25),
      color(242, 127, 165),
      color(76, 76, 76),
      color(153, 153, 153),
      color(76, 127, 153),
      color(127, 63, 178),
      color(51, 76, 178),
      color(102, 76, 51),
      color(102, 127, 51),
      color(153, 51, 51),
      color(25, 25, 25),
      color(250, 238, 77),
      color(92, 219, 213),
      color(74, 128, 255),
      color(0, 217, 58),
      color(129, 86, 49),
      color(112, 2, 0, MapVersion.MINECRAFT_1_8),
      color(209, 177, 161, MapVersion.MINECRAFT_1_12),
      color(159, 82, 36, MapVersion.MINECRAFT_1_12),
      color(149, 87, 108, MapVersion.MINECRAFT_1_12),
      color(112, 108, 138, MapVersion.MINECRAFT_1_12),
      color(186, 133, 36, MapVersion.MINECRAFT_1_12),
      color(103, 117, 53, MapVersion.MINECRAFT_1_12),
      color(160, 77, 78, MapVersion.MINECRAFT_1_12),
      color(57, 41, 35, MapVersion.MINECRAFT_1_12),
      color(135, 107, 98, MapVersion.MINECRAFT_1_12),
      color(87, 92, 92, MapVersion.MINECRAFT_1_12),
      color(122, 73, 88, MapVersion.MINECRAFT_1_12),
      color(76, 62, 92, MapVersion.MINECRAFT_1_12),
      color(76, 50, 35, MapVersion.MINECRAFT_1_12),
      color(76, 82, 42, MapVersion.MINECRAFT_1_12),
      color(142, 60, 46, MapVersion.MINECRAFT_1_12),
      color(37, 22, 16, MapVersion.MINECRAFT_1_12),
      color(189, 48, 49, MapVersion.MINECRAFT_1_16),
      color(148, 63, 97, MapVersion.MINECRAFT_1_16),
      color(92, 25, 29, MapVersion.MINECRAFT_1_16),
      color(22, 126, 134, MapVersion.MINECRAFT_1_16),
      color(58, 142, 140, MapVersion.MINECRAFT_1_16),
      color(86, 44, 62, MapVersion.MINECRAFT_1_16),
      color(20, 180, 133, MapVersion.MINECRAFT_1_16),
      color(100, 100, 100, MapVersion.MINECRAFT_1_17),
      color(216, 175, 147, MapVersion.MINECRAFT_1_17),
      color(127, 167, 150, MapVersion.MINECRAFT_1_17)
  };

  private static final byte MINECRAFT_MULTIPLIER = 4;

  private static final Map<MapVersion, Map<Color, Byte>> colorToIndexMap = new EnumMap<>(MapVersion.class);

  private static final Map<MapVersion, byte[]> remapBuffers = new EnumMap<>(MapVersion.class);

  private static final byte[] mainBuffer = new byte[256 * 256 * 256];

  public static void main(String[] args) {
    Map<Color, Byte> previous = new ConcurrentHashMap<>();
    for (MapVersion version : MapVersion.values()) {
      Map<Color, Byte> current = new OverlayVanillaMap<>(previous, new ConcurrentHashMap<>());
      colorToIndexMap.put(version, current);
      previous = current;

      remapBuffers.put(version, new byte[colors.length * MINECRAFT_MULTIPLIER]);
    }

    for (byte i = 0; i < colors.length; i++) {
      Color color = colors[i];
      if (color == null) {
        continue;
      }

      for (byte j = 0; j < MINECRAFT_MULTIPLIER; ++j) {
        int index = i * MINECRAFT_MULTIPLIER + j;
        colorToIndexMap.get(color.getSince()).put(color.multiply(j), (byte) (index));
      }
    }

    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    int tasksCounter = 0;

    for (int idx = 0; idx < 256; ++idx) {
      int r = idx;
      executor.execute(() -> {
        for (int g = 0; g < 256; ++g) {
          for (int b = 0; b < 256; ++b) {
            Color color = new Color(r, g, b);
            mainBuffer[color.toIndex()] = matchColor(color, MapVersion.MAXIMUM_VERSION);
          }
        }
      });
      ++tasksCounter;
    }

    for (MapVersion version : MapVersion.values()) {
      byte[] remapBuffer = remapBuffers.get(version);
      for (byte i = 0; i < colors.length; i++) {
        byte idx = i;
        Color preColor = colors[idx];
        if (preColor == null) {
          continue;
        }

        executor.execute(() -> {
          for (int ix = 0; ix < MINECRAFT_MULTIPLIER; ++ix) {
            Color color = preColor.multiply(ix);
            byte remappedColor;
            if (color.getSince().compareTo(version) <= 0) {
              remappedColor = (byte) ((idx * 4) + ix);
            } else {
              remappedColor = matchColor(color, version);
            }
            remapBuffer[idx * 4 + ix] = remappedColor;
          }
        });
        ++tasksCounter;
      }
    }

    while (executor.getCompletedTaskCount() != tasksCounter) {
      try {
        Thread.sleep(2500L);
        System.out.println(executor.getCompletedTaskCount() * 100 / tasksCounter + "% done");
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    saveBuffer("colors_main_map", mainBuffer);

    for (MapVersion version : MapVersion.values()) {
      saveBuffer("colors_" + version.toString().toLowerCase(Locale.ROOT) + "_map", remapBuffers.get(version));
    }
  }

  private static void saveBuffer(String filename, byte[] buffer) {
    try (FileOutputStream fos = new FileOutputStream(filename)) {
      fos.write(buffer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static double getDistance(Color c1, Color c2) {
    double rmean = (double) (c1.getRed() + c2.getRed()) / 2.0;
    double r = c1.getRed() - c2.getRed();
    double g = c1.getGreen() - c2.getGreen();
    int b = c1.getBlue() - c2.getBlue();
    double weightR = 2.0 + rmean / 256.0;
    double weightG = 4.0;
    double weightB = 2.0 + (255.0 - rmean) / 256.0;

    return weightR * r * r + weightG * g * g + weightB * (double) b * (double) b;
  }

  public static byte matchColor(Color color, MapVersion version) {
    Color match = colors[1];
    double best = -1.0;

    for (Color cachedColor : colorToIndexMap.get(version).keySet()) {
      double distance = getDistance(color, cachedColor);

      if (distance < best || best == -1.0) {
        best = distance;
        match = cachedColor;
      }
    }

    return colorToIndexMap.get(version).get(match);
  }

  private static Color color(int red, int green, int blue) {
    return new Color(red, green, blue);
  }

  private static Color color(int red, int green, int blue, MapVersion since) {
    return new Color(red, green, blue, since);
  }

  public static class Color {

    private final int red;
    private final int green;
    private final int blue;
    private final MapVersion since;

    public Color(int red, int green, int blue) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.since = MapVersion.MINIMUM_VERSION;
    }

    public Color(int red, int green, int blue, MapVersion since) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.since = since;
    }

    public Color multiply(int multiplier) {
      switch (multiplier) {
        case 0: {
          int red = (this.red * 180) >> 8;
          int green = (this.green * 180) >> 8;
          int blue = (this.blue * 180) >> 8;
          return new Color(red, green, blue, this.since);
        }
        case 1: {
          int red = (this.red * 220) >> 8;
          int green = (this.green * 220) >> 8;
          int blue = (this.blue * 220) >> 8;
          return new Color(red, green, blue, this.since);
        }
        case 2: {
          return new Color(this.red, this.green, this.blue, this.since);
        }
        case 3: {
          int red = (this.red * 135) >> 8;
          int green = (this.green * 135) >> 8;
          int blue = (this.blue * 135) >> 8;
          return new Color(red, green, blue, this.since);
        }
        default: {
          return this;
        }
      }
    }

    public int toIndex() {
      return (this.red << 16) | (this.green << 8) | (this.blue);
    }

    public int getRed() {
      return this.red;
    }

    public int getGreen() {
      return this.green;
    }

    public int getBlue() {
      return this.blue;
    }

    public MapVersion getSince() {
      return this.since;
    }

    @Override
    public int hashCode() {
      return this.toIndex();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Color) {
        Color checkColor = (Color) obj;
        return checkColor.red == this.red && checkColor.blue == this.blue && checkColor.green == this.green;
      } else {
        return false;
      }
    }
  }

  private enum MapVersion {
    MINIMUM_VERSION,
    MINECRAFT_1_8,
    MINECRAFT_1_12,
    MINECRAFT_1_16,
    MINECRAFT_1_17;

    static final MapVersion MAXIMUM_VERSION = MINECRAFT_1_17;
  }

}