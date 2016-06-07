import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;

public class Quiz2 {

	/**
	 * 
	 * Napat Lipimongkol 5531019021
	 * 
	 */

	public static final double BETA5[] = { -0.84, -0.25, 0.25, 0.84 };

	public static double[] randomTimeSeries(int X, int n) {
		double[] out = new double[n];
		for (int i = 0; i < out.length; i++)
			out[i] = Math.random() * X;
		return out;
	}

	public static void saveTimeSeries(double[] in, String fileName) throws FileNotFoundException {
		PrintWriter p = new PrintWriter(new File(fileName));
		for (double d : in)
			p.printf("%.2f ", d);
		p.close();
	}

	public static double findMean(double[] in) {
		double sum = 0.0;
		for (double d : in)
			sum += d;
		return sum / in.length;
	}

	public static double findSD(double[] in, double mean) {
		double sum = 0.0;
		for (double d : in)
			sum += (d - mean) * (d - mean);
		return Math.sqrt(sum / in.length);
	}

	public static void zNormalize(double[] inout) {
		double mean = findMean(inout);
		double sd = findSD(inout, mean);
		for (int i = 0; i < inout.length; i++)
			inout[i] = (inout[i] - mean) / sd;
	}

	public static String timeSeriesToSAX(double[] in) {
		String out = "";
		for (double d : in) {
			int c = Arrays.binarySearch(Quiz2.BETA5, d);
			c = c < 0 ? -(c + 1) : c;
			out += (char) ('A' + c);
		}
		return out;
	}

	public static double minDist(String A, String B) {
		double table[][] = new double[BETA5.length + 1][BETA5.length + 1];
		for (int i = 0; i < table.length; i++) {
			for (int j = 0; j < table[i].length; j++) {
				table[i][j] = Math.abs((i > 0 ? BETA5[i - 1] : BETA5[0]) - (j > 0 ? BETA5[j - 1] : BETA5[0]));
				table[i][j] = Math.min(table[i][j], Math.abs(
						(i > 0 ? BETA5[i - 1] : BETA5[0]) - (j < BETA5.length ? BETA5[j] : BETA5[BETA5.length - 1])));
				table[i][j] = Math.min(table[i][j], Math.abs(
						(i < BETA5.length ? BETA5[i] : BETA5[BETA5.length - 1]) - (j > 0 ? BETA5[j - 1] : BETA5[0])));
				table[i][j] = Math.min(table[i][j], Math.abs((i < BETA5.length ? BETA5[i] : BETA5[BETA5.length - 1])
						- (j < BETA5.length ? BETA5[j] : BETA5[BETA5.length - 1])));
			}
		}
		double dist = 0.0;
		for (int i = 0; i < A.length(); i++)
			dist += table[A.charAt(i) - 'A'][B.charAt(i) - 'A'] * table[A.charAt(i) - 'A'][B.charAt(i) - 'A'];
		return Math.sqrt(dist);
	}

	public static void main(String[] args) throws FileNotFoundException {
		int X = 5, Y = 5, Z = 5;
		int n = 1 << X, w = 1 << Y;
		double[] A = Quiz2.randomTimeSeries(X, n);
		double[] B = Quiz2.randomTimeSeries(X, n);
		Quiz2.saveTimeSeries(A, "A.txt");
		Quiz2.saveTimeSeries(B, "B.txt");
		Quiz2.zNormalize(A);
		Quiz2.zNormalize(B);
		Quiz2.saveTimeSeries(A, "A_norm.txt");
		Quiz2.saveTimeSeries(B, "B_norm.txt");
		String SAX_A = Quiz2.timeSeriesToSAX(A);
		String SAX_B = Quiz2.timeSeriesToSAX(B);
		System.out.println("SAX_A = " + SAX_A);
		System.out.println("SAX_B = " + SAX_B);
		System.out.println("minDist = " + minDist(SAX_A, SAX_B));
	}
}
