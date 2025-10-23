import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;
import java.sql.ResultSet;

public class HospitalManagement {
    private static final String url = "jdbc:mysql://localhost:3306/hospital";
    private static final String username = "root";
    private static final String password = ""; 

    public static void main(String args[]) {
        Scanner scanner = new Scanner(System.in);
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("✅ Connection successful!");

            Patient patient = new Patient(connection, scanner);
            Doctors doctor = new Doctors(connection);

            while (true) {
                System.out.println("\n=== HOSPITAL MANAGEMENT SYSTEM ===");
                System.out.println("1. Add Patient");
                System.out.println("2. View Patients");
                System.out.println("3. View Doctors");
                System.out.println("4. Book Appointment");
                System.out.println("5. View Appointments");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        patient.addPatient();
                        break;

                    case 2:
                        patient.viewPatients();
                        break;

                    case 3:
                        doctor.viewDoctors();
                        break;

                    case 4:
                        // Show available doctors first
                        System.out.print("Enter appointment date (YYYY-MM-DD) to see available doctors: ");
                        String date = scanner.next();
                        showAvailableDoctors(date, connection);
                        bookAppointment(patient, doctor, connection, scanner);
                        break;

                    case 5:
                        viewAppointments(connection);
                        break;

                    case 6:
                        connection.close();
                        System.out.println("🔒 Connection closed. Goodbye!");
                        scanner.close();
                        return;

                    default:
                        System.out.println("⚠️ Invalid choice! Try again.");
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.out.println("❌ Failed to close connection: " + e.getMessage());
                }
            }
            scanner.close();
        }
    }

    // ✅ Booking Appointment
    public static void bookAppointment(Patient patient, Doctors doctor, Connection connection, Scanner scanner) {
        System.out.print("Enter patient ID: ");
        int patientId = scanner.nextInt();
        System.out.print("Enter doctor ID: ");
        int doctorId = scanner.nextInt();
        System.out.print("Enter appointment date (YYYY-MM-DD): ");
        String appointmentDate = scanner.next();

        if (patient.getPatientById(patientId) && doctor.getDoctorById(doctorId)) {
            if (checkDoctorAvailability(doctorId, appointmentDate, connection)) {
                String appointmentQuery = "INSERT INTO appointments (patient_id, doctor_id, appointment_date) VALUES (?, ?, ?)";
                try {
                    PreparedStatement preparedStatement = connection.prepareStatement(appointmentQuery);
                    preparedStatement.setInt(1, patientId);
                    preparedStatement.setInt(2, doctorId);
                    preparedStatement.setDate(3, java.sql.Date.valueOf(appointmentDate));

                    int affectedRows = preparedStatement.executeUpdate();
                    if (affectedRows > 0) {
                        System.out.println("✅ Appointment booked successfully for " + appointmentDate + "!");
                    } else {
                        System.out.println("❌ Failed to book appointment!");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("❌ Invalid patient ID or doctor ID!");
        }
    }

    // ✅ Check Doctor Availability
    public static boolean checkDoctorAvailability(int doctorId, String appointmentDate, Connection connection) {
        String query = "SELECT COUNT(*) FROM appointments WHERE doctor_id = ? AND appointment_date = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, doctorId);
            preparedStatement.setDate(2, java.sql.Date.valueOf(appointmentDate));

            ResultSet resultset = preparedStatement.executeQuery();
            if (resultset.next()) {
                int count = resultset.getInt(1);
                if (count == 0) {
                    System.out.println("✅ Doctor is available on " + appointmentDate + "!");
                    return true;
                } else {
                    System.out.println("⚠️ Doctor is not available on " + appointmentDate + "!");
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Show all appointments
    public static void viewAppointments(Connection connection) {
        String query = "SELECT a.id, p.name AS patient_name, d.name AS doctor_name, a.appointment_date " +
                       "FROM appointments a " +
                       "JOIN patients p ON a.patient_id = p.id " +
                       "JOIN doctors d ON a.doctor_id = d.id";

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery();

            System.out.println("\n+----+----------------+----------------+--------------+");
            System.out.println("| ID | Patient Name   | Doctor Name    | Date         |");
            System.out.println("+----+----------------+----------------+--------------+");

            while (rs.next()) {
                System.out.printf("| %-2d | %-14s | %-14s | %-12s |\n",
                        rs.getInt("id"),
                        rs.getString("patient_name"),
                        rs.getString("doctor_name"),
                        rs.getDate("appointment_date"));
            }

            System.out.println("+----+----------------+----------------+--------------+\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ Show available doctors for a specific date
    public static void showAvailableDoctors(String date, Connection connection) {
        String query = "SELECT id, name, specialization FROM doctors " +
                       "WHERE id NOT IN (SELECT doctor_id FROM appointments WHERE appointment_date = ?)";

        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();

            System.out.println("\nAvailable Doctors on " + date + ":");
            System.out.println("+----+----------------+----------------+");
            System.out.println("| ID | Name           | Specialization |");
            System.out.println("+----+----------------+----------------+");
            while (rs.next()) {
                System.out.printf("| %-2d | %-14s | %-14s |\n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("specialization"));
            }
            System.out.println("+----+----------------+----------------+\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
