import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

public class FinalProject2 {

	public static Graph result = new Graph("Result");
	public static ArrayList<Graph> graphs = new ArrayList<Graph>();

	public static void main(String[] args) {
		JFrame f = new JFrame("Time Series: SHAPE AVERAGE");
		Container c = f.getContentPane();
		c.setLayout(new BorderLayout());
		FlowLayout fl = new FlowLayout(FlowLayout.LEFT);
		fl.setHgap(2);
		fl.setVgap(0);
		JPanel jp = new JPanel(fl);
		jp.add(result.l);
		c.add(jp, BorderLayout.NORTH);
		JScrollPane sp = new JScrollPane();
		sp.setPreferredSize(new Dimension(1200, Math.min(520, 150 * graphs.size())));
		JPanel pp = new JPanel(new GridLayout(0, 1, 0, 0));
		sp.setViewportView(pp);
		sp.revalidate();
		c.add(sp, BorderLayout.CENTER);
		JPanel jpb = new JPanel(new GridLayout(1, 0));
		JButton btnR = new JButton("(-)Remove");
		JButton btnA = new JButton("(+)Add");
		JPanel jpr = new JPanel(new GridLayout(1, 0));
		ButtonGroup bg = new ButtonGroup();
		JRadioButton rb1 = new JRadioButton("Merge", true);
		rb1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exponent = 0.5;
				calculate();
				repaintAll();
			}
		});
		JRadioButton rb2 = new JRadioButton("Value", false);
		rb2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exponent = -1;
				calculate();
				repaintAll();
			}
		});
		JRadioButton rb3 = new JRadioButton("Separate", false);
		rb3.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				exponent = 2;
				calculate();
				repaintAll();
			}
		});
		bg.add(rb2);
		jpr.add(rb2);
		jpr.add(new JLabel());
		jpr.add(new JLabel("Shape"));
		bg.add(rb1);
		jpr.add(rb1);
		bg.add(rb3);
		jpr.add(rb3);
		jpb.add(jpr);
		jpb.add(btnR);
		jpb.add(btnA);
		c.add(jpb, BorderLayout.SOUTH);
		btnA.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Graph g = new Graph("Graph-" + (graphs.size() + 1));
				graphs.add(g);
				pp.add(g.l);
				sp.setPreferredSize(new Dimension(1220, Math.min(520, 150 * graphs.size())));
				sp.revalidate();
				f.pack();
				FinalProject2.calculate();
			}
		});
		btnR.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (graphs.size() > 1) {
					pp.remove(graphs.get(graphs.size() - 1).l);
					graphs.remove(graphs.size() - 1);
					sp.setPreferredSize(new Dimension(1220, Math.min(520, 150 * graphs.size())));
					sp.revalidate();
					f.pack();
					FinalProject2.calculate();
				} else {
					float g[] = graphs.get(graphs.size() - 1).graph;
					for (int i = 0; i < g.length; i++) {
						g[i] = Graph.MAX / 2;
					}
					FinalProject2.calculate();
					graphs.get(graphs.size() - 1).plot();
				}
			}
		});
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setResizable(false);
		f.pack();
		f.setVisible(true);
		btnA.getActionListeners()[0].actionPerformed(null);
	}

	public static void calculate() {
		if (graphs.size() > 0) {
			if (exponent < 0)
				valueAverage();
			else
				shapeAverage();
		}
		result.plot();
		result.l.repaint();
	}

	public static void valueAverage() {
		for (int i = 0; i < result.graph.length; i++) {
			result.graph[i] = 0;
		}
		for (Graph g : graphs) {
			for (int i = 0; i < result.graph.length; i++) {
				result.graph[i] += g.graph[i];
			}
		}
		for (int i = 0; i < result.graph.length; i++) {
			result.graph[i] /= graphs.size();
		}
	}

	public static void shapeAverage() {
		float base[] = graphs.get(0).graph;
		for (int i = 1; i < graphs.size(); i++) {
			dtwSakoeChiba(base, graphs.get(i).graph);
			int path[] = dtwPath();
			int interPath[] = new int[path.length];
			for (int j = 0; j < interPath.length; j++) {
				interPath[j] = (j * i + path[j]) / (i + 1);
			}
			int path2[] = dtwPath2();
			int interPath2[] = new int[path2.length];
			for (int j = 0; j < interPath2.length; j++) {
				interPath2[j] = (j + path2[j] * i) / (i + 1);
			}
			float newBase[] = new float[base.length];
			int curr = 1;
			int curr2 = 1;
			for (int j = 0; j < newBase.length; j++) {
				while (curr < interPath.length - 1 && interPath[curr] < j)
					curr++;
				while (curr2 < interPath2.length - 1 && interPath2[curr2] < j)
					curr2++;
				if (curr < 2 || curr2 < 2 || path[curr - 1] - path[curr - 2] <= path2[curr2 - 1] - path2[curr2 - 2]) {
					newBase[j] = ((base[curr - 1] * i + graphs.get(i).graph[path[curr - 1]]) / (i + 1));
				} else {
					newBase[j] = ((base[path2[curr2 - 1]] * i + graphs.get(i).graph[curr2 - 1]) / (i + 1));
				}
			}
			base = newBase;
		}
		result.graph = base;
		for (int i = 0; i < graphs.size(); i++) {
			dtwSakoeChiba(base, graphs.get(i).graph);
			graphs.get(i).pathFromMain = dtwPath();
		}
	}

	public static float dtw[][];
	public static final int WINDOW_SIZE = 100;

	public static double exponent = 0.75;

	public static float dtwSakoeChiba(float[] a, float[] b) {
		dtw = new float[a.length][b.length];
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < b.length; j++) {
				dtw[i][j] = Float.POSITIVE_INFINITY;
			}
		}
		dtw[0][0] = 0f;
		for (int i = 0; i < a.length; i++) {
			for (int j = Math.max(0, i - WINDOW_SIZE); j < Math.min(b.length, i + WINDOW_SIZE); j++) {
				int I[] = { i - 1, i, i - 1 };
				int J[] = { j - 1, j - 1, j };
				for (int k = 0; k < I.length; k++) {
					if (I[k] >= 0 && J[k] >= 0) {
						dtw[i][j] = Math.min(dtw[i][j], (float) Math.pow(Math.abs(a[i] - b[j]), exponent)
								+ dtw[I[k]][J[k]] + (k == 0 ? 0 : 1) / 10f);
					}
				}
			}
		}
		return dtw[a.length - 1][b.length - 1];
	}

	public static int[] dtwPath() {
		int path[] = new int[dtw.length];
		int x = dtw[0].length - 1;
		int y = dtw.length - 1;
		while (x != 0 || y != 0) {
			path[y] = x;
			float min = Float.MAX_VALUE;
			if (x > 0 && y > 0)
				min = Math.min(min, dtw[y - 1][x - 1]);
			if (x > 0)
				min = Math.min(min, dtw[y][x - 1]);
			if (y > 0)
				min = Math.min(min, dtw[y - 1][x]);
			if (x > 0 && y > 0 && min == dtw[y - 1][x - 1]) {
				x--;
				y--;
			} else if (x > 0 && min == dtw[y][x - 1])
				x--;
			else if (y > 0 && min == dtw[y - 1][x])
				y--;
		}
		return path;
	}

	public static int[] dtwPath2() {
		int path[] = new int[dtw.length];
		int x = dtw[0].length - 1;
		int y = dtw.length - 1;
		while (x != 0 || y != 0) {
			path[x] = y;
			float min = Float.MAX_VALUE;
			if (x > 0 && y > 0)
				min = Math.min(min, dtw[y - 1][x - 1]);
			if (x > 0)
				min = Math.min(min, dtw[y][x - 1]);
			if (y > 0)
				min = Math.min(min, dtw[y - 1][x]);
			if (x > 0 && y > 0 && min == dtw[y - 1][x - 1]) {
				x--;
				y--;
			} else if (x > 0 && min == dtw[y][x - 1])
				x--;
			else if (y > 0 && min == dtw[y - 1][x])
				y--;
		}
		return path;
	}

	public static void repaintAll() {
		result.plot();
		result.l.repaint();
		for (Graph g : graphs) {
			g.plot();
			g.l.repaint();
		}
	}
}

