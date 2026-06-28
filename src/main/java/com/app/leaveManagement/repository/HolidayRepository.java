package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    @Query("SELECT h.date FROM Holiday h WHERE h.date BETWEEN :startDate AND :endDate")
    List<LocalDate> findHolidayDatesBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}