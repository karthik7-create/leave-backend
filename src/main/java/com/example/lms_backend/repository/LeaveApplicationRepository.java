package com.example.lms_backend.repository;

import com.example.lms_backend.entity.LeaveApplication;
import com.example.lms_backend.entity.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    Page<LeaveApplication> findByApplicantId(Long applicantId, Pageable pageable);

    Page<LeaveApplication> findByApplicantIdAndStatus(Long applicantId, LeaveStatus status, Pageable pageable);

    Page<LeaveApplication> findByApplicantIdIn(List<Long> applicantIds, Pageable pageable);

    Page<LeaveApplication> findByApplicantIdInAndStatus(List<Long> applicantIds, LeaveStatus status, Pageable pageable);

    Page<LeaveApplication> findByApplicantIdInAndStatus(List<Long> applicantIds, LeaveStatus status, Pageable pageable, @Param("unused") String unused);

    @Query("SELECT la FROM LeaveApplication la WHERE la.applicant.id = :userId " +
            "AND la.status IN :statuses " +
            "AND la.startDate <= :endDate AND la.endDate >= :startDate")
    List<LeaveApplication> findOverlappingLeaves(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveStatus> statuses);

    @Query("SELECT la FROM LeaveApplication la WHERE la.applicant.id IN :applicantIds " +
            "AND la.status = :status")
    Page<LeaveApplication> findByApplicantIdsAndStatus(
            @Param("applicantIds") List<Long> applicantIds,
            @Param("status") LeaveStatus status,
            Pageable pageable);

    @Query("SELECT la FROM LeaveApplication la WHERE la.applicant.id IN :applicantIds " +
            "AND MONTH(la.startDate) = :month AND YEAR(la.startDate) = :year " +
            "AND la.status IN ('APPROVED', 'PENDING')")
    List<LeaveApplication> findTeamCalendar(
            @Param("applicantIds") List<Long> applicantIds,
            @Param("month") int month,
            @Param("year") int year);

    @Query("SELECT la.status, COUNT(la) FROM LeaveApplication la " +
            "WHERE la.applicant.id IN :applicantIds GROUP BY la.status")
    List<Object[]> countByStatusForTeam(@Param("applicantIds") List<Long> applicantIds);

    @Query("SELECT la.leaveType.name, COUNT(la) FROM LeaveApplication la " +
            "WHERE la.applicant.id IN :applicantIds AND la.status = 'APPROVED' " +
            "GROUP BY la.leaveType.name")
    List<Object[]> countApprovedByTypeForTeam(@Param("applicantIds") List<Long> applicantIds);

    @Query("SELECT FUNCTION('DATE_FORMAT', la.startDate, '%Y-%m'), COUNT(la) FROM LeaveApplication la " +
            "WHERE la.applicant.id IN :applicantIds AND la.status = 'APPROVED' " +
            "GROUP BY FUNCTION('DATE_FORMAT', la.startDate, '%Y-%m') " +
            "ORDER BY FUNCTION('DATE_FORMAT', la.startDate, '%Y-%m')")
    List<Object[]> countApprovedByMonthForTeam(@Param("applicantIds") List<Long> applicantIds);
}
