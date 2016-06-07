import java.util.Scanner;

public class BitStringClassify {

	public static void main(String[] args) {
		Scanner kb = new Scanner(System.in);
		System.out.print("Please Enter a long bit string (>=500 chars) : ");
		String input = kb.next();
		kb.close();
		int count[] = new int[16];// 0:0000 1:0001 2:0010 ... 14:1110 15:1111
		int total = 0;
		for (int i = 4; i < input.length(); i++) {
			count[Integer.parseInt(input.substring(i - 4, i), 2)]++;
			total++;
		}
		double maxEntropy = -count.length * (1.0 / count.length) * Math.log(1.0 / count.length) / Math.log(2);
		double entropy = 0.0;
		for (int i = 0; i < count.length; i++) {
			System.out.print(String.format("%04d : %d \t", Integer.parseInt(Integer.toString(i, 2)), count[i]));
			if (i % 4 == 3)
				System.out.println();
			entropy += -((double) count[i] / total) * Math.log(((double) count[i] / total)) / Math.log(2);
		}
		System.out.println("Input Length = " + input.length() + " chars");
		System.out.println("Maximum Possible Entropy = " + maxEntropy);
		System.out.println("Input Entropy = " + entropy);
		System.out.println("Result = " + (entropy / maxEntropy > 0.98 ? "COMPUTER" : "HUMAN"));
	}
}
