package org.thoughtcrime.securesms.color;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MaterialColors {

  // JW: put original colors back, plus some extra
  public static final MaterialColorList CONVERSATION_PALETTE = new MaterialColorList(new ArrayList<>(Arrays.asList(
      MaterialColor.RED,
      MaterialColor.PINK,
      MaterialColor.PURPLE,
      MaterialColor.DEEP_PURPLE,
      MaterialColor.INDIGO,
      MaterialColor.BLUE,
      MaterialColor.LIGHT_BLUE,
      MaterialColor.CYAN,
      MaterialColor.TEAL,
      MaterialColor.GREEN,
      MaterialColor.LIGHT_GREEN,
      MaterialColor.LIME,
      MaterialColor.YELLOW,
      MaterialColor.ORANGE,
      MaterialColor.DEEP_ORANGE,
      MaterialColor.BROWN,
      MaterialColor.AMBER,
      MaterialColor.GREY,
      MaterialColor.BLUE_GREY,
      MaterialColor.ULTRAMARINE,
      MaterialColor.TEALS,
      MaterialColor.BURLAP,
      MaterialColor.TAUPE,
      MaterialColor.BLACK
  )));

  public static class MaterialColorList {

    private final List<MaterialColor> colors;

    private MaterialColorList(List<MaterialColor> colors) {
      this.colors = colors;
    }

    public MaterialColor get(int index) {
      return colors.get(index);
    }

    public int size() {
      return colors.size();
    }

    public @Nullable MaterialColor getByColor(Context context, int colorValue) {
      for (MaterialColor color : colors) {
        if (color.represents(context, colorValue)) {
          return color;
        }
      }

      return null;
    }

    public @ColorInt int[] asConversationColorArray(@NonNull Context context) {
      int[] results = new int[colors.size()];
      int   index   = 0;

      for (MaterialColor color : colors) {
        results[index++] = color.toConversationColor(context);
      }

      return results;
    }
  }
}

