package com.github.syuchan1005.katakatataaaaaaan;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.util.ImageLoader;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class KeyDownService implements ApplicationComponent {
    @NotNull
    private static Map<String, Image> imageMap = new HashMap<>();

    private void showImage(EditorImpl editorEx, char c) {
        Point cursorAbsoluteLocation = editorEx.visualPositionToXY(editorEx.getCaretModel().getVisualPosition());
        Point editorLocation = editorEx.getComponent().getLocationOnScreen();
        Point editorContentLocation = editorEx.getContentComponent().getLocationOnScreen();
        Point popupLocation = new Point(editorContentLocation.x + cursorAbsoluteLocation.x,
                editorLocation.y + cursorAbsoluteLocation.y - editorEx.getScrollingModel().getVerticalScrollOffset());
        JRootPane rootPane = SwingUtilities.getRootPane(editorEx.getComponent());
        addImage(c == '\n', rootPane.getLayeredPane(),
                new Point(popupLocation.x - rootPane.getLocationOnScreen().x, popupLocation.y - rootPane.getLocationOnScreen().y));
    }

    private void addImage(boolean isEnter, JLayeredPane pane, Point caretPosition) {
        String key = (isEnter ? "tan_" : "kata_") + rand(1, imageMap.size() / 2);
        Image img = resizeAlphaImage(imageMap.get(key), isEnter ? rand(80, 100) : rand(10, 20), 255);

        int[] beforePos = new int[]{rand(-10, 10), rand(-10, 10)};
        double[] afterData = new double[]{rand(-40, 40) - beforePos[0], rand(-40, 40) - beforePos[1], isEnter ? rand(30, 50) : rand(10, 20), 255};

        Rectangle rectangle = new Rectangle(caretPosition.x + beforePos[0], caretPosition.y + beforePos[1],
                ImageUtil.getRealWidth(img), ImageUtil.getRealHeight(img));
        JLabel label = new JLabel(new ImageIcon(img));
        label.setBounds(rectangle);
        pane.add(label);
        pane.setLayer(label, JLayeredPane.PALETTE_LAYER);

        animate(pane, (count) -> {
            for (int i = 0; i < afterData.length; i++) {
                afterData[i] /= count;
            }
        }, (i) -> {
            Image processImage = resizeAlphaImage(img, (int) (ImageUtil.getRealWidth(img) + (afterData[2] * i)), 255 - (int) (afterData[3] * i));
            label.setIcon(new ImageIcon(processImage));
            label.setBounds((int) (rectangle.x + afterData[0] * i), (int) (rectangle.y + afterData[1] * i), ImageUtil.getRealWidth(processImage), ImageUtil.getRealHeight(processImage));
        }, () -> pane.remove(label), 50, 500);
    }

    private static Image resizeAlphaImage(@NotNull Image image, int width, int alpha) {
        double scale = (double) width / ImageUtil.getRealWidth(image);
        Image scaleImage = (scale == 1) ? image : ImageUtil.scaleImage(image, scale);
        BufferedImage bufferedImage = ImageUtil.toBufferedImage(scaleImage);
        for (int x = 0; x < ImageUtil.getRealWidth(scaleImage); x++) {
            for (int y = 0; y < ImageUtil.getRealHeight(scaleImage); y++) {
                /* inject alphaValue */
                bufferedImage.setRGB(x, y, bufferedImage.getRGB(x, y) & ((alpha << 24) | 0x00FFFFFF));
            }
        }
        return bufferedImage;
    }

    private static void animate(JComponent component, Consumer<Integer> init, Consumer<Integer> animationFrame, Runnable finish, int perMills, int finishMills) {
        int count = (finishMills - (finishMills % perMills == 0 ? 1 : 0)) / perMills;
        int endDelay = finishMills - (perMills * count);
        init.accept(count);
        new Thread(() -> {
            try {
                for (int i = 0; i < count; i++) {
                    Thread.sleep(perMills);
                    animationFrame.accept(i + 1);
                    component.repaint();
                }
                Thread.sleep(endDelay);
                finish.run();
                component.repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static int rand(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private List<Character> escapes = Arrays.asList('\t', '\b', '\n', '\r', '\f', '\'');

    @Override
    public void initComponent() {
        if (imageMap.size() == 0) {
            System.out.println(UIUtil.isUnderDarcula());
            /* UIUtil.isUnderDarcula() returned false when use darcula (in static block) */
            for (int i = 1; i <= 4; i++) {
                imageMap.put("tan_" + i, Objects.requireNonNull(ImageLoader.loadFromResource("/images/tan_" + i + ".svg")));
                imageMap.put("kata_" + i, Objects.requireNonNull(ImageLoader.loadFromResource("/images/kata_" + i + ".svg")));
            }
        }

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof KeyEvent)) return;
            KeyEvent keyEvent = ((KeyEvent) event);
            if (event.getID() != KeyEvent.KEY_RELEASED && escapes.indexOf(keyEvent.getKeyChar()) == -1) return;
            if (!(keyEvent.getComponent() instanceof EditorComponentImpl)) return;
            EditorImpl editor = ((EditorComponentImpl) keyEvent.getComponent()).getEditor();
            showImage(editor, ((KeyEvent) event).getKeyChar());
        }, AWTEvent.KEY_EVENT_MASK);
    }
}
