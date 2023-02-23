package io.sw;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/patients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {
    private Set<Patient> patients = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public PatientResource() {
    }

    @GET
    public Set<Patient> list() {
        return patients;
    }

    @POST
    public Response add(Patient patient) {

        if (patient.condition == null || patient.condition.trim().length() < 1) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        patient.doctor = new Doctor("Doctor No", null, null, null);
        patients.add(patient);
        return Response.ok(patient).build();
    }

    @DELETE
    public Set<Patient> delete(Patient patient) {
        patients.removeIf(existingPatient -> existingPatient.identifier.contentEquals(patient.identifier));
        return patients;
    }
}
