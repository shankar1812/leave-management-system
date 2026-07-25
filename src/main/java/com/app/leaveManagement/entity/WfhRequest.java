package com.app.leaveManagement.entity;

import com.app.leaveManagement.enums.WfhStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "wfh_requests",
    indexes = {
        @Index(name = "idx_wfh_user_id", columnList = "user_id"),
        @Index(name = "idx_wfh_status",  columnList = "status"),
        @Index(name = "idx_wfh_date",    columnList = "date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "manager"})
public class WfhRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WfhStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(length = 500)
    private String managerRemarks;

    private LocalDateTime actionedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}