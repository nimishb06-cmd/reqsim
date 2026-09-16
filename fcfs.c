#include <stdio.h>

void priority()
{
    int n;
    int i, j;
    int temp;

    int id[20];
    int priority[20];
    int service[20];

    printf("\n===== PRIORITY SCHEDULING =====\n");

    printf("Higher number = higher priority\n");

    printf("Enter number of requests: ");
    scanf("%d", &n);

    for (i = 0; i < n; i++)
    {
        id[i] = i + 1;

        printf("\nRequest %d service time: ", i + 1);
        scanf("%d", &service[i]);

        printf("Request %d priority: ", i + 1);
        scanf("%d", &priority[i]);
    }

    /* Sort according to priority */
    for (i = 0; i < n - 1; i++)
    {
        for (j = i + 1; j < n; j++)
        {
            if (priority[j] > priority[i])
            {
                temp = priority[i];
                priority[i] = priority[j];
                priority[j] = temp;

                temp = service[i];
                service[i] = service[j];
                service[j] = temp;

                temp = id[i];
                id[i] = id[j];
                id[j] = temp;
            }
        }
    }

    printf("\nRequest\tPriority\tService Time\n");

    for (i = 0; i < n; i++)
    {
        printf("R%d\t%d\t\t%d\n",
               id[i],
               priority[i],
               service[i]);
    }
}
