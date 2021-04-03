package io.sw;

public class Patient {

    public String identifier;
    public String name;
    public String condition;
    public Doctor doctor;

    public Patient() {
    }

    public Patient(String identifier, String name, String condition) {
        this.identifier = identifier;
        this.name = name;
        this.condition = condition;
    }

    public Patient(String identifier, String name, String condition, Doctor doctor) {
        this.identifier = identifier;
        this.name = name;
        this.condition = condition;
        this.doctor = doctor;
    }

}
