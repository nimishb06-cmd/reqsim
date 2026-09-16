#include <stdio.h>

void fcfs()
{
    int n;
    int i;
    int current = 0;

    int arrival[20];
    int burst[20];
    int completion[20];
    int turnaround[20];
    int waiting[20];

    float total_waiting = 0;

    printf("\n===== FCFS EMERGENCY QUEUE =====\n");

    printf("Enter number of requests: ");
    scanf("%d", &n);

    for (i = 0; i < n; i++)
    {
        printf("\nRequest %d arrival time: ", i + 1);
        scanf("%d", &arrival[i]);

        printf("Request %d burst time: ", i + 1);
        scanf("%d", &burst[i]);
    }

    for (i = 0; i < n; i++)
    {
        if (current < arrival[i])
            current = arrival[i];

        current = current + burst[i];

        completion[i] = current;

        turnaround[i] =
            completion[i] - arrival[i];

        waiting[i] =
            turnaround[i] - burst[i];

        total_waiting += waiting[i];
    }
    printf("\nGANTT CHART\n\n");

    printf(" ");
    for(i = 0; i < n; i++)
        printf("-------");
    printf("-\n");

    printf("|");
    for(i = 0; i < n; i++)
        printf("  R%d  |", i + 1);
    printf("\n ");

    for(i = 0; i < n; i++)
        printf("-------");
    printf("-\n");

    current = 0;
    printf("%d", current);

    for(i = 0; i < n; i++)
    {
        if(current < arrival[i])
            current = arrival[i];

        current += burst[i];
        printf("      %d", current);
    }

    printf("\nREQ\tAT\tBT\tCT\tTAT\tWT\n");

    for (i = 0; i < n; i++)
    {
        printf("%d\t%d\t%d\t%d\t%d\t%d\n",
               i + 1,
               arrival[i],
               burst[i],
               completion[i],
               turnaround[i],
               waiting[i]);
    }

    printf("\nAverage Waiting Time = %.2f\n",
           total_waiting / n);
}
