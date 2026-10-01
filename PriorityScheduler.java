import java.util.Scanner;

public class Priority {

    public static void priority() {

        Scanner sc = new Scanner(System.in);

        int n, i, j, temp;
        int[] id = new int[20];
        int[] pr = new int[20];
        int[] st = new int[20];
        int[] ct = new int[20];
        int[] tat = new int[20];
        int[] wt = new int[20];

        float avgWT = 0, avgTAT = 0;

        System.out.println("\n===== PRIORITY SCHEDULING =====");
        System.out.println("Higher number = higher priority");

        System.out.print("Enter number of requests: ");
        n = sc.nextInt();

        for (i = 0; i < n; i++) {
            id[i] = i + 1;

            System.out.print("\nRequest " + (i + 1) + " Burst Time: ");
            st[i] = sc.nextInt();

            System.out.print("Request " + (i + 1) + " Priority: ");
            pr[i] = sc.nextInt();
        }

        // Sort by Priority (Descending)
        for (i = 0; i < n - 1; i++) {
            for (j = i + 1; j < n; j++) {
                if (pr[j] > pr[i]) {

                    temp = pr[i];
                    pr[i] = pr[j];
                    pr[j] = temp;

                    temp = st[i];
                    st[i] = st[j];
                    st[j] = temp;

                    temp = id[i];
                    id[i] = id[j];
                    id[j] = temp;
                }
            }
        }

        // Calculate
        wt[0] = 0;
        ct[0] = st[0];
        tat[0] = ct[0];

        for (i = 1; i < n; i++) {
            wt[i] = ct[i - 1];
            ct[i] = wt[i] + st[i];
            tat[i] = ct[i];
        }

        // Output Table
        System.out.println("\nREQ\tPRI\tST\tCT\tTAT\tWT");

        for (i = 0; i < n; i++) {
            avgWT += wt[i];
            avgTAT += tat[i];

            System.out.println("R" + id[i] + "\t" + pr[i] + "\t"
                    + st[i] + "\t" + ct[i] + "\t"
                    + tat[i] + "\t" + wt[i]);
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

        System.out.print("0");
        for (i = 0; i < n; i++)
            System.out.printf("%7d", ct[i]);

        System.out.printf("\n\nAverage Waiting Time = %.2f",
                avgWT / n);
        System.out.printf("\nAverage Turnaround Time = %.2f\n",
                avgTAT / n);
    }
}
