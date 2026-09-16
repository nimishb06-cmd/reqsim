#include <stdio.h>

void fcfs()
{
    int n, i, j;
    int at[20], bt[20], ct[20], tat[20], wt[20], id[20];
    int current = 0, temp;
    float avgwt = 0, avgtat = 0;

    printf("===== FCFS EMERGENCY QUEUE =====\n");
    printf("Enter number of requests: ");
    scanf("%d", &n);

    for(i=0;i<n;i++)
    {
        id[i] = i + 1;
        printf("\nRequest %d arrival time: ", i+1);
        scanf("%d",&at[i]);
        printf("Request %d burst time: ", i+1);
        scanf("%d",&bt[i]);
    }

    /* Sort by Arrival Time */
    for(i=0;i<n-1;i++)
    {
        for(j=i+1;j<n;j++)
        {
            if(at[i] > at[j])
            {
                temp=at[i]; at[i]=at[j]; at[j]=temp;
                temp=bt[i]; bt[i]=bt[j]; bt[j]=temp;
                temp=id[i]; id[i]=id[j]; id[j]=temp;
            }
        }
    }

    current = 0;
    for(i=0;i<n;i++)
    {
        if(current < at[i])
            current = at[i];

        current += bt[i];

        ct[i] = current;
        tat[i] = ct[i] - at[i];
        wt[i] = tat[i] - bt[i];

        avgwt += wt[i];
        avgtat += tat[i];
    }

    printf("\nREQ\tAT\tBT\tCT\tTAT\tWT\n");
    for(i=0;i<n;i++)
    {
        printf("R%d\t%d\t%d\t%d\t%d\t%d\n",
        id[i],at[i],bt[i],ct[i],tat[i],wt[i]);
    }

    printf("\nGANTT CHART\n\n");

    printf(" ");
    for(i=0;i<n;i++) printf("-------");
    printf("-\n|");

    for(i=0;i<n;i++)
        printf(" R%d |",id[i]);

    printf("\n ");
    for(i=0;i<n;i++) printf("-------");
    printf("-\n");

    current = 0;
    printf("%d",current);

    for(i=0;i<n;i++)
    {
        if(current < at[i])
            current = at[i];

        current += bt[i];
        printf("     %d",current);
    }

    printf("\n\nAverage Turnaround Time = %.2f", avgtat/n);
    printf("\nAverage Waiting Time = %.2f\n", avgwt/n);

}
