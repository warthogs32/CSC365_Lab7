import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;

import java.util.*;
import java.time.LocalDate;

class InnReservations {
    public static void main(String[] args) {
        try {
            InnReservations ir = new InnReservations();
            ir.askUser();
        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
        }
    }

    private void askUser() throws SQLException {
        Scanner sc = new Scanner(System.in);
        String answer = "";
        while (!answer.toLowerCase().equals("q")) {
            System.out.println("\nChoose Option: \n");
            System.out.println("[G]et Rooms and Rates");
            System.out.println("[F]ind Available Reservations");
            System.out.println("[C]hange Reservation");
            System.out.println("[Ca]ncel Reservation");
            System.out.println("[Q]uit Program");
            answer = sc.nextLine();
            if (answer.toLowerCase().equals("g")) {
                RoomsAndRates();
            } else if (answer.toLowerCase().equals("f")) {
                Reservations();
            } else if (answer.toLowerCase().equals("c")) {
                ReservationChange();
            } else if (answer.toLowerCase().equals("ca")) {
                ReservationCancellation();
            }
        }
    }


    private void RoomsAndRates() throws SQLException {
        String sql = "SELECT rp.Room, Popularity, NextAvailable, LengthStay, mp.CheckOut  FROM\n" +
                "(SELECT r.Room, MAX(r.CheckOut),\n" +
                "ROUND(SUM(DATEDIFF(LEAST(r.CheckOut, CurDate()),\n" +
                "    GREATEST(r.CheckIn, CurDate() - INTERVAL 180 DAY)))/180, 2) AS Popularity\n" +
                "FROM lab7_reservations r\n" +
                "JOIN lab7_rooms rm ON r.Room = rm.RoomCode\n" +
                "WHERE r.CheckOut >= (CurDate() - INTERVAL 180 DAY)\n" +
                "AND r.CheckIn <= CurDate()\n" +
                "GROUP BY r.Room) rp\n" +
                " \n" +
                "JOIN\n" +
                " \n" +
                "(SELECT r.Room, MAX(r.CheckOut) + INTERVAL 1 DAY AS NextAvailable\n" +
                "FROM lab7_reservations r\n" +
                "JOIN lab7_rooms rm ON r.Room = rm.RoomCode\n" +
                "GROUP BY r.Room) av\n" +
                " \n" +
                "ON rp.Room = av.Room\n" +
                " \n" +
                "JOIN\n" +
                " \n" +
                "(SELECT t1.Room, DATEDIFF(t1.CheckOut, t1.CheckIn) AS LengthStay, t1.Checkout FROM\n" +
                "(SELECT *\n" +
                "FROM lab7_reservations r\n" +
                "JOIN lab7_rooms rm ON r.Room = rm.RoomCode) t1\n" +
                " \n" +
                "JOIN\n" +
                " \n" +
                "(SELECT r.Room, MAX(r.CheckOut) AS MostRecent\n" +
                "FROM lab7_reservations r\n" +
                "JOIN lab7_rooms rm ON r.Room = rm.RoomCode\n" +
                "WHERE r.CheckOut >= (CurDate() - INTERVAL 180 DAY)\n" +
                "AND r.CheckIn <= CurDate()\n" +
                "GROUP BY r.Room) t2\n" +
                "ON t1.Room = t2.Room AND t1.CheckOut = t2.MostRecent) mp\n" +
                " \n" +
                "ON rp.Room = mp.Room\n" +
                " \n" +
                "ORDER BY Popularity;";

        String url = System.getenv("HP_JDBC_URL");
        String user = System.getenv("HP_JDBC_USER");
        String pass = System.getenv("HP_JDBC_PW");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Database connection acquired - processing query");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                System.out.println("RoomCode Popularity NextAvailable LengthStay Checkout");
                // Step 5: Receive results
                while (rs.next()) {
                    String room = rs.getString("Room");
                    float popularity = rs.getFloat("Popularity");
                    Date nextAvailable = rs.getDate("NextAvailable");
                    int lengthStay = rs.getInt("LengthStay");
                    Date checkout = rs.getDate("Checkout");
                    System.out.format("%s      %.2f       %s    %d          %s%n", room, popularity,
                                                                    nextAvailable, lengthStay, checkout);
                }
            }
        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
        }
    }

    private void Reservations() throws SQLException {
        String url = System.getenv("HP_JDBC_URL");
        String user = System.getenv("HP_JDBC_USER");
        String pass = System.getenv("HP_JDBC_PW");
        Scanner sc = new Scanner(System.in);
        System.out.println("Please Enter First Name: ");
        String firstName = sc.nextLine();
        System.out.println("Last Name: ");
        String lastName = sc.nextLine();
        System.out.println("Desired Room Code: ");
        String roomCode = sc.nextLine();
        System.out.println("Desired Bed Type: ");
        String bedType = sc.nextLine();
        System.out.println("Beginning Date of Stay: ");
        String beginDate = sc.nextLine();
        System.out.println("End Date of Stay: ");
        String endDate = sc.nextLine();
        System.out.println("Number of Children: ");
        String numChildren = sc.nextLine();
        System.out.println("Number of Adults: ");
        String numAdults = sc.nextLine();
        List<Object> params = new ArrayList<Object>();
        params.add(Integer.parseInt(numChildren) + Integer.parseInt(numAdults));
        params.add(Integer.parseInt(numChildren) + Integer.parseInt(numAdults));
        params.add(beginDate);
        params.add(endDate);
        StringBuilder sb = new StringBuilder("select rm.roomcode AS RoomCode\n" +
                "from lab7_rooms as rm\n" +
                "where rm.maxocc >= ?\n" +
                "and rm.roomcode not in (\n" +
                "select rv.room\n" +
                "from lab7_rooms as rm\n" +
                "join lab7_reservations as rv\n" +
                "on rm.roomcode = rv.room\n" +
                "where rm.maxOcc >= ?\n" +
                "and rv.checkout > ?\n" +
                "and rv.checkin < ?\n" +
                "group by rv.room);\n");

        if (!"any".equalsIgnoreCase(roomCode)) {
            sb.append(" AND rm.roomcode = ?");
            params.add(roomCode);
        }
        if (!"any".equalsIgnoreCase(bedType)) {
            sb.append(" AND rm.bedtype = ?");
            params.add(bedType);
        }
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Database connection acquired - processing query");
            try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
                int i = 1;
                for (Object p : params) {
                    pstmt.setObject(i++, p);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    System.out.println("Available Rooms:");
                    while (rs.next()) {
                        String availableRoom = rs.getString("RoomCode");
                        System.out.format("%s      %n", availableRoom);
                    }
                }
            }
        }  catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
        }
    }

    private void ReservationChange() throws SQLException {
        String url = System.getenv("HP_JDBC_URL");
        String user = System.getenv("HP_JDBC_USER");
        String pass = System.getenv("HP_JDBC_PW");
        Scanner sc = new Scanner(System.in);
    }

    private void ReservationCancellation() throws SQLException {
        String url = System.getenv("HP_JDBC_URL");
        String user = System.getenv("HP_JDBC_USER");
        String pass = System.getenv("HP_JDBC_PW");
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter reservation code you wish to cancel: ");
        int resCode = Integer.parseInt(sc.nextLine());
        StringBuilder sb = new StringBuilder("SELECT * FROM lab7_reservations\n" +
                "where code = ?;");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Database connection acquired - processing query");
            try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
                pstmt.setObject(1, resCode);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String code = rs.getString("Code");
                        String room = rs.getString("Room");
                        String checkin = rs.getString("Checkin");
                        String checkout = rs.getString("CheckOut");
                        String last = rs.getString("LastName");
                        String first = rs.getString("FirstName");
                        System.out.println("Is this the reservation you would like to cancel? (Y / N)");
                        System.out.format("%s %s %s %s %s %s %n\n", code, room, checkin, checkout, first, last);
                        String resp = sc.nextLine();
                        if (resp.toLowerCase().equals("y")) {
                            String deleteSql = "DELETE from lab7_reservations where code = ?";
                            try (PreparedStatement delstmt = conn.prepareStatement(deleteSql)) {
                                delstmt.setObject(1, resCode);
                                int rowCount = delstmt.executeUpdate();
                                if (rowCount == 1) {
                                    System.out.println("Reservation successfully deleted");
                                }
                            }
                        }
                    }

                }
            }
        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
        }
    }

}
