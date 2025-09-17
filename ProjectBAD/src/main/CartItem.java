package main;

public class CartItem {
    private String name;
    private double price;
    private int quantity;
    private double total;

    private String donutID; 

    public CartItem(String donutID, String name, double price, int quantity, double total) {
        this.donutID = donutID;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.total = total;
    }

    public String getDonutID() {
        return donutID;
    }

    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public double getTotal() { return total; }
}