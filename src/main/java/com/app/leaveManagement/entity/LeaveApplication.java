package com.app.leaveManagement.entity;

import com.app.leaveManagement.enums.HalfDayType;
import com.app.leaveManagement.enums.LeaveStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "leaveType", "approvals"})

@Entity
@Table(
    name = "leave_applications",
    indexes = {
        @Index(name = "idx_leave_app_user_id", columnList = "user_id"),
        @Index(name = "idx_leave_app_status", columnList = "status"),
        @Index(name = "idx_leave_app_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_leave_app_user_status", columnList = "user_id, status")
    }
)
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal totalDays;

    @Enumerated(EnumType.STRING)
    private HalfDayType halfDayType;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    @OneToMany(mappedBy = "leaveApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LeaveApproval> approvals = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}