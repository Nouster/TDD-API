package fr.univlr.info.AppointmentAPIV1.controller;


import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import fr.univlr.info.AppointmentAPIV1.store.AppointmentRepository;
import fr.univlr.info.AppointmentAPIV1.store.DoctorRepository;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(path = "/api")
public class DoctorController {
    // Ces interfaces qui étendent JpaRepository me permettent de communiquer avec la BDD
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorModelAssembler doctorModelAssembler;


    public DoctorController(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository, DoctorModelAssembler doctorModelAssembler) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorModelAssembler = doctorModelAssembler;
    }

    // Même chose que pour les appointments :
    // Si le client fournit dans l'en-tête de sa requête `Accept: application/json`,
    // le serveur lui répondra avec un JSON classique, sans liens hypermédia.
    @GetMapping(value = "/doctors", produces = "application/json")
    ResponseEntity<Collection<Doctor>> all() {
        List<Doctor> doctors = doctorRepository.findAll();
        return new ResponseEntity<>(doctors, HttpStatus.OK);
    }

    // Par contre, si le client fournit `Accept: application/hal+json` dans l'en-tête de la requête,
    // alors le serveur renverra une réponse enrichie au format HAL (Hypertext Application Language).
    // Chaque ressource médecin (Doctor) sera alors encapsulée dans un `EntityModel` avec des liens hypermédia
    // ça permettra de naviguer facilement vers des ressources liées.
    @GetMapping(value = "/doctors", produces = "application/hal+json")
    public CollectionModel<EntityModel<Doctor>> allHal() {
        List<EntityModel<Doctor>> doctors = doctorRepository.findAll().stream()
                .map(doctorModelAssembler::toModel)
                .collect(Collectors.toList());

        return CollectionModel.of(doctors,
                linkTo(methodOn(DoctorController.class).allHal()).withSelfRel());
    }

    @GetMapping("/doctors/{name}")
    public ResponseEntity<Doctor> findDoctor(@PathVariable String name) {
        Doctor doctor = doctorRepository.findByName(name);

        // L'exception sera gérée au niveau de la classe GlobalExceptionHandler
        if (doctor == null) {
            throw new DoctorNotFoundException(name);
        }

        return ResponseEntity.ok(doctor);
    }

    @GetMapping(value = "/doctors/{name}", produces = "application/hal+json")
    public EntityModel<Doctor> oneHal(@PathVariable String name) {

        Doctor doctor = doctorRepository.findByName(name);

        // L'exception sera gérée au niveau de la classe GlobalExceptionHandler
        if (doctor == null) {
            throw new DoctorNotFoundException(name);
        }

        return doctorModelAssembler.toModel(doctor);
    }

    @GetMapping(value = "/doctors/{name}/appointments", produces = "application/json")
    public ResponseEntity<List<Appointment>> getAppointmentsForDoctor(@PathVariable String name) {

        Doctor doctor = doctorRepository.findByName(name);

        if (doctor == null) {
            throw new DoctorNotFoundException(name);
        }

        // Pour Rechercher les rendez-vous associés au docteur
        List<Appointment> appointments = appointmentRepository.findByDoctor(name);

        return ResponseEntity.ok(appointments);
    }

    // Si le client spécifie dans l'en-tête de sa requête qu'il souhaite que le serveur lui retourne la réponse
    // avec des liens hypermedias
    @GetMapping(value = "/doctors/{name}/appointments", produces = "application/hal+json")
    public CollectionModel<EntityModel<Appointment>> getAppointmentsForDoctorHal(@PathVariable String name) {
        Doctor doctor = doctorRepository.findByName(name);
        if (doctor == null) {
            throw new DoctorNotFoundException(name);
        }

        List<Appointment> appointments = appointmentRepository.findByDoctor(name);

        List<EntityModel<Appointment>> appointmentResources = appointments.stream()
                .map(appointment -> EntityModel.of(
                        appointment,
                        linkTo(methodOn(AppointmentController.class)
                                .getAppointmentById(appointment.getId())).withSelfRel()))
                .collect(Collectors.toList());

        return CollectionModel.of(
                appointmentResources,
                linkTo(methodOn(DoctorController.class)
                        .getAppointmentsForDoctorHal(name)).withSelfRel());
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

        // Si le médecin n'existe pas on s'arrête et l'exception sera gérée par le GlobalExceptionHandler
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
