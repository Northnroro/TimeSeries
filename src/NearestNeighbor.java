import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class NearestNeighbor {
	public static void main(String[] args) {
		ArrayList<Point> pointsL = new ArrayList<Point>();
		ArrayList<Point> pointsR = new ArrayList<Point>();
		BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
		JFrame f = new JFrame("1-Nearest Neighbor");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setAlwaysOnTop(true);
		JLabel l = new JLabel(new ImageIcon(img));
		f.getContentPane().add(l);
		f.setResizable(false);
		f.pack();
		l.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					synchronized (pointsL) {
						pointsL.add(new Point(e.getX(), e.getY()));
					}
				} else {
					synchronized (pointsR) {
						pointsR.add(new Point(e.getX(), e.getY()));
					}
				}
			}
		});
		f.setVisible(true);
		Graphics2D g = img.createGraphics();
		while (true) {
			try {
				for (int i = 0; i < img.getHeight(); i++) {
					for (int j = 0; j < img.getWidth(); j++) {
						double min = Double.MAX_VALUE;
						img.setRGB(j, i, Color.CYAN.getRGB());
						synchronized (pointsL) {
							for (Point p : pointsL) {
								double dist = Math.hypot(j - p.x, i - p.y);
								min = Math.min(dist, min);
							}
						}
						synchronized (pointsR) {
							for (Point p : pointsR) {
								double dist = Math.hypot(j - p.x, i - p.y);
								if (dist < min) {
									img.setRGB(j, i, Color.PINK.getRGB());
									break;
								}
							}
						}
					}
				}
				synchronized (pointsL) {
					for (Point p : pointsL) {
						g.setColor(Color.BLUE);
						g.fillOval(p.x - 2, p.y - 2, 5, 5);
						g.setColor(Color.BLACK);
						g.drawOval(p.x - 2, p.y - 2, 5, 5);
					}
				}
				synchronized (pointsR) {
					for (Point p : pointsR) {
						g.setColor(Color.RED);
						g.fillOval(p.x - 2, p.y - 2, 5, 5);
						g.setColor(Color.BLACK);
						g.drawOval(p.x - 2, p.y - 2, 5, 5);
					}
				}
				f.repaint();
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
