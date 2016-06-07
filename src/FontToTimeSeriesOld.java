import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.github.sarxos.webcam.Webcam;

// รับภาษาไทยได้

public class FontToTimeSeriesOld {

	private static String fontNames[];
	private String currentFont;

	private int mode = 0; // 0:text 1:cam
	private Dimension d = new Dimension(320, 240);
	private BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
	private BufferedImage imgr = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
	private Color selectedColor = Color.BLACK;
	private int threshold = 120;

	public FontToTimeSeriesOld() {
		initializeFont();
		initializeFrame();
	}

	private void initializeFont() {
		if (fontNames == null) {
			Font[] fs = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
			fontNames = new String[fs.length];
			for (int i = 0; i < fs.length; i++) {
				fontNames[i] = fs[i].getFontName();
			}
			currentFont = fontNames[0];
		}
	}

	private void initializeFrame() {
		JFrame f = new JFrame("Font To TimeSeries");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setAlwaysOnTop(true);
		Container c = f.getContentPane();
		c.setLayout(new BorderLayout());
		JComboBox<String> cb = new JComboBox<String>(fontNames);
		c.add(cb, BorderLayout.NORTH);
		JTextField t = new JTextField(1);
		t.setFont(new Font(currentFont, Font.PLAIN, 200));
		t.setHorizontalAlignment(JTextField.CENTER);
		t.setBackground(Color.YELLOW);
		t.setForeground(Color.BLUE);
		t.setCaretColor(Color.RED);
		c.add(t, BorderLayout.CENTER);
		JPanel p = new JPanel();
		JButton b = new JButton("Text/Cam");
		p.add(b);
		JButton b2 = new JButton("               Convert!               ");
		p.add(b2);
		c.add(p, BorderLayout.SOUTH);
		JSlider s = new JSlider(SwingConstants.HORIZONTAL, 0, 700, threshold);
		s.setPaintTicks(true);
		s.setPaintLabels(true);
		s.setMajorTickSpacing(100);
		s.setMinorTickSpacing(25);
		JLabel l = new JLabel(new ImageIcon(img));
		cb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				currentFont = (String) e.getItem();
				t.setFont(new Font(currentFont, Font.PLAIN, 200));
			}
		});
		t.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				((JTextField) e.getSource()).setText("");
			}
		});
		b2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (mode == 0) {
					if (t.getText().length() == 1) {
						new Display(currentFont, t.getText().charAt(0));
					} else {
						JOptionPane.showMessageDialog(f, "Character Invalid", "ERROR", JOptionPane.ERROR_MESSAGE);
					}
				} else if (mode == 1) {
					new Display(img);
				}
			}
		});
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (mode == 0) {
					mode = 1;
					f.remove(cb);
					f.remove(t);
					f.add(s, BorderLayout.NORTH);
					f.add(l, BorderLayout.CENTER);
					f.pack();
				} else if (mode == 1) {
					mode = 0;
					f.remove(s);
					f.remove(l);
					f.add(cb, BorderLayout.NORTH);
					f.add(t, BorderLayout.CENTER);
					f.pack();
				}
			}
		});
		s.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				threshold = ((JSlider) e.getSource()).getValue();
			}
		});
		l.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				selectedColor = new Color(imgr.getRGB(e.getX(), e.getY()));
			}
		});
		f.pack();
		f.setVisible(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (mode != 1) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Graphics2D g = img.createGraphics();
				g.setColor(Color.BLACK);
				g.drawString("Loading Webcam...", 100, 100);
				f.repaint();
				try {
					Thread.sleep(20);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				Webcam w = Webcam.getDefault();
				w.setViewSize(d);
				w.open();
				while (true) {
					if (mode == 1) {
						try {
							imgr = w.getImage();
							g.drawImage(extract(imgr), 0, 0, null);
							f.repaint();
							Thread.sleep(20);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}

	private BufferedImage extract(BufferedImage img) {
		BufferedImage im = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < img.getHeight(); i++) {
			for (int j = 0; j < img.getWidth(); j++) {
				if (distance(img.getRGB(j, i), selectedColor) < threshold) {
					im.setRGB(j, i, Color.WHITE.getRGB());
				} else {
					im.setRGB(j, i, Color.BLACK.getRGB());
				}
			}
		}
		return im;
	}

	private int distance(int rgb, Color c) {
		Color d = new Color(rgb);
		int sum = 0;
		sum += Math.abs(d.getRed() - c.getRed());
		sum += Math.abs(d.getGreen() - c.getGreen());
		sum += Math.abs(d.getBlue() - c.getBlue());
		return sum;
	}

	public static void main(String[] args) {
		new FontToTimeSeriesOld();
	}

	static class Display implements Runnable {
		private static final int GRAPH_HEIGHT = 400, GRAPH_WIDTH = 1100, CHAR_OFFSET = 30, WINDOW_SIZE = 20;
		// private static final int DIRS[][] = new int[][] { { -1, 0 }, { 0, 1
		// }, { 1, 0 }, { 0, -1 } };
		private static final int DIRS[][] = new int[][] { { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 0 }, { 1, 1 }, { -1, 1 },
				{ -1, -1 }, { 1, -1 } };
		private ArrayList<Point> borders = new ArrayList<Point>();
		private ArrayList<Float> angles = new ArrayList<Float>();

		private int charHeight, charWidth;
		private BufferedImage img, imgChar, imgGraph;
		private JFrame f;

		private Thread t;

		public Display(String font, char ch) {
			f = new JFrame(font + " --- " + ch);
			f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			f.setAlwaysOnTop(true);
			Container c = f.getContentPane();
			Font ff = new Font(font, Font.PLAIN, 300);
			FontMetrics fm = f.getFontMetrics(ff);
			charHeight = fm.getHeight();
			charWidth = fm.charWidth(ch);
			img = new BufferedImage(charWidth + GRAPH_WIDTH + 2 * CHAR_OFFSET,
					Math.max(charHeight + 2 * CHAR_OFFSET, GRAPH_HEIGHT), BufferedImage.TYPE_INT_ARGB);
			imgChar = new BufferedImage(charWidth + 2 * CHAR_OFFSET, charHeight + 2 * CHAR_OFFSET,
					BufferedImage.TYPE_INT_ARGB);
			imgGraph = new BufferedImage(GRAPH_WIDTH, GRAPH_HEIGHT, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = imgChar.createGraphics();
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, imgChar.getWidth(), imgChar.getHeight());
			g.setColor(Color.WHITE);
			g.setFont(ff);
			g.drawString(ch + "", CHAR_OFFSET, charHeight - fm.getDescent() + CHAR_OFFSET);
			c.add(new JLabel(new ImageIcon(img)));
			f.pack();
			f.setVisible(true);
			t = new Thread(null, this, "UpdateDrawing", 9999);
			t.start();
		}

		public Display(BufferedImage imgc) {
			f = new JFrame("Captured Image");
			f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			f.setAlwaysOnTop(true);
			Container c = f.getContentPane();
			img = new BufferedImage(imgc.getWidth() + GRAPH_WIDTH, Math.max(imgc.getHeight(), GRAPH_HEIGHT),
					BufferedImage.TYPE_INT_ARGB);
			imgChar = new BufferedImage(imgc.getWidth(), imgc.getHeight(), imgc.getType());
			imgChar.createGraphics().drawImage(imgc, 0, 0, null);
			imgGraph = new BufferedImage(GRAPH_WIDTH, GRAPH_HEIGHT, BufferedImage.TYPE_INT_ARGB);
			c.add(new JLabel(new ImageIcon(img)));
			f.pack();
			f.setVisible(true);
			t = new Thread(null, this, "UpdateDrawing", 9999);
			t.start();
		}

		@Override
		public void run() {
			Graphics2D g = imgGraph.createGraphics();
			new Thread(null, new Runnable() {
				@Override
				public void run() {
					findBorder(imgChar);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					t.interrupt();
				}
			}, "FindBorder", 99999999).start();
			Graphics2D g2d = img.createGraphics();
			g2d.setColor(Color.BLACK);
			while (true) {
				try {
					int baseY = Math.max(0, imgGraph.getHeight() - imgChar.getHeight());
					g2d.drawImage(imgChar, 0, baseY, null);
					g.setColor(Color.WHITE);
					g.fillRect(0, 0, imgGraph.getWidth(), imgGraph.getHeight());
					synchronized (angles) {
						@SuppressWarnings("unchecked")
						ArrayList<Float> smoothAngles = (ArrayList<Float>) angles.clone();
						for (int i = 0; i < 1; i++) {
							smooth(smoothAngles);
						}
						g.setColor(Color.GRAY);
						g.setStroke(new BasicStroke(2.0f));
						g.drawLine(0, GRAPH_HEIGHT / 10, GRAPH_WIDTH, GRAPH_HEIGHT / 10);
						g.drawLine(0, GRAPH_HEIGHT * 9 / 10, GRAPH_WIDTH, GRAPH_HEIGHT * 9 / 10);
						g.drawLine(0, GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 / 10 / 2, GRAPH_WIDTH,
								GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 / 10 / 2);
						g.setStroke(new BasicStroke(1.0f));
						g.drawLine(0, GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 / 10 / 6, GRAPH_WIDTH,
								GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 / 10 / 6);
						g.drawLine(0, GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 * 2 / 10 / 6, GRAPH_WIDTH,
								GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 * 2 / 10 / 6);
						g.drawLine(0, GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 * 4 / 10 / 6, GRAPH_WIDTH,
								GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 * 4 / 10 / 6);
						g.drawLine(0, GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 * 5 / 10 / 6, GRAPH_WIDTH,
								GRAPH_HEIGHT / 10 + GRAPH_HEIGHT * 8 * 5 / 10 / 6);
						for (int i = 1; i < smoothAngles.size(); i++) {
							float f = smoothAngles.get(i);
							float pf = smoothAngles.get(i - 1);
							if (i % 100 == 0) {
								g.setStroke(new BasicStroke(1.0f));
								g.setColor(Color.GRAY);
								g.drawLine(GRAPH_WIDTH * i / (smoothAngles.size() - 1), 0,
										GRAPH_WIDTH * i / (smoothAngles.size() - 1), GRAPH_HEIGHT);
							}
							int rnd = (int) (Math.random() * 10);
							if (rnd < 4) {
								g.setColor(new Color(255, 0, 255, 255));
							} else if (rnd < 8) {
								g.setColor(new Color(255, 0, 0, 255));
							} else {
								g.setColor(new Color(255, 150, 0, 255));
							}
							g.setStroke(new BasicStroke(3.0f));
							g.drawLine(GRAPH_WIDTH * (i - 1) / (smoothAngles.size() - 1),
									(int) (pf * GRAPH_HEIGHT / 90 * -0.8 + GRAPH_HEIGHT * 0.9),
									GRAPH_WIDTH * i / (smoothAngles.size() - 1),
									(int) (f * GRAPH_HEIGHT / 90 * -0.8 + GRAPH_HEIGHT * 0.9));
						}
					}
					g.setStroke(new BasicStroke(2.0f));
					g2d.drawImage(imgGraph, imgChar.getWidth(), 0, null);
					synchronized (borders) {
						for (int i = 0; i < borders.size(); i++) {
							Point p = borders.get(i);
							int rnd = (int) (Math.random() * 10);
							int s = 1;
							if (rnd < 3) {
								g2d.setColor(new Color(255, 0, 255, 50));
								s = 3;
							} else if (rnd < 6) {
								g2d.setColor(new Color(255, 0, 0, 50));
								s = 3;
							} else if (rnd < 8) {
								g2d.setColor(new Color(255, 150, 0, 50));
								s = 3;
							} else if (rnd < 9) {
								g2d.setColor(new Color(255, 220, 220, 255));
								s = 2;
							} else {
								g2d.setColor(new Color(255, 255, 255, 255));
							}
							g2d.fillOval(p.x - s, baseY + p.y - s, 2 * s + 1, 2 * s + 1);
						}
					}
					f.repaint();
					Thread.sleep(20);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		protected void smooth(ArrayList<Float> angles) {
			float arr[] = new float[angles.size()];
			for (int i = 2; i < arr.length - 2; i++) {
				arr[i] = (angles.get(i - 2) + 2 * angles.get(i - 1) + 4 * angles.get(i) + 2 * angles.get(i + 1)
						+ angles.get(i + 2)) / 10.0f;
			}
			for (int i = 1; i < arr.length - 1; i++) {
				angles.set(i, arr[i]);
			}
		}

		private void findBorder(BufferedImage img) {
			boolean visited[][] = new boolean[imgChar.getHeight()][imgChar.getWidth()];
			findBorder(img, new Point(0, 0), visited);
			for (int i = borders.size(); i < borders.size() + WINDOW_SIZE; i++) {
				Point p2 = borders.get(i % borders.size());
				Point p1 = borders.get(i - WINDOW_SIZE);
				float angle = (float) (Math.atan2(p2.y - p1.y, p2.x - p1.x) / Math.PI * 180);
				angle += 360;
				angle %= 180;
				if (angle > 90) {
					angle = 180 - angle;
				}
				synchronized (angles) {
					angles.add(angle);
				}
			}
		}

		private void findBorder(BufferedImage img, Point p, boolean[][] visited) {
			visited[p.y][p.x] = true;
			if (img.getRGB(p.x, p.y) == Color.WHITE.getRGB()) {
				boolean borderFound = false;
				for (int i = 0; i < DIRS.length; i++) {
					int x = p.x + DIRS[i][1];
					int y = p.y + DIRS[i][0];
					if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()
							&& img.getRGB(x, y) == Color.BLACK.getRGB()) {
						borderFound = true;
						break;
					}
				}
				if (borderFound) {
					synchronized (borders) {
						borders.add(new Point(p.x, p.y));
						if (borders.size() > WINDOW_SIZE) {
							Point p2 = borders.get(borders.size() - 1);
							Point p1 = borders.get(borders.size() - 1 - WINDOW_SIZE);
							float angle = (float) (Math.atan2(p2.y - p1.y, p2.x - p1.x) / Math.PI * 180);
							angle += 360;
							angle %= 180;
							if (angle > 90) {
								angle = 180 - angle;
							}
							synchronized (angles) {
								angles.add(angle);
							}
						}
					}
					try {
						Thread.sleep(4);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for (int i = 0; i < DIRS.length; i++) {
						int x = p.x + DIRS[i][1];
						int y = p.y + DIRS[i][0];
						if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight() && !visited[y][x]
								&& img.getRGB(x, y) == Color.WHITE.getRGB()) {
							findBorder(img, new Point(x, y), visited);
						}
					}
				}
				return;
			}
			for (int i = 0; i < DIRS.length; i++) {
				int x = p.x + DIRS[i][1];
				int y = p.y + DIRS[i][0];
				if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight() && !visited[y][x]) {
					findBorder(img, new Point(x, y), visited);
				}
			}
		}
	}
}
