package io.sw;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/schedule")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SchedulingResource {
    private Set<Doctor> doctors = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));
    private Set<Patient> assignedPatients = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public SchedulingResource() {
        doctors.add(new Doctor("Dr. Michael", "Cardiologist", "", Arrays.asList("irregular heart beat")));
        doctors.add(new Doctor("Dr. Elisabeth", "Urologist", "", Arrays.asList("bladder infection")));
        doctors.add(new Doctor("Dr. Johnson", "Pulmonologists", "", Arrays.asList("breathing problems")));
    }

    @GET
    public Set<Patient> list() {
        return assignedPatients;
    }

    @POST
    public Response assign(Patient patient) {

        try {
            patient.doctor = matchOnCondition(patient);
            assignedPatients.add(patient);
            return Response.ok(patient).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private Doctor matchOnCondition(Patient patient) {

        return doctors.stream()
                .filter(doc -> doc.conditions.contains(patient.condition))
                .findFirst().get();
    }
}
