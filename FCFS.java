import java.util.Scanner;

public class FCFS {

    public static void run() {

        Scanner sc = new Scanner(System.in);

        System.out.print(
                "Enter number of processes: ");

        int n = sc.nextInt();

        int[] burst = new int[n];
        int[] waiting = new int[n];
        int[] turnaround = new int[n];

        for (int i = 0; i < n; i++) {

            System.out.print(
                    "Burst time P" + (i + 1) + ": ");

            burst[i] = sc.nextInt();
        }

        waiting[0] = 0;

        for (int i = 1; i < n; i++) {

            waiting[i] =
                    waiting[i - 1] + burst[i - 1];
        }

        for (int i = 0; i < n; i++) {

            turnaround[i] =
                    waiting[i] + burst[i];
        }

        double avgWaiting = 0;
        double avgTurnaround = 0;

        System.out.println(
                "\nProcess\tBurst\tWaiting\tTurnaround");

        for (int i = 0; i < n; i++) {

            System.out.println(
                    "P" + (i + 1)
                    + "\t"
                    + burst[i]
                    + "\t"
                    + waiting[i]
                    + "\t"
                    + turnaround[i]);

            avgWaiting += waiting[i];
            avgTurnaround += turnaround[i];
        }

        System.out.println(
                "\nAverage Waiting Time = "
                + avgWaiting / n);

        System.out.println(
                "Average Turnaround Time = "
                + avgTurnaround / n);
    }
}