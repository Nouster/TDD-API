package fr.univlr.info.AppointmentAPIV1.controller;


import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import fr.univlr.info.AppointmentAPIV1.store.AppointmentRepository;
import fr.univlr.info.AppointmentAPIV1.store.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping(path = "/api")
public class DoctorController {
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorController(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/doctors")
    ResponseEntity<Collection<Doctor>> all() {
        List<Doctor> doctors = doctorRepository.findAll();
        return new ResponseEntity<>(doctors, HttpStatus.OK);
    }

    @GetMapping("/doctors/{name}")
    public ResponseEntity<Doctor> findDoctor(@PathVariable String name) {
        Doctor doctor = doctorRepository.findByName(name);

        if (doctor == null) {
            throw new DoctorNotFoundException(name);
        }

        return ResponseEntity.ok(doctor);
    }


    @GetMapping("/doctors/{name}/appointments")
    public ResponseEntity<List<Appointment>> getAppointmentsForDoctor(@PathVariable String name) {
        // Recherche du docteur par son nom
        Doctor doctor = doctorRepository.findByName(name);
        if (doctor == null) {
            throw new DoctorNotFoundException(name);
        }

        // Recherche des rendez-vous associés au docteur
        List<Appointment> appointments = appointmentRepository.findByDoctor(name);

        return ResponseEntity.ok(appointments);
    }


    @PostMapping("/doctors")
    ResponseEntity<Doctor> create(@RequestBody Doctor doctor) {
        Doctor savedDoctor = doctorRepository.save(doctor);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{name}")
                .buildAndExpand(savedDoctor.getId())
                .toUri();

        return ResponseEntity.created(location).body(savedDoctor);
    }

    // Dans la méthode ci-dessous, je vais d'abord évacuer les scénarios d'erreurs
    @DeleteMapping("/doctors/{name}")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        // Rechercher le médecin par son nom
        Doctor doctor = doctorRepository.findByName(name);

        // Si le médecin n'existe pas on s'arrête
        if (doctor == null) {
            throw new DoctorNotFoundException(name);
        }

        // Vérifier si le médecin a des rendez-vous associés
        List<Appointment> appointments = appointmentRepository.findByDoctor(name);

        // Ci-dessous, si la condition me renvoie true et donc que le tableau n'est pas vide, ça veut dire que le médecin a des rendez-vous qui lui sont associés
        if (!appointments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Si aucun rendez-vous, on peut supprimer le médecin
        doctorRepository.delete(doctor);
        return ResponseEntity.noContent().build(); // Retourne 204 No Content après suppression réussie car je n'ai rien à retourner

    }

}
