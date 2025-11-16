package by.alexeiop.reservationonspringboot.Service;

import by.alexeiop.reservationonspringboot.Repository.Reservation;
import by.alexeiop.reservationonspringboot.Repository.ReservationEntity;
import by.alexeiop.reservationonspringboot.Repository.ReservationRepository;
import by.alexeiop.reservationonspringboot.Repository.ReservationStatus;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    private boolean isCorrectDateReservation(Reservation reservation) {
        if (reservation.startDate().isBefore(LocalDate.now())) {
            return false;
        }
        if (reservation.startDate().isAfter(reservation.endDate())) {
            return false;
        }
        return true;
    }
    Reservation mapReservationEntityToDomainReservation(ReservationEntity reservationEntity) {
        return new  Reservation(
                reservationEntity.getId(),
                reservationEntity.getUserId(),
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate(),
                reservationEntity.getStatus()
        );
    }

    public List<Reservation> getReservations() {
        List<ReservationEntity> reservationEntityList = reservationRepository.findAll();
        List<Reservation> reservations = reservationEntityList.stream().map(it -> {
            return mapReservationEntityToDomainReservation(it);
        }).toList();
        return reservations;
    }

    public Reservation getReservationById(long id) {
        Optional<ReservationEntity> findReservationEntity = reservationRepository.findById(id);

        if (findReservationEntity.isPresent()) {
            ReservationEntity reservationEntity = findReservationEntity.get();
            return mapReservationEntityToDomainReservation(reservationEntity);
        }
        throw new EntityNotFoundException("Not found reservation by id" + id);
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if (reservationToCreate.id() != null) {
            throw new IllegalArgumentException("id should be null");
        }
        if (reservationToCreate.status() != null) {
            throw new IllegalArgumentException("status should be null");
        }
        if (!isCorrectDateReservation(reservationToCreate)) {
            throw new IllegalArgumentException("uncorrected date reservation");
        }
        boolean isConflict = isReservationConflict(reservationToCreate);
        if (isConflict) {
            throw new IllegalStateException("cannot modify reservation because of conflict");
        }
        ReservationEntity entityToSave = new ReservationEntity(
                null,
                reservationToCreate.userId(),
                reservationToCreate.roomId(),
                reservationToCreate.startDate(),
                reservationToCreate.endDate(),
                ReservationStatus.PENDING
        );
        var savedEntity = reservationRepository.save(entityToSave);
        return mapReservationEntityToDomainReservation(savedEntity);
    }

    public Reservation updateReservation(long id, Reservation reservationToUpdate) {
        Optional<ReservationEntity> findReservationEntity = reservationRepository.findById(id);
        //можно сделать таким образом
        //ReservationEntity reservationEntity = reservationRepository.findById(id)
        //        .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id" + id));

        if (findReservationEntity.isEmpty()) {
            throw new EntityNotFoundException("not found reservation with id " + id);
        }

        ReservationEntity reservationEntity = findReservationEntity.get();
        Reservation reservation =  mapReservationEntityToDomainReservation(reservationEntity);
        if (reservation.status() != ReservationStatus.PENDING) {
            throw new IllegalStateException("cannot modify reservation with status " + reservation.status());
        }
        if (!isCorrectDateReservation(reservationToUpdate)) {
            throw new IllegalArgumentException("uncorrected date reservation");
        }
        boolean isConflict = isReservationConflict(reservationToUpdate);
        if (isConflict) {
            throw new IllegalStateException("cannot modify reservation because of conflict");
        }
        ReservationEntity reservationToUpdated = new ReservationEntity(
                reservation.id(),
                reservationToUpdate.userId(),
                reservationToUpdate.roomId(),
                reservationToUpdate.startDate(),
                reservationToUpdate.endDate(),
                ReservationStatus.PENDING
        );
        ReservationEntity updatedReservation = reservationRepository.save(reservationToUpdated);
        return mapReservationEntityToDomainReservation(updatedReservation);
    }

    public void deleteReservation(Long id) {

        ReservationEntity reservationEntity = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id" + id));

        Reservation reservation = mapReservationEntityToDomainReservation(reservationEntity);
        ReservationEntity reservationToDelete = new ReservationEntity(
                reservation.id(),
                reservation.userId(),
                reservation.roomId(),
                reservation.startDate(),
                reservation.endDate(),
                ReservationStatus.CANCELLED
        );
        reservationRepository.save(reservationToDelete);
    }

    public Reservation approveReservation(long id) {
        ReservationEntity reservationEntity = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Not found reservation by id" + id));

        Reservation reservation = mapReservationEntityToDomainReservation(reservationEntity);
        if (reservation.status() != ReservationStatus.PENDING) {
            throw new IllegalStateException("cannot modify reservation with status " + reservation.status());
        }
        boolean isConflict = isReservationConflict(reservation);
        if (isConflict) {
            throw new IllegalStateException("cannot modify reservation because of conflict");
        }
        ReservationEntity approvedReservation = new ReservationEntity(
                reservation.id(),
                reservation.userId(),
                reservation.roomId(),
                reservation.startDate(),
                reservation.endDate(),
                ReservationStatus.APPROVED
        );

        reservationRepository.save(approvedReservation);

        for (ReservationEntity reservationEntityToCanceled : reservationRepository.findAll()) {
            Reservation reservationToCanceled = mapReservationEntityToDomainReservation(reservationEntityToCanceled);
            if (!reservationToCanceled.id().equals(id)) {
                if (
                        reservationToCanceled.startDate()
                        .isBefore( mapReservationEntityToDomainReservation(approvedReservation).endDate())
                        &&
                        reservationToCanceled.endDate()
                        .isAfter(mapReservationEntityToDomainReservation(approvedReservation).startDate())
                ) {
                    ReservationEntity newReservationToCanceled = new ReservationEntity(
                            reservationToCanceled.id(),
                            reservationToCanceled.userId(),
                            reservationToCanceled.roomId(),
                            reservationToCanceled.startDate(),
                            reservationToCanceled.endDate(),
                            ReservationStatus.CANCELLED
                    );
                    reservationRepository.save(newReservationToCanceled);
                }
            }
        }
        return mapReservationEntityToDomainReservation(approvedReservation);
    }

    private boolean isReservationConflict(Reservation reservation) {
        for (ReservationEntity existReservationEntity : reservationRepository.findAll()) {
            Reservation existReservation = mapReservationEntityToDomainReservation(existReservationEntity);
            if (existReservation.roomId().equals(reservation.roomId())) {
//                if (!existReservation.userId().equals(reservation.userId())) {
                if (existReservation.status().equals(ReservationStatus.APPROVED)) {
                    if (reservation.startDate().isBefore(existReservation.endDate()) && reservation.endDate().isAfter(existReservation.startDate())) {
                        return true;
                    }
                }
//                }
            }
        }
        return false;
    }

}
