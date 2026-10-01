import java.util.Scanner;

public class FCFS {

    public static void fcfs() {

        Scanner sc = new Scanner(System.in);

        int[] at = new int[20];
        int[] bt = new int[20];
        int[] ct = new int[20];
        int[] tat = new int[20];
        int[] wt = new int[20];
        int[] id = new int[20];

        int n, i, j, temp;
        int current = 0;
        float avgWT = 0, avgTAT = 0;

        System.out.println("\n===== FCFS EMERGENCY QUEUE =====");
        System.out.print("Enter number of requests: ");
        n = sc.nextInt();

        // Input
        for (i = 0; i < n; i++) {
            id[i] = i + 1;

            System.out.print("\nRequest " + (i + 1) + " Arrival Time: ");
            at[i] = sc.nextInt();

            System.out.print("Request " + (i + 1) + " Burst Time: ");
            bt[i] = sc.nextInt();
        }

        // Sort by Arrival Time
        for (i = 0; i < n - 1; i++) {
            for (j = i + 1; j < n; j++) {
                if (at[i] > at[j]) {

                    temp = at[i];
                    at[i] = at[j];
                    at[j] = temp;

                    temp = bt[i];
                    bt[i] = bt[j];
                    bt[j] = temp;

                    temp = id[i];
                    id[i] = id[j];
                    id[j] = temp;
                }
            }
        }

        // FCFS Calculation
        current = 0;

        for (i = 0; i < n; i++) {

            if (current < at[i])
                current = at[i];

            current += bt[i];

            ct[i] = current;
            tat[i] = ct[i] - at[i];
            wt[i] = tat[i] - bt[i];

            avgWT += wt[i];
            avgTAT += tat[i];
        }

        // Output Table
        System.out.println("\nREQ\tAT\tBT\tCT\tTAT\tWT");

        for (i = 0; i < n; i++) {
            System.out.println("R" + id[i] + "\t" + at[i] + "\t" + bt[i]
                    + "\t" + ct[i] + "\t" + tat[i] + "\t" + wt[i]);
        }

        // Gantt Chart
        System.out.println("\nGANTT CHART\n");

        System.out.print(" ");
        for (i = 0; i < n; i++)
            System.out.print("-------");
        System.out.println("-");

        System.out.print("|");
        for (i = 0; i < n; i++)
            System.out.print(" R" + id[i] + " |");
        System.out.println();

        System.out.print(" ");
        for (i = 0; i < n; i++)
            System.out.print("-------");
        System.out.println("-");

        current = 0;
        System.out.print(current);

        for (i = 0; i < n; i++) {
            if (current < at[i])
                current = at[i];

            current += bt[i];
            System.out.print("      " + current);
        }

        System.out.printf("\n\nAverage Turnaround Time = %.2f",
                avgTAT / n);
        System.out.printf("\nAverage Waiting Time = %.2f\n",
                avgWT / n);
    }
}
