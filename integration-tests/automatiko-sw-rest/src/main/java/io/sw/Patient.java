package io.sw;

public class Patient {

    public String id;
    public String name;
    public String condition;
    public Doctor doctor;

    public Patient() {}

    public Patient(String id, String name, String condition) {
        this.id = id;
        this.name = name;
        this.condition = condition;
    }

    public Patient(String id, String name, String condition, Doctor doctor) {
        this.id = id;
        this.name = name;
        this.condition = condition;
        this.doctor = doctor;
    }



}
