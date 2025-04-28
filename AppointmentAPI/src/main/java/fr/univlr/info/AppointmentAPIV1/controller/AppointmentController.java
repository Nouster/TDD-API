package fr.univlr.info.AppointmentAPIV1.controller;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import fr.univlr.info.AppointmentAPIV1.store.AppointmentRepository;
import fr.univlr.info.AppointmentAPIV1.store.DoctorRepository;
import org.springframework.beans.BeanUtils;
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
import java.util.Optional;

@RestController
@RequestMapping(path = "/api")
@Validated
public class AppointmentController {
    private final AppointmentRepository apptRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentController(AppointmentRepository apptRepository, DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
        this.apptRepository = apptRepository;
    }


    @GetMapping("/appointments")
    ResponseEntity<Collection<Appointment>> all() {
        List<Appointment> appts = apptRepository.findAll();
        return new ResponseEntity<>(appts, HttpStatus.OK);
    }

    @GetMapping("/appointments/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id) {
        Appointment appt = apptRepository.findById(id).orElseThrow(()-> new AppointmentNotFoundException(id));
        return new ResponseEntity<>(appt, HttpStatus.OK);
    }

    @PostMapping("/appointments")
    public ResponseEntity<Appointment> newAppointment(@Valid @RequestBody Appointment appt) {
        // Validation de la date de début et de fin
        if (appt.getStartDate().before(new Date()) || appt.getEndDate().before(new Date())) {
            // Si la date de début ou de fin est dans le passé, je retourne une erreur 400 (Bad Request)
            return ResponseEntity.badRequest().body(null);
        }

        // J'appelle le repository pour me permettre de communiquer avec la BDD et persister ma ressource
        Appointment savedAppointment = apptRepository.save(appt);

        // Si le rendez-vous a un docteur associé, on l'ajoute à la liste de ses rendez-vous
        /*if (appt.getDoctor() != null) {
            Doctor doctor = doctorRepository.findByName(appt.getDoctor());
            if (doctor != null) {
                doctor.getAppointments().add(savedAppointment);
                doctorRepository.save(doctor);
            }
        }*/

        // À partir d'ici et après la sauvegarde en BDD, je crée l'URL
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedAppointment.getId())
                .toUri();

        // Enfin, dernière étape. Je retourne une réponse avec le code 201 (Created) et l'URL de la ressource en question
        return ResponseEntity.created(location).body(savedAppointment);
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

    @DeleteMapping("appointments")
    public ResponseEntity<Appointment> deleteAllAppointments() {
        if(apptRepository.findAll().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        apptRepository.deleteAll();

        return new ResponseEntity<>(null, HttpStatus.OK);
    }
}