class Graph {
	public BufferedImage img = new BufferedImage(1200, 150, BufferedImage.TYPE_INT_ARGB);
	public JLabel l = new JLabel(new ImageIcon(img));
	public boolean pressed = false;
	public String name = "";

	public static final float MAX = 10f;

	private Integer prevX = null;
	private Integer prevY = null;

	public float[] graph = new float[101];

	public static int posMain = -1;
	public int pathFromMain[];

	public Graph(String name) {
		this.name = name;
		l.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				pressed = false;
				prevX = null;
				prevY = null;
			}

			@Override
			public void mousePressed(MouseEvent e) {
				pressed = true;
				prevX = e.getX();
				prevY = e.getY();
				l.getMouseMotionListeners()[0].mouseMoved(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				pressed = false;
				prevX = null;
				prevY = null;
				if (name.equals("Result")) {
					posMain = -1;
					FinalProject2.repaintAll();
				}
			}
		});
		l.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				if (name.equals("Result")) {
					posMain = (e.getX() + img.getWidth() / (graph.length - 1) / 2) * (graph.length - 1)
							/ img.getWidth();
					FinalProject2.repaintAll();
				}
				if (pressed && e.getX() >= 0 && e.getX() < img.getWidth() && prevX != null && prevY != null) {
					int x = e.getX();
					while (x != prevX) {
						graph[(x + img.getWidth() / (graph.length - 1) / 2) * (graph.length - 1) / img.getWidth()] = MAX
								- ((e.getY() + (prevY - e.getY())
										- (prevY - e.getY()) * (prevX - x) / (prevX - e.getX())) * MAX
										/ img.getHeight());
						if (prevX < x) {
							x--;
						} else if (prevX > x) {
							x++;
						}
					}
					graph[(e.getX() + img.getWidth() / (graph.length - 1) / 2) * (graph.length - 1)
							/ img.getWidth()] = MAX - (e.getY() * MAX / img.getHeight());
					prevX = e.getX();
					prevY = e.getY();
				}
				FinalProject2.calculate();
				plot();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
		});
		for (int i = 0; i < graph.length; i++) {
			graph[i] = MAX / 2;
		}
		plot();
	}

	public void plot() {
		Graphics2D g = img.createGraphics();
		g.setStroke(new BasicStroke(2.0f));
		if (this.name.equals("Result"))
			g.setColor(new Color(200, 255, 200));
		else
			g.setColor(new Color(245, 245, 150));
		g.fillRect(0, 0, img.getWidth(), img.getHeight());
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
		g.setFont(new Font("Arial", Font.BOLD, 20));
		g.drawString(name, 10, 30);
		g.setColor(Color.WHITE);
		g.drawLine(0, img.getHeight() / 2, img.getWidth(), img.getHeight() / 2);
		g.setColor(Color.RED);
		for (int i = 1; i < graph.length; i++) {
			g.drawLine((int) (img.getWidth() / (graph.length - 1) * (i - 1)),
					(int) (img.getHeight() - (graph[i - 1] * img.getHeight() / MAX)),
					(int) (img.getWidth() / (graph.length - 1) * (i)),
					(int) (img.getHeight() - (graph[i] * img.getHeight() / MAX)));
		}
		if (posMain >= 0) {
			g.setColor(Color.BLUE);
			if (name.equals("Result")) {
				g.drawLine((int) (img.getWidth() / (graph.length - 1) * posMain), 0,
						(int) (img.getWidth() / (graph.length - 1) * posMain), img.getHeight());
			} else {
				g.drawLine((int) (img.getWidth() / (graph.length - 1) * pathFromMain[posMain]), 0,
						(int) (img.getWidth() / (graph.length - 1) * pathFromMain[posMain]), img.getHeight());
			}
		}
		l.repaint();
	}
}