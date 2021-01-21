package io.sw;

import java.util.List;

public class Doctor {
    public String name;
    public String type;
    public String img;
    public List<String> conditions;

    public Doctor() {}

    public Doctor(String name, String type, String img, List<String> conditions) {
        this.name = name;
        this.type = type;
        this.img = img;
        this.conditions = conditions;
    }
}
