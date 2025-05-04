package fr.univlr.info.AppointmentAPIV1.controller;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import fr.univlr.info.AppointmentAPIV1.store.AppointmentRepository;
import fr.univlr.info.AppointmentAPIV1.store.DoctorRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(path = "/api")
@Validated
public class AppointmentController {
    private final AppointmentRepository apptRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentModelAssembler appointmentModelAssembler;


    public AppointmentController(AppointmentRepository apptRepository, DoctorRepository doctorRepository, AppointmentModelAssembler appointmentModelAssembler, AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.apptRepository = apptRepository;
        this.appointmentModelAssembler = appointmentModelAssembler;
    }


    // Si le client spécifie dans l'en-tête de la requête Accept: application/json alors le serveur lui retournera un json classique
    @GetMapping(value = "/appointments", produces = "application/json")
    ResponseEntity<Collection<Appointment>> all(@RequestParam(value = "date", required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date) {
        List<Appointment> appts;

        // Si ma requête contient un parametre d'URL date, alors j'appelle la méthode définit dans mon interface.
        // c’est une méthode de requête dérivée JPA. Il n'est pas nécessaire que je l'implémente
        if (date != null) {
            appts = apptRepository.findByStartDateAfter(date);
        } else {
            appts = apptRepository.findAll();
        }

        return new ResponseEntity<>(appts, HttpStatus.OK);
    }

    // Si le client spécifie dans l'en-tête de la requête accept: application/hal+json alors le serveur lui retournera un json au format HAL
    @GetMapping(value = "/appointments", produces = "application/hal+json")
    public CollectionModel<EntityModel<Appointment>> allHal(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date) {

        List<Appointment> appts = (date != null)
                ? apptRepository.findByStartDateAfter(date)
                : apptRepository.findAll();

        List<EntityModel<Appointment>> appointmentResources = appts.stream()
                .map(appointmentModelAssembler::toModel)
                .collect(Collectors.toList());

        return CollectionModel.of(
                appointmentResources,
                linkTo(methodOn(AppointmentController.class).allHal(null)).withSelfRel()
        );
    }

    @GetMapping("/appointments/{id}")
    public EntityModel<Appointment> getAppointmentById(@PathVariable Long id) {
        Appointment appt = apptRepository.findById(id).orElseThrow(()-> new AppointmentNotFoundException(id));
        return EntityModel.of(appt, //
                linkTo(methodOn(AppointmentController.class).getAppointmentById(id)).withSelfRel(),
                linkTo(methodOn(AppointmentController.class).all(null)).withRel("appointments"));
    }

    @PostMapping("/appointments")
    public ResponseEntity<Appointment> newAppointment(@Valid @RequestBody Appointment appt) {
        Doctor doctor = doctorRepository.findByName(appt.getDoctor());

        // Validation de la date de début et de fin
        // Je vérifie que les dates de début et de fin du rendez-vous sont valides.
        // Si l'une des deux est dans le passé (avant la date et l'heure actuelles), cela signifie
        // que l'utilisateur essaie de créer un rendez-vous rétroactif, ce qui est interdit.
        // Dans ce cas, je retourne une réponse HTTP 400 (Bad Request), avec un corps vide,
        // pour indiquer clairement que la requête envoyée est incorrecte.

        if (appt.getStartDate().before(new Date()) || appt.getEndDate().before(new Date())) {
            // Si la date de début ou de fin est dans le passé, je retourne une erreur 400 (Bad Request)
            return ResponseEntity.badRequest().body(null);
        }

        // Ici je vérifie les conflits avec les rendez-vous existants du médecin
        List<Appointment> existingAppointments = apptRepository.findByDoctor(doctor.getName());
        for (Appointment existingAppointment : existingAppointments) {
            // Vérification si les plages horaires se chevauchent
            if (isTimeOverlap(appt, existingAppointment)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        // J'appelle le repository pour me permettre de communiquer avec la BDD et persister ma ressource
        Appointment savedAppointment = apptRepository.save(appt);

        // Si le rendez-vous a un docteur associé, on l'ajoute à la liste de ses rendez-vous
        if (appt.getDoctor() != null) {
                doctor.getAppointments().add(savedAppointment);
                doctorRepository.save(doctor);
            }


        // À partir d'ici et après la sauvegarde en BDD, je crée l'URL
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedAppointment.getId())
                .toUri();

        // Enfin, dernière étape. Je retourne une réponse avec le code 201 (Created) et l'URL de la ressource en question
        return ResponseEntity.created(location).body(savedAppointment);
    }


    // Méthode pour vérifier si les créneaux horaires se chevauchent. Par exemple, si la date de fin est avant la date de début je retourne false
    private boolean isTimeOverlap(Appointment appt1, Appointment appt2) {
        return !(appt1.getEndDate().toInstant().isBefore(appt2.getStartDate().toInstant()) || appt1.getStartDate().toInstant().isAfter(appt2.getEndDate().toInstant()));
    }

    /**
     * Cette fois, avec cette méthode, je récupère un Appointment à partir de son identifiant (id).
     *
     * Cette Méthode est appelée lorsqu'une requête GET est envoyée à l'URL /api/appointments/{id}.
     * - Si l'Appointment existe en base de données, on le retourne avec le code HTTP 200 (OK).
     * - Si l'Appointment n'existe pas, on retourne une réponse avec le code HTTP 404 (Not Found).
     *
     * @param id L'identifiant du rendez-vous à récupérer
     * @return ResponseEntity contenant soit l'Appointment (status 200), soit une réponse vide (status 404)
     */

    @PutMapping("/appointments/{id}")
    public ResponseEntity<Appointment> updateAppointment(@PathVariable Long id, @Valid @RequestBody Appointment appt) {
        // Vérifier si le rendez-vous existe
        Appointment existingAppt = apptRepository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));

        // Mettre à jour les champs nécessaires du rendez-vous
        BeanUtils.copyProperties(appt, existingAppt, "id"); // J'exclus l'ID de la copie car je ne veux pas modifier l'identifiant

        // Sauvegarder les modifications dans la base de données
        Appointment updatedAppointment = apptRepository.save(existingAppt);

        return new ResponseEntity<>(updatedAppointment, HttpStatus.OK);

    }

