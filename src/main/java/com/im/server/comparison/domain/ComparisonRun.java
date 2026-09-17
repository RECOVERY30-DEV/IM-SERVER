package com.im.server.comparison.domain;

import com.im.server.comparison.api.ComparisonRunStatus;
import com.im.server.comparison.api.OverallStatus;
import com.im.server.comparison.api.UncertainReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** V1·V2 비교 Job. FR04~FR09 파이프라인의 진행 상태와 결과를 갖는다. */
@Entity
@Table(name = "comparison_runs")
@Getter
@Setter
@NoArgsConstructor
public class ComparisonRun {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "application_id", nullable = false)
  private String applicationId;

  @Column(name = "pre_snapshot_id", nullable = false)
  private Long preSnapshotId;

  @Column(name = "post_snapshot_id", nullable = false)
  private Long postSnapshotId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ComparisonRunStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "overall_status", length = 20)
  private OverallStatus overallStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "uncertain_reason", length = 30)
  private UncertainReason uncertainReason;

  @Column(name = "total_steps", nullable = false)
  private int totalSteps;

  @Column(name = "completed_steps", nullable = false)
  private int completedSteps;

  @Column(name = "progress_percent", nullable = false)
  private int progressPercent;

  @Column(name = "prompt_version")
  private String promptVersion;

  @Column(name = "rule_version", nullable = false, length = 32)
  private String ruleVersion;

  @Column(name = "calculation_version", nullable = false, length = 32)
  private String calculationVersion;

  @Column(name = "comparison_hash", length = 64)
  private String comparisonHash;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  public ComparisonRun(
      String applicationId,
      Long preSnapshotId,
      Long postSnapshotId,
      String ruleVersion,
      String calculationVersion) {
    this.applicationId = applicationId;
    this.preSnapshotId = preSnapshotId;
    this.postSnapshotId = postSnapshotId;
    this.ruleVersion = ruleVersion;
    this.calculationVersion = calculationVersion;
    this.status = ComparisonRunStatus.RUNNING;
    this.totalSteps = 0;
    this.completedSteps = 0;
    this.progressPercent = 0;
    this.startedAt = Instant.now();
  }

  public void markStep(int completedSteps, int totalSteps) {
    this.completedSteps = completedSteps;
    this.totalSteps = totalSteps;
    this.progressPercent = totalSteps == 0 ? 0 : (completedSteps * 100) / totalSteps;
  }

  public void complete(OverallStatus overallStatus, String comparisonHash) {
    this.overallStatus = overallStatus;
    this.comparisonHash = comparisonHash;
    this.status = ComparisonRunStatus.COMPLETED;
    this.completedAt = Instant.now();
  }

  public void fail(UncertainReason uncertainReason) {
    this.status = ComparisonRunStatus.FAILED;
    this.overallStatus = OverallStatus.UNCERTAIN;
    this.uncertainReason = uncertainReason;
    this.completedAt = Instant.now();
  }
}
