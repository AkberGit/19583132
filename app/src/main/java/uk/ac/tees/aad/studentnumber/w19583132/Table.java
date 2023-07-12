package uk.ac.tees.aad.studentnumber.w19583132;

import java.util.HashMap;
import java.util.List;

public class Table {
    String tableId;
    boolean available;
    String currentUId;
    HashMap<String, Integer> orders;

    public Table(String tableId, boolean available, String currentUId, HashMap<String, Integer> orders) {
        this.tableId = tableId;
        this.available = available;
        this.currentUId = currentUId;
        this.orders = orders;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getCurrentUId() {
        return currentUId;
    }

    public void setCurrentUId(String currentUId) {
        this.currentUId = currentUId;
    }

    public Table(){}
    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public boolean isAvailable() {
        return this.currentUId.isEmpty();
    }

    public HashMap<String, Integer> getOrders() {
        return orders;
    }

    public void setOrders(HashMap<String, Integer> orders) {
        this.orders = orders;
    }
}
