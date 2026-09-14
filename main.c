#include <stdio.h>

void fcfs();
void priority();

int main()
{
    int choice;

    while (1)
    {
        printf("\n==============================\n");
        printf("       RESQSIM OS MODULE\n");
        printf("==============================\n");

        printf("1. FCFS Emergency Queue\n");
        printf("2. Priority Scheduling\n");
        printf("0. Exit\n");

        printf("Enter choice: ");
        scanf("%d", &choice);

        switch (choice)
        {
            case 1:
                fcfs();
                break;

            case 2:
                priority();
                break;

            case 0:
                return 0;

            default:
                printf("Invalid choice!\n");
        }
    }
}