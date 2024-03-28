import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Menu {
    private static final int ADD_WEBSITE = 1;
    private static final int PRINT_ALL = 2;
    private static final int DELETE_WEBSITE = 3;
    private static final int CHANGE_USER = 4;
    private static final int CHANGE_PASS = 5;
    private static final int DELETE_USER = 6;
    private static final int QUIT = 7;
    private Connection connection;
    private Scanner input;
    private User user;

    public Menu(Connection connection, Scanner input, User user) {
        this.connection = connection;
        this.input = input;
        this.user = user;
    }

    public void menu() throws SQLException {
        int response = showMenu();

        while (response != QUIT) {
            switch (response) {
                case ADD_WEBSITE:
                    addWebsite();
                    break;
                case PRINT_ALL:
                    printAllWebsites();
                    break;
                case DELETE_WEBSITE:
                    deleteWebsite();
                    break;
                case CHANGE_USER:
                    changeWebsiteName();
                    break;
                case CHANGE_PASS:
                    changePassword();
                    break;
                case DELETE_USER:
                    deleteUser();
                    break;
                default:
                    System.out.println("Please try again.");
                    break;
            }
            response = showMenu();
        }

    }

    private int showMenu() {
        System.out.println("\nWelcome to PasswordManager!");
        System.out.println("Choose from the following options:");
        System.out.println("1. Add a website's username and password to the database.");
        System.out.println("2. Print all websites and usernames in the database.");
        System.out.println("3. Delete a website's username and password from the database.");
        System.out.println("4. Change a username for a website.");
        System.out.println("5. Change a password for a website");
        System.out.println("6. Delete your user. WARNING: This will delete all of your saved usernames and passwords.");
        
        System.out.println("7. Quit.");

        return input.nextInt();
    }

    // ***************ADDING THE WEBSITES***************
    private void addWebsite() throws SQLException{
        System.out.println();
        System.out.print("What is the name of the website?\nwebsite: ");
        String website = input.next();
        String password;
        int websiteDataId = 0;

        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM website_data\n" +
                "LEFT JOIN passwords p on website_data.website_id = p.website_id\n" +
                "LEFT JOIN users u on p.user_id = u.id\n" +
                "WHERE u.id like (?) AND website like (?)")) {
            stmt.setInt(1, this.user.getId());
            stmt.setString(2, website);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if (rs.getString("website").equalsIgnoreCase(website)) {
                    System.out.println("This website and username combo already exists. Would you like to update the password? Y/N");
                    if (input.next().equalsIgnoreCase("y")) {
                        System.out.print("What is the new password?\npassword: ");
                        password = input.next();
                        websiteDataId = rs.getInt("website_id");

                        try (PreparedStatement st = connection.prepareStatement("UPDATE website_data SET password = ? where website_id like (?)")){
                            st.setString(1, password);
                            st.setInt(2, websiteDataId);
                            st.executeUpdate();
                        }
                        catch(Exception e){
                            System.out.println(e.getMessage());
                        }
                        System.out.println("The password for website: " + website + " has been updated with " + password + ".");
                    }
                    return;
                }
            }
        }

        System.out.print("What is the password?\npassword: ");
        password = input.next();

        try (PreparedStatement stmt = connection.prepareStatement("SELECT MAX(website_id) as id FROM website_data")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) websiteDataId = rs.getInt("id") + 1;
        }
        if (websiteDataId == 0) throw new RuntimeException("Error while inserting new password.");

        try (PreparedStatement st = connection.prepareStatement("INSERT INTO " +
                        "website_data(website, password, website_id) VALUES (?, ?, ?)")) {
                    st.setString(1, website);
                    st.setString(2, password);
                    st.setInt(3, websiteDataId);
                    st.executeUpdate();
        }

        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO passwords(user_id, website_id) VALUES(?, ?)")) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, websiteDataId);
            stmt.executeUpdate();
        }

        System.out.println("The website " + website + " and password " + password + " has been added.");
    }


    // ***************PRINTING ALL WEBSITES***************
    public void printAllWebsites() throws SQLException {
        System.out.println("\nWebsites:");
        System.out.println("---------");
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM website_data\n" +
                "LEFT JOIN passwords p on website_data.website_id = p.website_id\n" +
                "LEFT JOIN users u on p.user_id = u.id\n" +
                "WHERE u.id like (?)")) {
            stmt.setInt(1, this.user.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                System.out.println("Website: " + rs.getString("website") + "\n");
            }
        }
    }

    // ***************DELETE WEBSITE*************** 
    private void deleteWebsite() throws SQLException {
        printAllWebsites();

        System.out.print("Which website?\nwebsite: ");
        String website = input.next();


        int websiteDataId = getWebsiteDataId(website);
        if (websiteDataId == 0) throw new RuntimeException("Error while deleting website.");

        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM passwords " +
                "WHERE user_id like (?) " +
                "AND website_id like (?)")) {
            stmt.setInt(1, user.getId());
            stmt.setInt(2, websiteDataId);
            stmt.executeUpdate();
        }

        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM website_data " +
                "WHERE website_id like (?)")) {
            stmt.setInt(1, websiteDataId);
            stmt.executeUpdate();
        }

        System.out.println("The website " + website + " was deleted.");
        System.out.println();
    }

    // ***************DELETE WEBSITE*************** 
    private int getWebsiteDataId(String website) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM website_data\n" +
                "LEFT JOIN passwords p on website_data.website_id = p.website_id\n" +
                "LEFT JOIN users u on p.user_id = u.id\n" +
                "WHERE website like (?)\n" +
                "AND u.id like (?)")) {
            stmt.setString(1, website);
            stmt.setInt(2, user.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("website_id");
        }
        return 0;
    }

    // ***************CHANGES THE WEBSITE NAME***************
    private void changeWebsiteName() throws SQLException{
        System.out.println("\nWhat is the old website?\nwebsite: ");
        String website = input.next();

        int websiteDataId = getWebsiteDataId(website);
        if (websiteDataId == 0) throw new RuntimeException("Error while updating website.");

        System.out.println("What is the new website?\nwebsite: ");
        String newWebsite = input.next();

        try (PreparedStatement st = connection.prepareStatement("UPDATE website_data SET website = ? where website_id like (?)")) {
            st.setString(1, newWebsite);
            st.setInt(2, websiteDataId);
            st.executeUpdate();
        }

        catch(Exception e){
            System.out.println("Error while updating email: " + e.getMessage());
        }

        System.out.println("email updated.");
        System.out.println();

        System.out.println("Would you like to update the password? Y/N");
        if (input.next().equalsIgnoreCase("y")) {
            System.out.print("What is the new password?\npassword: ");
            String password = input.next();

            try (PreparedStatement st = connection.prepareStatement("UPDATE website_data SET password = ? where website_id like (?)")){
                st.setString(1, password);
                st.setInt(2, websiteDataId);
                st.executeUpdate();
            }
            catch(Exception e){
                System.out.println("Error while updating password: " + e.getMessage());
            }
            System.out.println("The password for website: " + newWebsite + " has been updated with " + password + ".");
        }
    }

    private void changePassword() throws SQLException{
        System.out.println("\nWhat is the old website?\nwebsite: ");
        String website = input.next();

        int websiteDataId = getWebsiteDataId(website);
        if (websiteDataId == 0) throw new RuntimeException("Error while updating website.");

        System.out.println("What is the new password?\npassword: ");
        String password = input.next();

        try (PreparedStatement st = connection.prepareStatement("UPDATE website_data SET password = ? where website_id like (?)")){
            st.setString(1, password);
            st.setInt(2, websiteDataId);
            st.executeUpdate();
        }
        catch(Exception e){
            System.out.println("Error while updating password: " + e.getMessage());
        }

        System.out.println("The password for website: " + website + " has been updated with " + password + ".");
    }

    private void deleteUser() throws SQLException {
        System.out.println("Are you sure you want to delete your user? Y/N");
        if (input.next().equalsIgnoreCase("n")) return;

        List<Integer> websiteIds = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM passwords " +
                "WHERE user_id like (?)")) {
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                websiteIds.add(rs.getInt("website_id"));
            }
        }

        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM passwords " +
                "WHERE user_id like (?)")) {
            stmt.setInt(1, user.getId());
            stmt.executeUpdate();
        }

        for (Integer website : websiteIds) {
            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM website_data " +
                    "WHERE website_id like (?)")) {
                stmt.setInt(1, website);
                stmt.executeUpdate();
            }
        }

        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM users " +
                "WHERE id like (?)")) {
            stmt.setInt(1, user.getId());
            stmt.executeUpdate();
        }

        System.out.println("User " + user.getUsername() + " deleted.");
        System.exit(0);
    }
}
