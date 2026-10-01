import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        int ch;

        do {
            System.out.println("\n===== DISASTER RESPONSE SYSTEM =====");
            System.out.println("1. ADMIN Module");
            System.out.println("2. VICTIM Module");
            System.out.println("3. FCFS Scheduling");
            System.out.println("4. Priority Scheduling");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            ch = sc.nextInt();
            sc.nextLine();

            switch (ch) {
                case 1:
                    Admin.menu();
                    break;
                case 2:
                    Victim.menu();
                    break;
                case 3:
                    FCFS.fcfs();
                    break;
                case 4:
                    Priority.priority();
                    break;
                case 5:
                    System.out.println("Thank You");
                    break;
                default:
                    System.out.println("Invalid Choice");
            }

        } while (ch != 5);
    }
}
