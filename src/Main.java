import java.util.Map;
import java.util.Scanner;
import java.sql.*;

public class Main {

    private static final String createTableIfMissing = """
            IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='People')
            CREATE TABLE People (
              id INT IDENTITY(1,1) PRIMARY KEY,
              first_name VARCHAR(50) NOT NULL,
              last_name  VARCHAR(50) NOT NULL,
              CONSTRAINT UQ_people_name UNIQUE (first_name, last_name)
            );
            """;

    private static final String isPersonInTable = "SELECT COUNT(*) FROM People " +
                                                  "WHERE first_name=? AND last_name=?";

    private static final String insertPerson = "INSERT INTO People(first_name,last_name) VALUES(?,?)";

    private static final String totalPeople = "SELECT COUNT(*) FROM People";

    private static final String displayInitialCount = """
          SELECT UPPER(LEFT(first_name,1)) AS initial, COUNT(*) AS count
            FROM People
           GROUP BY UPPER(LEFT(first_name,1))
           ORDER BY initial
          """;

    public static void main(String[] args){

        System.out.println("Drivers on classpath:"); //testing if setup worked
        DriverManager.getDrivers().asIterator().forEachRemaining(d -> System.out.println("  " + d.getClass().getName()));

        // Retrieve sensitive data from environment variables
        Map<String, String> env = System.getenv();
        String endpoint = env.get("db_connection");
        String username = env.get("db_username");
        String password = env.get("db_password");

        String connectionUrl =
                "jdbc:sqlserver://" + endpoint + ";"
                        + "database=BENNY_BRECHER;"
                        + "user=" + username + ";"
                        + "password=" + password + ";"
                        + "encrypt=true;"
                        + "trustServerCertificate=true;"
                        + "loginTimeout=30;";



        Scanner scanner = new Scanner(System.in);
        System.out.println("What's Your First Name?");
        String fName = scanner.nextLine().trim();
        System.out.println("What's Your Last Name?");
        String lName = scanner.nextLine().trim();
        System.out.println("How Many Random Names Do You Want To Add?");
        int choice = scanner.nextInt();
        scanner.close();


        try (Connection connection = DriverManager.getConnection(connectionUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableIfMissing);

            //First we see if the given name is already in table
            try(PreparedStatement dupeCheckStatement = connection.prepareStatement(isPersonInTable)){
                dupeCheckStatement.setString(1,fName);
                dupeCheckStatement.setString(2,lName);
                try(ResultSet resultSet = dupeCheckStatement.executeQuery()){
                    resultSet.next();
                   if(resultSet.getInt(1) != 0){
                       System.out.println("That name already exists.");
                       return;
                   }
                }
            }

            //Then if we didn't just return must be that its safe to add to table
            try(PreparedStatement insertStatement = connection.prepareStatement(insertPerson)){
                insertStatement.setString(1,fName);
                insertStatement.setString(2,lName);
                insertStatement.addBatch();
                for(int i = 0; i < choice; i++){
                    insertStatement.setString(1,randomFirstName());
                    insertStatement.setString(2,randomLastName());
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
                System.out.println("Inserted successfully.");
            }

            //Then display the number of records in the database
            try(ResultSet peopleCount = connection.createStatement().executeQuery(totalPeople)){
                peopleCount.next();
                System.out.println("Number of People In Table: "+peopleCount.getInt(1));
            }

            //And finally arrange the initials
            try(ResultSet nameCount = connection.createStatement().executeQuery(displayInitialCount)){
                while(nameCount.next()){
                    String initial = nameCount.getString("initial");
                    int count = nameCount.getInt("count");
                    System.out.println(initial + ":" + count);
                }
            }
        }
        catch (SQLException e) {
            System.out.println("Database Error: "+e.getMessage());
        }

    }



    private static String randomFirstName() {
        String[] firstNames = {
                "John", "Michael", "David", "Sarah", "Emily",
                "Jessica", "Robert", "James", "Mary", "Jennifer"
        };
        return firstNames[new java.util.Random().nextInt(firstNames.length)];
    }

    private static String randomLastName() {
        String[] lastNames = {
                "Smith", "Johnson", "Williams", "Brown", "Jones",
                "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
                "Hernandez", "Lopez"
        };
        return lastNames[new java.util.Random().nextInt(lastNames.length)];
    }

}