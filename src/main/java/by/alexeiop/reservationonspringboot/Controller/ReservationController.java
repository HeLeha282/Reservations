package by.alexeiop.reservationonspringboot.Controller;

import by.alexeiop.reservationonspringboot.Repository.Reservation;
import by.alexeiop.reservationonspringboot.Service.ReservationService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/reservations")
public class  ReservationController {

    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<Reservation>> getReservations() {
        log.info("log called method getReservations");
        return ResponseEntity.ok(reservationService.getReservations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationsById(@PathVariable("id") long id) {
        log.info("called method getReservationById: id={}", id);
        try {
            return ResponseEntity.ok(reservationService.getReservationById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }


    }

    @PostMapping
    public ResponseEntity<Reservation> createReservation(@RequestBody Reservation reservationToCreate) {
        log.info("called method createReservation");
        try{
            Reservation reservation = reservationService.createReservation(reservationToCreate);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("test-header", "123")
                    .body(reservation);
        }
        catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

    }

    @PutMapping("/{id}")
    public ResponseEntity<Reservation> updateReservation(@PathVariable long id,
                                                         @RequestBody Reservation reservationToUpdate) {
        log.info("called method updateReservation: id={}, reservationToUpdate={}", id, reservationToUpdate);
        try {
            Reservation updatedReservation = reservationService.updateReservation(id, reservationToUpdate);
            return ResponseEntity.ok(updatedReservation);
        }catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }catch (EntityNotFoundException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Reservation> deleteReservation(@PathVariable Long id) {
        log.info("called method deleteReservation: id={}", id);
        try {
            reservationService.deleteReservation(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Reservation> approveReservation(@PathVariable long id){
        log.info("called method approveReservation: id={}", id);
        Reservation reservation = reservationService.approveReservation(id);
        return ResponseEntity.ok(reservation);
    }

}
