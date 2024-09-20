package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    // TODO: Part 1
    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if(tokens.length != 3) {
            System.out.println("Create patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        // check 2: check if the username has been taken already
        if(usernameExistsPatient(username)) {
            System.out.println("Username taken, try again");
            return;
        }

        // check 3: check if password is strong
        if(weakPassword(password)) {
            System.out.println("Please choose a stronger password");
            return;
        }

        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed");
            e.printStackTrace();
        }
    }

    private static boolean weakPassword(String password) {
        if (password.length() < 8) {
            System.out.println("Please ensure password is at least 8 characters long");
            return true;
        }
        if (!(password.contains("!") | password.contains("@") | password.contains("#") | password.contains("?"))) {
            System.out.println("Please use one of the following special characters: !, @, #, ?");
            return true;
        }

        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }


        if (!hasLowerCase) {
            System.out.println("Please include at least 1 lower case letter");
            return true;
        }
        if (!hasUpperCase) {
            System.out.println("Please include at least 1 upper case letter");
            return true;
        }
        if (!hasDigit) {
            System.out.println("Please include at least 1 number");
            return true;
        }
        return false;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Create patient failed");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    // TODO: Part 1
    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentPatient != null || currentCaregiver != null) {
            System.out.println("User already logged in, try again");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            System.out.println("Logged in as " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    // TODO: Part 2
    private static void searchCaregiverSchedule(String[] tokens) {
        // search_caregiver_schedule <date>
        // check 1: make sure the user is logged in
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }
        String dateStr = tokens[1];
        Date time;

        try {
            time = Date.valueOf(dateStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            // Query to get available caregivers
            String queryCaregivers = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
            PreparedStatement statement = con.prepareStatement(queryCaregivers);
            statement.setDate(1, time);
            ResultSet resultSet = statement.executeQuery();

            // Check if there are available caregivers
            if (!resultSet.next()) {
                System.out.println("No caregiver is available");
                return;
            }

            // Print available caregivers
            do {
                System.out.println(resultSet.getString("Username"));
            } while (resultSet.next());

            // Query to get available vaccines and doses
            String queryVaccines = "SELECT Name, Doses FROM Vaccines ORDER BY Name";
            statement = con.prepareStatement(queryVaccines);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String vaccineName = resultSet.getString("Name");
                int doses = resultSet.getInt("Doses");
                System.out.println(vaccineName + " " + doses);
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }


    // TODO: Part 2
    private static void reserve(String[] tokens) {
        // reserve <date> <vaccine>
        // date: yyyy-mm-dd

        // check 1: make sure the user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        // check 2: check if the logged-in user is a patient (since patients only use this function)
        if (currentPatient == null) {
            System.out.println("Please login as a patient");
            return;
        }

        // check 3: the length for tokens need to be exactly 3 to include all information (with operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }

        String date = tokens[1];
        String vaccineName = tokens[2];

        // check 4: check if date is in valid format
        // yyyy-mm-dd
        // 0123456789
        if (date.indexOf("-") != 4) {
            System.out.println("Please try again");
            return;
        }

        // check 5: check if order is [date, vaccine]
        if(!tokens[1].contains("-") | tokens[2].contains("-")) {
            System.out.println("Please try again");
            return;
        }

        // Connect to the database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            // Check 6: if there are available caregivers for the given date
            String checkAvailability = "SELECT Username FROM Availabilities WHERE Time = ? AND Username NOT IN (SELECT caregiverUsername FROM Appointments WHERE Time = ?) ORDER BY Username ASC";
            PreparedStatement checkAvailabilityStmt = con.prepareStatement(checkAvailability);
            checkAvailabilityStmt.setDate(1, Date.valueOf(date));
            checkAvailabilityStmt.setDate(2, Date.valueOf(date));
            ResultSet caregiverResult = checkAvailabilityStmt.executeQuery();

            if (!caregiverResult.next()) {
                System.out.println("No caregiver is available");
                return;
            }

            // Get the available caregiver
            String caregiverUsername = caregiverResult.getString("Username");

            // Check 7: if there are enough vaccine doses available
            String checkVaccine = "SELECT Doses FROM Vaccines WHERE Name = ?";
            PreparedStatement checkVaccineStmt = con.prepareStatement(checkVaccine);
            checkVaccineStmt.setString(1, vaccineName);
            ResultSet vaccineResult = checkVaccineStmt.executeQuery();

            if (!vaccineResult.next()) {
                System.out.println("Please try again");
                return;
            }

            int availableDoses = vaccineResult.getInt("Doses");
            if (availableDoses <= 0) {
                System.out.println("Not enough available doses");
                return;
            }

            // Get the current maximum appointmentID
            String getMaxAppointmentId = "SELECT MAX(appointmentID) AS maxId FROM Appointments";
            PreparedStatement getMaxIdStmt = con.prepareStatement(getMaxAppointmentId);
            ResultSet maxIdResult = getMaxIdStmt.executeQuery();

            int newAppointmentId = 1; // default to 1 if there are no existing appointments
            if (maxIdResult.next()) {
                newAppointmentId = maxIdResult.getInt("maxId") + 1;
            }

            // Reserve an appointment with the new appointmentID
            String reserveAppointment = "INSERT INTO Appointments (appointmentID, patientUsername, caregiverUsername, vaccineName, Time) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement reserveStmt = con.prepareStatement(reserveAppointment);
            reserveStmt.setInt(1, newAppointmentId);
            reserveStmt.setString(2, currentPatient.getUsername());
            reserveStmt.setString(3, caregiverUsername);
            reserveStmt.setString(4, vaccineName);
            reserveStmt.setDate(5, Date.valueOf(date));
            reserveStmt.executeUpdate();

            // Mark the caregiver as unavailable for the given date
            String removeAvailability = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
            PreparedStatement removeAvailabilityStmt = con.prepareStatement(removeAvailability);
            removeAvailabilityStmt.setString(1, caregiverUsername);
            removeAvailabilityStmt.setDate(2, Date.valueOf(date));
            removeAvailabilityStmt.executeUpdate();

            // Update the number of available doses
            String updateVaccine = "UPDATE Vaccines SET Doses = Doses - 1 WHERE Name = ?";
            PreparedStatement updateVaccineStmt = con.prepareStatement(updateVaccine);
            updateVaccineStmt.setString(1, vaccineName);
            updateVaccineStmt.executeUpdate();

            System.out.println("Appointment ID " + newAppointmentId + ", Caregiver username " + caregiverUsername);

        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    // TODO: Part 2
    private static void showAppointments(String[] tokens) {
        // check 1: make sure the user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        // check 2: the length for tokens need to be exactly 1 to include all information (with the operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            if (currentCaregiver != null) {
                // check 2: if Caregiver is logged in
                // format is "vaccine name, date, patient id"
                String query = "SELECT appointmentID, vaccineName, Time, patientUsername FROM Appointments WHERE caregiverUsername = ? ORDER BY AppointmentID";
                PreparedStatement statement = con.prepareStatement(query);
                statement.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    int appointmentId = resultSet.getInt("appointmentID");
                    String vaccineName = resultSet.getString("vaccineName");
                    Date date = resultSet.getDate("Time");
                    String patientName = resultSet.getString("patientUsername");
                    System.out.println(appointmentId + " " + vaccineName + " " + date + " " + patientName);
                }
            } else if (currentPatient != null) {
                // check 3: if Patient logged in
                // format is "vaccine name, date, caregiver id"
                String query = "SELECT appointmentID, vaccineName, Time, caregiverUsername FROM Appointments WHERE patientUsername = ? ORDER BY AppointmentID";
                PreparedStatement statement = con.prepareStatement(query);
                statement.setString(1, currentPatient.getUsername());
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    int appointmentId = resultSet.getInt("appointmentID");
                    String vaccineName = resultSet.getString("vaccineName");
                    Date date = resultSet.getDate("Time");
                    String caregiverName = resultSet.getString("caregiverUsername");
                    System.out.println(appointmentId + " " + vaccineName + " " + date + " " + caregiverName);
                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    // TODO: Part 2
    private static void logout(String[] tokens) {
        // check 1: if a user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
        } else if(tokens.length != 1) {
            System.out.println("Please try again");
        } else {
            // log out the user
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out");
        }
    }
}
