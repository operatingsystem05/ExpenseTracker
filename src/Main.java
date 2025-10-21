import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

public class Main
{
    private static final String filePath = "src" + File.separator + "config.properties";

    public static void main(String[] args)
    {
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream(filePath)) {
            props.load(fis);
        } catch (IOException e) {
            System.out.println("Could not read config file: " + e.getMessage());
            return;
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        try (Connection conx = DriverManager.getConnection(url, user, password);
             Scanner sc = new Scanner(System.in)) {

            createTable(conx);

            while (true) {
                System.out.println("\n--- Expense Tracker ---");
                System.out.println("1. Add expense");
                System.out.println("2. Show all expenses");
                System.out.println("3. Filter by category");
                System.out.println("4. Calculate total spending");
                System.out.println("5. Exit");
                System.out.print("Choose an option: ");

                String choice = sc.nextLine();

                switch (choice) {
                    case "1" -> addExpenses(conx, sc);
                    case "2" -> showAll(conx);
                    case "3" -> filterByCategory(conx, sc);
                    case "4" -> totalExpenses(conx);
                    case "5" -> {
                        System.out.println("Closing the app.");
                        return;
                    }
                    default -> System.out.println("Select a valid option (1–5).");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not run the app: " + e.getMessage());
        }
    }

    private static void createTable(Connection conx)
    {
        String sql = """
                CREATE TABLE IF NOT EXISTS expenses (
                    id SERIAL PRIMARY KEY,
                    category VARCHAR(100),
                    amount INT,
                    date DATE
                )
                """;

        try (Statement stmt = conx.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'expenses' is ready.");
        } catch (SQLException e) {
            System.out.println("Could not create table: " + e.getMessage());
        }
    }

    private static void addExpenses(Connection conx, Scanner sc)
    {
        System.out.print("Enter expense category: ");
        String category = sc.nextLine().trim();

        System.out.print("Enter expense amount: ");
        int amount;
        try {
            amount = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount. Must be an integer.");
            return;
        }

        System.out.print("Enter expense date (YYYY-MM-DD): ");
        String dateStr = sc.nextLine().trim();
        java.sql.Date sqlDate;
        try {
            sqlDate = Date.valueOf(dateStr); // Ensures correct format
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid date format. Use YYYY-MM-DD.");
            return;
        }

        String sql = "INSERT INTO expenses (category, amount, date) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setString(1, category);
            ps.setInt(2, amount);
            ps.setDate(3, sqlDate);
            int rows = ps.executeUpdate();
            System.out.println("✅ Expense added (" + rows + " row(s) affected)");
        } catch (SQLException e) {
            System.out.println("Error adding expense: " + e.getMessage());
        }
    }

    private static void showAll(Connection conx)
    {
        String sql = "SELECT * FROM expenses ORDER BY date DESC";

        try (Statement stmt = conx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.printf("%-5s %-15s %-10s %-12s%n", "ID", "Category", "Amount", "Date");

            while (rs.next()) {
                int id = rs.getInt("id");
                String category = rs.getString("category");
                int amount = rs.getInt("amount");
                Date date = rs.getDate("date");

                System.out.printf("%-5d %-15s %-10d %-12s%n", id, category, amount, date);
            }
        } catch (SQLException e) {
            System.out.println("Error loading expenses: " + e.getMessage());
        }
    }

    private static void filterByCategory(Connection conx, Scanner sc)
    {
        System.out.print("Enter category to filter: ");
        String category = sc.nextLine().trim();

        String sql = "SELECT * FROM expenses WHERE category ILIKE ? ORDER BY date DESC";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setString(1, category);
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            System.out.printf("%-5s %-15s %-10s %-12s%n", "ID", "Category", "Amount", "Date");

            while (rs.next()) {
                found = true;
                int id = rs.getInt("id");
                String cat = rs.getString("category");
                int amount = rs.getInt("amount");
                Date date = rs.getDate("date");

                System.out.printf("%-5d %-15s %-10d %-12s%n", id, cat, amount, date);
            }

            if (!found) {
                System.out.println("No expenses found for category: " + category);
            }

        } catch (SQLException e) {
            System.out.println("Error filtering expenses: " + e.getMessage());
        }
    }

    private static void totalExpenses(Connection conx)
    {
        String sql = "SELECT SUM(amount) FROM expenses";

        try (Statement stmt = conx.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt(1);
                System.out.println("💰 Total Spending: " + total);
            }
        } catch (SQLException e) {
            System.out.println("Could not calculate total spending: " + e.getMessage());
        }
    }

    static class Expense
    {
        private int id;
        private String category;
        private int amount;
        private Date date;

        public Expense(int id, String category, int amount, Date date) {
            this.id = id;
            this.category = category;
            this.amount = amount;
            this.date = date;
        }

        @Override
        public String toString() {
            return String.format("%-5d %-15s %-10d %-12s", id, category, amount, date);
        }
    }
}
