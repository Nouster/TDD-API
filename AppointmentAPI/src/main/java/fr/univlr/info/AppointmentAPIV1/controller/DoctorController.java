package fr.univlr.info.AppointmentAPIV1.controller;


import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
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

    public DoctorController(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @GetMapping("/doctors")
    ResponseEntity<Collection<Doctor>> all() {
        List<Doctor> doctors = doctorRepository.findAll();
        return new ResponseEntity<>(doctors, HttpStatus.OK);
    }

    @GetMapping("/doctors/{name}")
    ResponseEntity<Doctor> findDoctor(@PathVariable String name) {
        Doctor doctor = new Doctor();
        try{
             doctor = doctorRepository.findByName(name);
        } catch (DoctorNotFoundException e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(doctor, HttpStatus.OK);
    }


    @PostMapping("/doctors")
    ResponseEntity<Doctor> create(@RequestBody Doctor doctor) {
        Doctor savedDoctor = doctorRepository.save(doctor);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{name}")
                .buildAndExpand(savedDoctor.getId())
                .toUri();

        return new ResponseEntity<>(savedDoctor, HttpStatus.CREATED);
    }

}
