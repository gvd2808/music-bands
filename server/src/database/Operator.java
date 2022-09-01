package database;


import collections.Album;
import collections.Coordinates;
import collections.MusicBand;
import common.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Operator {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock wrightLock = lock.writeLock();

    static final String DB_URL = "jdbc:postgresql://localhost:5432/musicBands";
    private static final String username = "s335053";
    private static final String password = "klx776";
    private Connection connection;
    private Statement statement;
    private boolean isConnected = false;

    public Operator(){
        connect(username, password);
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Boolean appendBand(MusicBand band){ // TODO not working
        String sql = String.format("insert into %s" +
                "    (id, name, coordinate_x, coordinate_y, genre, number_of_participants," +
                "    singles_count, creation_date, username, best_album_name, best_album_sales)"+

                "    VALUES (DEFAULT, %s, %f, %f, %s, %d, %d, DEFAULT, %s, %s, %f)", URLS.BANDS.getPass(),
                    band.getName(), band.getCoordinates().getX(), band.getCoordinates().getY(), band.getGenre(), band.getNumberOfParticipants(),
                    band.getSinglesCount(), band.getUsername(), band.getBestAlbum().getName(), band.getBestAlbum().getSales()
        );
        try {
            wrightLock.lock();
            int updated = statement.executeUpdate(sql);
            wrightLock.unlock();
            if (updated > 0) return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Boolean updateBand(MusicBand band){
        String sql = String.format("update %s set" +
                        "    name = %s, coordinate_x = %f, coordinate_y = %f, genre = %s, number_of_participants = %d," +
                        "    singles_count = %d, best_album_name = %s, best_album_sales = %f" +
                        "    where id = %d and username = %s",
                URLS.BANDS.getPass(),
                band.getName(), band.getCoordinates().getX(), band.getCoordinates().getY(), band.getGenre(), band.getNumberOfParticipants(),
                band.getSinglesCount(), band.getBestAlbum().getName(), band.getBestAlbum().getSales(),
                band.getId(), band.getUsername());
        try {
            wrightLock.lock();
            int updated = statement.executeUpdate(sql);
            wrightLock.unlock();
            if (updated > 0) return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Boolean clearMap(String username){
        try {
            statement.execute(Requester.DELETE_USER_BANDS.get(new String[]{username}));
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    public Boolean authorize(User user) {
        String[] args = {user.getUsername()};

        try {
            readLock.lock();
            ResultSet res = statement.executeQuery(Requester.USER_HASH.get(args));
            readLock.unlock();
            System.out.println(res + " " + user.getUsername() + " - authorization");
            res.next();
            System.out.println(res.getString("passwordHash"));
            return res.getString("passwordHash").trim().equals(encodePass(user.getPassword()));
        } catch (SQLException throwable) {
            throwable.printStackTrace();
            System.out.println("authorization failed " + username);
            return false;
        }
    }

    public Boolean register(User user){
        String[] args = {user.getUsername(), encodePass(user.getPassword())};
        try {
            wrightLock.lock();
            statement.execute(Requester.ADD_USER.get(args));
            wrightLock.unlock();
            System.out.println("registration " + user.getUsername() + "authorization");
            return true;
        } catch (SQLException throwable) {
            throwable.printStackTrace();
            return false;
        }
    }

    public Map<Integer, MusicBand> getData() throws SQLException {
        ResultSet resultSet = loadData();
        if (resultSet == null) return null;
        Map<Integer, MusicBand> map = new HashMap<>();
        while (resultSet.next()) {
            MusicBand band = new MusicBand(resultSet.getDate("creation_date"));
            band.setId(resultSet.getInt("id"));
            band.setName(resultSet.getString("name"));
            band.setGenre(resultSet.getString("genre"));
            band.setCoordinates(new Coordinates(resultSet.getFloat("coordinate_x"), resultSet.getFloat("coordinate_y")));
            band.setBestAlbum(new Album(resultSet.getString("best_album_name"),resultSet.getFloat("best_album_sales")));
            band.setSinglesCount(resultSet.getLong("singles_count"));
            band.setNumberOfParticipants(resultSet.getLong("number_of_participants"));
            band.setUser(resultSet.getString("username"));
            if (band.isCorrect()){
                map.put(band.getId(), band);
            }
        }
        return map;
    }

    private ResultSet loadData(){
        try {
            return statement.executeQuery(Requester.LOAD_DATA.get(null));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean executeStatement(Requester request){
        try{
            statement.execute(request.get(null));
            return true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
    }

    /** hashing function */
    private String encodePass(String originalString){
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        byte[] encodedHash = digest.digest(
                originalString.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedHash);
    }
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /** admin authorisation */
    public synchronized void connect(String username, String password){
        try {
            this.connection = DriverManager.getConnection(DB_URL, username, password);
            if (!connection.isClosed()) System.out.println("Connected to PostgresSQL server");
            this.isConnected = !connection.isClosed();
        } catch (SQLException e) {
            System.out.println("Error in connecting postgresSQL server!");
            e.printStackTrace();
        }

    }
    public synchronized void disconnect(){
        try {
            this.statement.close();
            this.connection.close();
            isConnected = false;
            if (connection.isClosed()) System.out.println("PostgresSQL connection successfully closed");
        } catch (SQLException e) {
            System.out.println("ERROR: can not disconnect!");
        }
    }
}