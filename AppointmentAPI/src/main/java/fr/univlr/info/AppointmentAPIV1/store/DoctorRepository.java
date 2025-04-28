package fr.univlr.info.AppointmentAPIV1.store;

import fr.univlr.info.AppointmentAPIV1.controller.DoctorNotFoundException;
import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository  extends JpaRepository<Doctor,Long> {
    Doctor findByName(String name) throws DoctorNotFoundException;
}
