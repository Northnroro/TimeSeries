import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class FinalProjectB {

	public static ThreadLocal<Float[]> weight = new ThreadLocal<Float[]>();

	protected static ThreadLocal<Float[][]> dtw = new ThreadLocal<Float[][]>() {
		@Override
		protected Float[][] initialValue() {
			Float[][] f = new Float[1][1];
			f[0][0] = 0f;
			return f;
		};
	};
	protected static final ThreadLocal<ArrayList<Float>> upperEnvelope = new ThreadLocal<ArrayList<Float>>();
	protected static final ThreadLocal<ArrayList<Float>> lowerEnvelope = new ThreadLocal<ArrayList<Float>>();
	protected static final ThreadLocal<SignalB> cachedSignal = new ThreadLocal<SignalB>();

	protected static ArrayList<DataSetB> dataset = new ArrayList<DataSetB>();

	public FinalProjectB(Float[] weight) {
		FinalProjectB.weight.set(weight);
		loadDataSet();
	}

	protected void loadDataSet() {
		synchronized (dataset) {
			if (dataset.size() == 0) {
				for (File f : new File(".").listFiles()) {
					if (f.isFile() && f.getName().lastIndexOf("_TEST") > 0) {
						dataset.add(new DataSetB(f.getName().substring(0, f.getName().lastIndexOf('_'))));
					}
				}
			}
		}
	}

	protected float testAll() {
		long startTime = System.currentTimeMillis();
		float percent = 0.0f;
		for (DataSetB d : dataset) {
			percent += d.calculatePercent();
		}
		System.out.println(new String(new char[Integer.parseInt(Thread.currentThread().getName().substring(7)) % 4])
				.replace("\0", "\t\t\t\t") + "Average Correct = " + String.format("%.2f", percent / dataset.size())
				+ "% " + (System.currentTimeMillis() - startTime) / 10 / 100f + "sec");
		return percent / dataset.size();
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		Float[][] weights = new Float[][] { new Float[] { 1f, 1f, 1f }, new Float[] { 1f, 1.2f, 1.3f } };
		int count = 0;
		Thread current = Thread.currentThread();
		while (true) {
			if (count >= weights.length)
				break;
			boolean threadCreate = false;
			for (int n = Thread.activeCount() - 1; n < 2; n++) {
				if (count >= weights.length)
					break;
				threadCreate = true;
				final int cnt = count++;
				new Thread(new Runnable() {
					@Override
					public void run() {
						FinalProjectB fp = new FinalProjectB(weights[cnt]);
						float percent = fp.testAll();
						System.out.println(">>>>>> " + percent + "% <<<<<<");
						synchronized (current) {
							current.notify();
						}
					}
				}, "Thread-" + count++).start();
				break;
			}
			if (threadCreate) {
				try {
					synchronized (current) {
						current.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class DataSetB {
	protected String name;
	protected ArrayList<SignalB> test = new ArrayList<SignalB>();
	protected ArrayList<SignalB> train = new ArrayList<SignalB>();

	protected int COUNT = 10;

	public DataSetB(String name) {
		this.name = name;
		try {
			Scanner s = new Scanner(new File(name + "_TEST"));
			while (s.hasNext() && COUNT-- > 0)
				test.add(new SignalB(s.nextLine()));
			s.close();
			s = new Scanner(new File(name + "_TRAIN"));
			while (s.hasNext())
				train.add(new SignalB(s.nextLine()));
			s.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println(new String(new char[Integer.parseInt(Thread.currentThread().getName().substring(7)) % 4])
				.replace("\0", "\t\t\t\t") + name + " Loaded.");
	}

	public float calculatePercent() {
		long startTime = System.currentTimeMillis();
		int correct = 0;
		for (SignalB s : test) {
			correct += s.isCorrect(s.findNearest(train)) ? 1 : 0;
		}
		System.out.println(
				new String(new char[Integer.parseInt(Thread.currentThread().getName().substring(7)) % 4]).replace("\0",
						"\t\t\t\t") + name + " [Correct = " + String.format("%.2f", (float) correct / test.size() * 100)
				+ "%] " + (System.currentTimeMillis() - startTime) / 10 / 100f + "sec");
		return (float) correct / test.size() * 100;
	}
}

class SignalB {
	protected int type;
	protected ArrayList<Float> signal = new ArrayList<Float>();

	protected final int WINDOW_SIZE = 10;

	public SignalB(String signal) {
		Scanner s = new Scanner(signal);
		type = (int) s.nextDouble();
		while (s.hasNextDouble())
			this.signal.add((float) s.nextDouble());
		s.close();
	}

	private void createEnvelope() {
		if (this == FinalProjectB.cachedSignal.get())
			return;
		FinalProjectB.cachedSignal.set(this);
		FinalProjectB.upperEnvelope.set(new ArrayList<Float>());
		FinalProjectB.lowerEnvelope.set(new ArrayList<Float>());
		for (int i = 0; i < this.signal.size(); i++) {
			float up = Float.NEGATIVE_INFINITY;
			float low = Float.POSITIVE_INFINITY;
			for (int j = Math.max(0, i - WINDOW_SIZE); j < Math.min(this.signal.size(), i + WINDOW_SIZE); j++) {
				up = Math.max(up, this.signal.get(j));
				low = Math.min(low, this.signal.get(j));
			}
			FinalProjectB.upperEnvelope.get().add(up);
			FinalProjectB.lowerEnvelope.get().add(low);
		}
	}

	public float keoghLB(SignalB that, Float[] w) {
		createEnvelope();
		float lb = 0.0f;
		for (int i = 0; i < that.signal.size(); i++) {
			lb += Math.max(0, that.signal.get(i) - FinalProjectB.upperEnvelope.get().get(i));
			lb -= Math.min(0, that.signal.get(i) - FinalProjectB.lowerEnvelope.get().get(i));
		}
		return lb;
	}

	// TODO DP-EQUATION
	public static final float DP_WEIGHT[][] = new float[][] {
		{ 1, 2, 1, 1 },
		{ 1, 2, 1 },
		{ 1, 2 },
		{ 1, 2, 1 },
		{ 1, 2, 1, 1 } };
	public static final int DP_RECURSIVE[][] = new int[][] { { 1, 3, 0, 2, 0, 1, 0, 0 }, { 1, 2, 0, 1, 0, 0 },
			{ 1, 1, 0, 0 }, { 2, 1, 1, 0, 0, 0 }, { 3, 1, 2, 0, 1, 0, 0, 0 } };

	public float dtw(SignalB that, Float[] w) {
		if (FinalProjectB.dtw.get().length < this.signal.size()
				|| FinalProjectB.dtw.get()[0].length < that.signal.size()) {
			FinalProjectB.dtw.set(new Float[Math.max(FinalProjectB.dtw.get().length, this.signal.size())][Math
					.max(FinalProjectB.dtw.get()[0].length, that.signal.size())]);
		}
		Float[][] dtw = FinalProjectB.dtw.get();
		for (int i = 0; i < this.signal.size(); i++) {
			for (int j = 0; j < that.signal.size(); j++) {
				dtw[i][j] = Float.POSITIVE_INFINITY;
			}
		}
		dtw[0][0] = 0f;
		for (int i = 0; i < this.signal.size(); i++) {
			for (int j = 0; j < that.signal.size(); j++) {
				Point P[] = new Point[DP_RECURSIVE.length];
				for (int k = 0; k < DP_RECURSIVE.length; k++) {
					P[k] = new Point(DP_RECURSIVE[k][0], DP_RECURSIVE[k][1]);
				}
				WeightPoint WP[][] = new WeightPoint[DP_WEIGHT.length][];
				for (int k = 0; k < DP_RECURSIVE.length; k++) {
					WP[k] = new WeightPoint[DP_WEIGHT[k].length];
					for (int l = 0; l < DP_WEIGHT[k].length; l++) {
						WP[k][l] = new WeightPoint(DP_WEIGHT[k][l], DP_RECURSIVE[k][2 * l], DP_RECURSIVE[k][2 * l + 1]);
					}
				}
				for (int k = 0; k < P.length; k++) {
					if (P[k].x >= 0 && P[k].y >= 0) {
						float sum = 0.0f;
						boolean valid = true;
						for (int l = 0; l < WP[k].length; l++) {
							if (WP[k][l].i < 0 || WP[k][l].j < 0) {
								valid = false;
								break;
							}
							sum += WP[k][l].w * Math.abs(this.signal.get(WP[k][l].i) - that.signal.get(WP[k][l].j));
						}
						if (valid) {
							dtw[i][j] = Math.min(dtw[i][j], dtw[P[k].x][P[k].y] + sum);
						}
					}
				}
			}
		}
		return dtw[this.signal.size() - 1][that.signal.size() - 1];
	}

	static class WeightPoint {
		public float w;
		public int i, j;

		public WeightPoint(float w, int i, int j) {
			this.w = w;
			this.i = i;
			this.j = j;
		}
	}

	public float dtwSakoeChiba(SignalB that, Float[] w) {
		if (this.signal.size() != that.signal.size()) {
			System.err.println("NOT SAME LENGTH");
			return dtw(that, w);
		}
		if (FinalProjectB.dtw.get().length < this.signal.size()
				|| FinalProjectB.dtw.get()[0].length < that.signal.size()) {
			FinalProjectB.dtw.set(new Float[Math.max(FinalProjectB.dtw.get().length, this.signal.size())][Math
					.max(FinalProjectB.dtw.get()[0].length, that.signal.size())]);
		}
		Float[][] dtw = FinalProjectB.dtw.get();
		for (int i = 0; i < this.signal.size(); i++) {
			for (int j = 0; j < that.signal.size(); j++) {
				dtw[i][j] = Float.POSITIVE_INFINITY;
			}
		}
		dtw[0][0] = 0f;
		for (int i = 0; i < this.signal.size(); i++) {
			for (int j = Math.max(0, i - WINDOW_SIZE); j < Math.min(that.signal.size(), i + WINDOW_SIZE); j++) {
				Point P[] = new Point[DP_RECURSIVE.length];
				for (int k = 0; k < DP_RECURSIVE.length; k++) {
					P[k] = new Point(i - DP_RECURSIVE[k][0], j - DP_RECURSIVE[k][1]);
				}
				WeightPoint WP[][] = new WeightPoint[DP_WEIGHT.length][];
				for (int k = 0; k < DP_RECURSIVE.length; k++) {
					WP[k] = new WeightPoint[DP_WEIGHT[k].length];
					for (int l = 0; l < DP_WEIGHT[k].length; l++) {
						WP[k][l] = new WeightPoint(DP_WEIGHT[k][l], i - DP_RECURSIVE[k][2 * l],
								j - DP_RECURSIVE[k][2 * l + 1]);
					}
				}
				for (int k = 0; k < P.length; k++) {
					if (P[k].x >= 0 && P[k].y >= 0) {
						float sum = 0.0f;
						boolean valid = true;
						for (int l = 0; l < WP[k].length; l++) {
							if (WP[k][l].i < 0 || WP[k][l].j < 0) {
								valid = false;
								break;
							}
							sum += WP[k][l].w * Math.abs(this.signal.get(WP[k][l].i) - that.signal.get(WP[k][l].j));
						}
						if (valid) {
							dtw[i][j] = Math.min(dtw[i][j], dtw[P[k].x][P[k].y] + sum);
						}
					}
				}
			}
		}
		return dtw[this.signal.size() - 1][that.signal.size() - 1];
	}

	public SignalB findNearest(ArrayList<SignalB> train) {
		SignalB near = null;
		float min = Float.POSITIVE_INFINITY;
		for (SignalB s : train) {
			// float lb = this.keoghLB(s, FinalProject.weight.get());
			// if (lb < min) {
			float dist = this.dtwSakoeChiba(s, FinalProjectB.weight.get());
			if (dist < min) {
				min = dist;
				near = s;
			}
			// }
		}
		return near;
	}

	public boolean isCorrect(SignalB near) {
		if (near == null) {
			System.err.println("NULL");
			return false;
		}
		return this.type == near.type;
	}
}