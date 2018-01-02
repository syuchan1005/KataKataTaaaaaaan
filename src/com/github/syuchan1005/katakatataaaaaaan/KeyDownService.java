package com.github.syuchan1005.katakatataaaaaaan;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.util.ImageLoader;
import com.intellij.util.ui.ImageUtil;
import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import sun.awt.image.ToolkitImage;

public class KeyDownService implements ProjectComponent {
	@NotNull
	private static Map<String, ToolkitImage> imageMap = new HashMap<>();

	static {
		for (int i = 1; i <= 4; i++) {
			imageMap.put("tan_" + i, ((ToolkitImage) ImageLoader.loadFromResource("/images/tan_" + i + ".png")));
			imageMap.put("kata_" + i, ((ToolkitImage) ImageLoader.loadFromResource("/images/kata_" + i + ".png")));
		}
	}

	private void keyDownAddImage(EditorImpl editorEx, char c) {
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
		ToolkitImage image = imageMap.get((isEnter ? "tan_" : "kata_") + rand(1, 4));
		BufferedImage img = processImage(image, isEnter ? rand(80, 100) : rand(10, 20), 255);
		JLabel label = new JLabel(new ImageIcon(img));
		int[] beforePos = new int[]{rand(-10, 10), rand(-10, 10)};
		Rectangle rectangle = new Rectangle(caretPosition.x + beforePos[0], caretPosition.y + beforePos[1], img.getWidth(), img.getHeight());
		label.setBounds(rectangle);
		pane.add(label);
		pane.setLayer(label, JLayeredPane.PALETTE_LAYER);
		double[] afterData = new double[]{rand(-40, 40) - beforePos[0], rand(-40, 40) - beforePos[1], isEnter ? rand(30, 50) : rand(10, 20), 255};
		animate(pane, (count) -> {
			for (int i = 0; i < afterData.length; i++) {
				afterData[i] /= count;
			}
		}, (i) -> {
			BufferedImage processImage = processImage(image, (int) (img.getWidth() + (afterData[2] * i)), 255 - (int) (afterData[3] * i));
			label.setIcon(new ImageIcon(processImage));
			label.setBounds((int) (rectangle.x + afterData[0] * i), (int) (rectangle.y + afterData[1] * i), processImage.getWidth(), processImage.getHeight());
		}, () -> pane.remove(label), 50, 500);
	}

	private static BufferedImage processImage(ToolkitImage image, int width, int alpha) {
		double scale = (double) width / image.getWidth();
		BufferedImage scaleImage = (BufferedImage) ImageUtil.scaleImage(image, scale == 1 ? 0.99 : scale);
		for (int x = 0; x < scaleImage.getWidth(); x++) {
			for (int y = 0; y < scaleImage.getHeight(); y++) {
				scaleImage.setRGB(x, y, scaleImage.getRGB(x, y) & ((alpha << 24) | 0x00FFFFFF));
			}
		}
		return scaleImage;
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

	private static Random random = new Random();

	private static int rand(int min, int max) {
		return random.nextInt(max - min + 1) + min;
	}


	private List<Character> escapes = Arrays.asList('\t', '\b', '\n', '\r', '\f', '\'');

	@Override
	public void projectOpened() {
		Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
			if (!(event instanceof KeyEvent)) return;
			KeyEvent keyEvent = ((KeyEvent) event);
			if (event.getID() != KeyEvent.KEY_RELEASED && escapes.indexOf(keyEvent.getKeyChar()) == -1) return;
			if (!(keyEvent.getComponent() instanceof EditorComponentImpl)) return;
			EditorImpl editor = ((EditorComponentImpl) keyEvent.getComponent()).getEditor();
			keyDownAddImage(editor, ((KeyEvent) event).getKeyChar());
		}, AWTEvent.KEY_EVENT_MASK);
	}
}
