package main;

import collections.Album;
import collections.Coordinates;
import collections.MusicBand;
import collections.StringXMLItem;
import database.Operator;
import main.Comparators.ComparatorID;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * CollectionHolder stores and operates with data.
 * All data stores in HashMap (elements are MusicBand classes)
 *
 * @author Grebenkin Vadim
 */
public class CollectionHolder {
    /**
     * pass-link to the loading xml file
     */
    private String dataPath;

    private int mapLength = 0;

    public Map<Integer, MusicBand> getMap() {
        return map;
    }

    /**
     * map is a {@link HashMap} that stores all the music groups data
     */
    private volatile Map<Integer, MusicBand> map = new HashMap<>();
    /**
     * input stream from the client
     */
    private BufferedInputStream inStream;
    /**
     * mass is a sorted array of map's ids
     */
    private int[] mass;
    /**
     * creationDate contents the date of CollectionHolder creation
     */
    private Date creationDate;

    // new objects
    private final Operator operator;



    /**
     * Constructor. Automatically sort object after creation
     * @param operator
     */
    public CollectionHolder(Operator operator) {
        this.operator = operator;
        this.loadData();
    }

    /**
     * loads data from database
     */
    public void loadData() {
        try {
            this.map = operator.getData();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("can not load the data!");
        }
    }

    /**
     * prints the {@link #map} elements into user's console
     *
     * @since 16.05.22 updated: stream CommandExecutor added
     */
    public void readMap() {
        System.out.println("\n" + "current data: " + "(" + this.map.size() + ")");
        this.map.values().forEach(band -> {
            System.out.println(band.getId() + ". " + band);
        });
        if (this.map.size() <= 0) System.out.println("Collection empty");
        System.out.print('\n');
    }

    /**
     * Prints the information about {@link #map}
     */
    public String mapInfo() {
        String message = "";
        message += ('\n');
        message += ("app.collections in storage " + "(" + this.mass.length + ")");
        message += ("creation date, time: " + this.creationDate);
        message += ("type of collection: " + this.map.getClass().getName());
        message += ('\n');
        return message;
    }

    /**
     * deletes all the storing MusicBands in {@link #map}
     */
    public synchronized void clearMap(String username) {
        operator.clearMap(username);
        loadData();
        System.out.println("Holder: "+username+"'s Collections cleared");
    }

    /**
     * adds new group to the {@link #map}
     *
     * @param newBand - new {@link MusicBand} to be added
     */
    public synchronized void addNewGroup(MusicBand newBand) {
        this.operator.appendBand(newBand);
        loadData();
        System.out.println("Holder: successfully added");
    }

    /**
     * {@link #mapLength} getter
     */
    public int getMapLength() {
        return mapLength;
    }


    /**
     * adding the new {@link MusicBand} into {@link #map} by its id
     *
     * @param newBand - new element {@link MusicBand} to add to {@link #map}
     * @param id      - the id of the new element
     */
    @Deprecated
    public void addNewGroup(MusicBand newBand, int id) { // update
        this.map.put(newBand.getId(), newBand);
        System.out.println("Holder: successfully added");
    }

    /**
     * deletes {@link #map} element with current id
     *
     * @param id - id of deleting element
     */
    public void deleteElement(String id) {
        Integer ID;
        try {
            ID = new Integer(id);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        this.map.remove(ID);
        this.mass = Tools.removeID(this.mass, ID);
        //this.sort(HolderSortTypes.DEFAULT);
    }

    public void deleteElement(Integer id) {
        String message = map.get(id).getName();
        this.map.remove(id);
        this.mass = Tools.removeID(this.mass, id);
        System.out.println("Server: group " + message + "deleted");
    }

    /**
     * Prints the {@link #map} element into client's console by its position in sorted array
     *
     * @param position - position of required element
     */
    public void ReadCollectionElement(int position) {
        if (mass.length >= position - 1) {
            System.out.println(this.map.get(mass[position - 1]).toString());
        } else {
            System.out.println("no elements requires these parameters");
        }
    }

    /**
     * Removes all {@link #map} element's which Number of participants is lower than one
     */
    @Deprecated
    public void removeLower(Long numberOfParticipants) {
        if (numberOfParticipants == null) {
            return;
        }
        Vector<Integer> vector = new Vector<Integer>();
        for (int i = 0; i != mass.length; i++) {
            Long current = this.map.get(mass[i]).getNumberOfParticipants();
            if (current != null) {
                if (current < numberOfParticipants) {
                    vector.add(mass[i]);
                }
            }
        }
        // deleting
        for (Integer i : vector) {
            this.deleteElement(i.toString());
        }
    }

    /**
     * getter for {@link #mass}
     */
    public int[] getIDs() {
        return this.mass;
    }

    /**
     * Getter for NumberOfParticipants of the {@link #map} element with current id
     *
     * @param bandID - id of the {@link #map} element
     * @return Number of Participants of the {@link MusicBand} in {@link #map} with bandID
     */
    public Long getNumberOfParticipants(int bandID) {
        if (this.map.get(bandID) != null)
            return this.map.get(bandID).getNumberOfParticipants();
        else return null;
    }

    /**
     * replaces the existing {@link #map} element by a new one
     *
     * @param newBand - new Element
     *                if there is no such element it will add it
     */
    public void replaceGroup(MusicBand newBand) {
        this.map.put(newBand.getId(), newBand);
    }

    public MusicBand getMinGroup() {
        Comparator<Map.Entry<Integer, MusicBand>> comparatorID = new ComparatorID();
        if (!map.isEmpty()) {
            return map.entrySet().stream().min(comparatorID).get().getValue();
        } else return null;
    }

    public Stream<MusicBand> getMapStream() {
        return this.map.values().stream();
    }
}
