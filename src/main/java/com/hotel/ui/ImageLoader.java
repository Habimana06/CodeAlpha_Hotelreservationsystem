package com.hotel.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public final class ImageLoader {
    private ImageLoader() {
    }

    public static ImageIcon load(String path, int width, int height) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            Image image = ImageIO.read(in).getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(image);
        } catch (IOException e) {
            return null;
        }
    }
}
