package by.alexeiop.reservationonspringboot.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    @Query("""
    SELECT COUNT(r) > 0 FROM ReservationEntity r
    WHERE r.roomId = :roomId
    and r.status = by.alexeiop.reservationonspringboot.Repository.ReservationStatus.APPROVED
    and :startDate < r.startDate
    and :endDate > r.endDate
""")
    boolean existsConflictReservationsForRoomAndDate(@Param("roomId") Long roomId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("""
           UPDATE ReservationEntity r
           SET r.status = by.alexeiop.reservationonspringboot.Repository.ReservationStatus.CANCELLED
           WHERE r.roomId = :roomId
           and r.status != by.alexeiop.reservationonspringboot.Repository.ReservationStatus.APPROVED
           and r.startDate < :endDateApproveReservation
           and r.endDate > :startDateApproveReservation
           """)
    void canceledConflictPendingReservationWithCurrentApproveReservation(@Param("roomId") Long roomId,
                                                                         @Param("endDateApproveReservation") LocalDate endDateApproveReservation,
                                                                         @Param("startDateApproveReservation") LocalDate startDateApproveReservation
    );
}









