package uk.ac.tees.aad.studentnumber.w19583132;

public class FoodItem {
    private String name;
    private String description;
    private String id;
    private String image;
    private double price;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }



    public FoodItem(String name, String description, String id, String image, double price) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.image = image;
        this.price = price;
    }


    public FoodItem() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