    @DeleteMapping("appointments/{id}")
    public ResponseEntity<Appointment> deleteAppointment(@PathVariable Long id) {
        Appointment existingAppt = apptRepository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));
        try{
            apptRepository.delete(existingAppt);
        } catch (HttpStatusCodeException e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(existingAppt, HttpStatus.OK);
    }

    @DeleteMapping("/appointments/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id) {

        // Je tente d'abord d'aller récupérer le rendez-vous avec son id
        Appointment appointment = apptRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));

        // J'évacue d'abord les scénarios d'erreur et je vérifie si la date de début du rendez-vous est dans le passé
        if (appointment.getStartDate().before(new Date())) {
            // Si la date est dans le passé, je retourne une erreur 409 comme attendu dans le test
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header(HttpHeaders.CONTENT_TYPE, MediaTypes.HTTP_PROBLEM_DETAILS_JSON_VALUE)
                    .body(Problem.create()
                            .withTitle("Method not allowed")
                            .withDetail("You can't cancel an appointment that is in the past"));
        }

        // Si le rendez-vous peut être annulé, je le supprime
        apptRepository.delete(appointment);
        apptRepository.save(appointment);

        // je retourne une réponse indiquant que l'annulation a réussi
        return ResponseEntity.ok(appointmentModelAssembler.toModel(appointment));
    }



    @DeleteMapping("appointments")
    public ResponseEntity<Appointment> deleteAllAppointments() {
        // J'évacue le scénario où je n'aurai rien en bdd
        if(apptRepository.findAll().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        apptRepository.deleteAll();

        return new ResponseEntity<>(null, HttpStatus.OK);
    }
}
