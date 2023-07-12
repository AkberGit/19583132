package uk.ac.tees.aad.studentnumber.w19583132;

import java.util.ArrayList;

public interface DataCallback {
    void onDataReceived(ArrayList<FoodItem> foodItems);
    void onError(String errorMessage);
}
