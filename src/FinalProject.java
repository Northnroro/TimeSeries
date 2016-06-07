import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class FinalProject {

	public static ThreadLocal<Float[]> weight = new ThreadLocal<Float[]>();

	public static float result[][][] = new float[10][10][10];

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
	protected static final ThreadLocal<Signal> cachedSignal = new ThreadLocal<Signal>();

	protected static ArrayList<DataSet> dataset = new ArrayList<DataSet>();

	public FinalProject(Float[] weight) {
		FinalProject.weight.set(weight);
		loadDataSet();
	}

	protected void loadDataSet() {
		int sum = 0;
		synchronized (dataset) {
			if (dataset.size() == 0) {
				for (File f : new File(".").listFiles()) {
					if (f.isFile() && f.getName().lastIndexOf("_TEST") > 0) {
						dataset.add(new DataSet(f.getName().substring(0, f.getName().lastIndexOf('_'))));
						System.out.println(dataset.get(dataset.size()-1).test.size() + " " + dataset.get(dataset.size()-1).train.size());
						sum += dataset.get(dataset.size()-1).test.size()*dataset.get(dataset.size()-1).train.size();
					}
				}
			}
		}
		System.out.println(">> " + sum);
	}

	protected float testAll() {
		long startTime = System.currentTimeMillis();
		float percent = 0.0f;
		for (DataSet d : dataset) {
			percent += d.calculatePercent();
		}
		System.out.println(new String(new char[Integer.parseInt(Thread.currentThread().getName().substring(7)) % 4])
				.replace("\0", "\t\t\t\t") + "Average Correct = " + String.format("%.2f", percent / dataset.size())
				+ "% " + (System.currentTimeMillis() - startTime) / 10 / 100f + "sec");
		return percent / dataset.size();
	}

	public static void main(String[] args) {
		int count = 111;
		Thread current = Thread.currentThread();
		while (true) {
			boolean threadCreate = false;
			//TODO THREAD NUMBER                     VVVVV
			for (int n = Thread.activeCount() - 1; n < 4; n++) {
				threadCreate = true;
				int cnt = count;
				synchronized (result) {
					if (count >= result[0][0].length * result[0].length * result.length)
						System.exit(1);
				}
				new Thread(new Runnable() {
					@Override
					public void run() {
						int i, j, k;
						synchronized (result) {
							i = cnt / result[0][0].length / result[0].length;
							j = (cnt / result[0][0].length) % result[0].length;
							k = cnt % result[0][0].length;
						}
						System.out.println("i:" + i + " j:" + j + " k:" + k + "  >  >  i , j , k");
						FinalProject fp = new FinalProject(new Float[] { (float) i, (float) j, (float) k });
						float percent;
						percent = fp.testAll();
						synchronized (result) {
							result[i][j][k] = percent;
//							for (int a = 0; a < result.length; a++) {
//								for (int b = 0; b < result[a].length; b++) {
//									for (int c = 0; c < result[a][b].length; c++) {
//										System.out.println((a) + ":" + (b) + ":" + (c) + " = " + result[a][b][c]);
//									}
//								}
//							}
						}
						synchronized (current) {
							current.notify();
						}
					}
				}, "Thread-" + count++).start();
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

class DataSet {
	protected String name;
	protected ArrayList<Signal> test = new ArrayList<Signal>();
	protected ArrayList<Signal> train = new ArrayList<Signal>();

	protected int COUNT = 10;

	public DataSet(String name) {
		this.name = name;
		try {
			Scanner s = new Scanner(new File(name + "_TEST"));
			while (s.hasNext() && COUNT-- > 0)
				test.add(new Signal(s.nextLine()));
			s.close();
			s = new Scanner(new File(name + "_TRAIN"));
			while (s.hasNext())
				train.add(new Signal(s.nextLine()));
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
		for (Signal s : test) {
			correct += s.isCorrect(s.findNearest(train)) ? 1 : 0;
		}
		System.out.println(
				new String(new char[Integer.parseInt(Thread.currentThread().getName().substring(7)) % 4]).replace("\0",
						"\t\t\t\t") + name + " [Correct = " + String.format("%.2f", (float) correct / test.size() * 100)
				+ "%] " + (System.currentTimeMillis() - startTime) / 10 / 100f + "sec");
		return (float) correct / test.size() * 100;
	}
}

class Signal {
	protected int type;
	protected ArrayList<Float> signal = new ArrayList<Float>();

	protected final int WINDOW_SIZE = 10;

	public Signal(String signal) {
		Scanner s = new Scanner(signal);
		type = (int) s.nextDouble();
		while (s.hasNextDouble())
			this.signal.add((float) s.nextDouble());
		s.close();
	}

	private void createEnvelope() {
		if (this == FinalProject.cachedSignal.get())
			return;
		FinalProject.cachedSignal.set(this);
		FinalProject.upperEnvelope.set(new ArrayList<Float>());
		FinalProject.lowerEnvelope.set(new ArrayList<Float>());
		for (int i = 0; i < this.signal.size(); i++) {
			float up = Float.NEGATIVE_INFINITY;
			float low = Float.POSITIVE_INFINITY;
			for (int j = Math.max(0, i - WINDOW_SIZE); j < Math.min(this.signal.size(), i + WINDOW_SIZE); j++) {
				up = Math.max(up, this.signal.get(j));
				low = Math.min(low, this.signal.get(j));
			}
			FinalProject.upperEnvelope.get().add(up);
			FinalProject.lowerEnvelope.get().add(low);
		}
	}

	public float keoghLB(Signal that, Float[] w) {
		createEnvelope();
		float lb = 0.0f;
		for (int i = 0; i < that.signal.size(); i++) {
			lb += Math.max(0, that.signal.get(i) - FinalProject.upperEnvelope.get().get(i));
			lb -= Math.min(0, that.signal.get(i) - FinalProject.lowerEnvelope.get().get(i));
		}
		return lb;
	}

	public float dtw(Signal that, Float[] w) {
		if (FinalProject.dtw.get().length < this.signal.size()
				|| FinalProject.dtw.get()[0].length < that.signal.size()) {
			FinalProject.dtw.set(new Float[Math.max(FinalProject.dtw.get().length, this.signal.size())][Math
					.max(FinalProject.dtw.get()[0].length, that.signal.size())]);
		}
		Float[][] dtw = FinalProject.dtw.get();
		for (int i = 0; i < this.signal.size(); i++) {
			for (int j = 0; j < that.signal.size(); j++) {
				dtw[i][j] = Float.POSITIVE_INFINITY;
			}
		}
		dtw[0][0] = 0f;
		for (int i = 0; i < this.signal.size(); i++) {
			for (int j = 0; j < that.signal.size(); j++) {
				int I[] = { i - 1, i, i - 1 };
				int J[] = { j - 1, j - 1, j };
				for (int k = 0; k < I.length; k++) {
					if (I[k] >= 0 && J[k] >= 0) {
						dtw[i][j] = Math.min(dtw[i][j],
								Math.abs(this.signal.get(i) - that.signal.get(j)) * w[k] + dtw[I[k]][J[k]]);
					}
				}
			}
		}
		return dtw[this.signal.size() - 1][that.signal.size() - 1];
	}

	public float dtwSakoeChiba(Signal that, Float[] w) {
		if (this.signal.size() != that.signal.size()) {
			System.err.println("NOT SAME LENGTH");
			return dtw(that, w);
		}
		if (FinalProject.dtw.get().length < this.signal.size()
				|| FinalProject.dtw.get()[0].length < that.signal.size()) {
			FinalProject.dtw.set(new Float[Math.max(FinalProject.dtw.get().length, this.signal.size())][Math
					.max(FinalProject.dtw.get()[0].length, that.signal.size())]);
		}
		Float[][] dtw = FinalProject.dtw.get();
		for (int i = 0; i < this.signal.size(); i++) {
			for (int j = 0; j < that.signal.size(); j++) {
				dtw[i][j] = Float.POSITIVE_INFINITY;
			}
		}
		dtw[0][0] = 0f;
		for (int i = 0; i < this.signal.size(); i++) {
			for (int j = Math.max(0, i - WINDOW_SIZE); j < Math.min(that.signal.size(), i + WINDOW_SIZE); j++) {
				int I[] = { i - 1, i, i - 1 };
				int J[] = { j - 1, j - 1, j };
				for (int k = 0; k < I.length; k++) {
					if (I[k] >= 0 && J[k] >= 0) {
						dtw[i][j] = Math.min(dtw[i][j],
								Math.abs(this.signal.get(i) - that.signal.get(j)) * w[k] + dtw[I[k]][J[k]]);
					}
				}
			}
		}
		return dtw[this.signal.size() - 1][that.signal.size() - 1];
	}

	public Signal findNearest(ArrayList<Signal> train) {
		Signal near = null;
		float min = Float.POSITIVE_INFINITY;
		Float[] W = FinalProject.weight.get();
		for (Signal s : train) {
			float lb = this.keoghLB(s, FinalProject.weight.get());
			if (lb < min /*|| W[0] < 1 || W[1] < 1 || W[2] < 1*/) {
				float dist = this.dtwSakoeChiba(s, FinalProject.weight.get());
				if (dist < min) {
					min = dist;
					near = s;
				}
			}
		}
		return near;
	}

	public boolean isCorrect(Signal near) {
		if (near == null) {
			System.err.println("NULL");
			return false;
		}
		return this.type == near.type;
	}
}