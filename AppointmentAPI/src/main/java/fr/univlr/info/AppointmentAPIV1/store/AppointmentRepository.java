package fr.univlr.info.AppointmentAPIV1.store;

import fr.univlr.info.AppointmentAPIV1.model.Appointment;
import fr.univlr.info.AppointmentAPIV1.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment,Long> {
    List<Appointment> id(Long id);
    List<Appointment> findByDoctor(String doctor);

}
