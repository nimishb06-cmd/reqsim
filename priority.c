#include <stdio.h>

void priority()
{
    int n, i, j, temp;
    int id[20], pr[20], bt[20];
    int ct[20], tat[20], wt[20];
    float avg_wt = 0, avg_tat = 0;

    printf("\n===== PRIORITY SCHEDULING =====\n");
    printf("Higher number = higher priority\n");

    printf("Enter number of requests: ");
    scanf("%d", &n);

    for(i = 0; i < n; i++)
    {
        id[i] = i + 1;

        printf("\nRequest %d Burst Time: ", i + 1);
        scanf("%d", &bt[i]);

        printf("Request %d Priority: ", i + 1);
        scanf("%d", &pr[i]);
    }
    //Set priority
    for(i = 0; i < n - 1; i++)
    {
        for(j = i + 1; j < n; j++)
        {
            if(pr[j] > pr[i])
            {
                temp = pr[i];
                pr[i] = pr[j];
                pr[j] = temp;

                temp = bt[i];
                bt[i] = bt[j];
                bt[j] = temp;

                temp = id[i];
                id[i] = id[j];
                id[j] = temp;
            }
        }
    }

    //Calculate WT, CT and TAT 
    wt[0] = 0;
    ct[0] = bt[0];
    tat[0] = ct[0];

    for(i = 1; i < n; i++)
    {
        wt[i] = ct[i - 1];
        ct[i] = wt[i] + bt[i];
        tat[i] = ct[i];
    }

    printf("\nReq\tPri\tBT\tCT\tTAT\tWT\n");

    for(i = 0; i < n; i++)
    {
        avg_wt += wt[i];
        avg_tat += tat[i];

        printf("R%d\t%d\t%d\t%d\t%d\t%d\n",
               id[i], pr[i], bt[i], ct[i], tat[i], wt[i]);
    }

    //Gantt Chart 
    printf("\nGantt Chart:\n");

    printf(" ");
    for(i = 0; i < n; i++)
        printf("-------");
    printf("-\n|");

    for(i = 0; i < n; i++)
        printf(" R%d |", id[i]);

    printf("\n ");
    for(i = 0; i < n; i++)
        printf("-------");
    printf("-\n");

    printf("0");
    for(i = 0; i < n; i++)
        printf("%7d", ct[i]);

    avg_wt /= n;
    avg_tat /= n;

    printf("\n\nAverage Waiting Time = %.2f", avg_wt);
    printf("\nAverage Turn Around Time = %.2f\n", avg_tat);
}
