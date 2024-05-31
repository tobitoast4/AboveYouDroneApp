package com.example.aboveyoudrone;

public class Drone {

    private String id;
    private int battery;
    private double price;

    public Drone(String id, int battery, double price) {
        this.id = id;
        this.battery = battery;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public int getBattery() {
        return battery;
    }

    public double getPrice() {
        return price;
    }
}
