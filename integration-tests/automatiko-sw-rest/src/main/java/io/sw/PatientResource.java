package io.sw;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

        patients.add(patient);
        return Response.ok(patient).build();
    }

    @DELETE
    public Set<Patient> delete(Patient patient) {
        patients.removeIf(existingPatient -> existingPatient.identifier.contentEquals(patient.identifier));
        return patients;
    }
}
