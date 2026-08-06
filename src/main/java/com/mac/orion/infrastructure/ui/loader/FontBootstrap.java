package com.mac.orion.infrastructure.ui.loader;


import javafx.scene.text.Font;

import java.net.URL;
import java.util.Objects;

public class FontBootstrap {


  public static void loadAll() {
    load("/ui/fonts/roboto/Roboto-Regular.ttf");
    load("/ui/fonts/roboto/Roboto-Bold.ttf");
    load("/ui/fonts/roboto/Roboto-Black.ttf");
    load("/ui/fonts/roboto/RobotoFlex-Regular.ttf");
    load("/ui/fonts/roboto_mono/RobotoMonoNerdFont-Regular.ttf");
    load("/ui/fonts/roboto_mono/RobotoMonoNerdFont-Medium.ttf");
    load("/ui/fonts/roboto_mono/RobotoMonoNerdFont-Bold.ttf");
    load("/ui/fonts/heavyData/HeavyDataNerdFont-Regular.ttf");
    load("/ui/fonts/heavyData/HeavyDataNerdFontPropo-Regular.ttf");
  }


  private static void load(String path) {
    URL url = FontBootstrap.class.getResource(path);
    Objects.requireNonNull(url, "resource not found: " + path);

    Font f = Font.loadFont(url.toExternalForm(), 10);
    if (f == null) throw new IllegalStateException("Cannot load: " + path);

    System.out.println("Loading font: " + f.getFamily() + " | " + f.getName());
  }


}
