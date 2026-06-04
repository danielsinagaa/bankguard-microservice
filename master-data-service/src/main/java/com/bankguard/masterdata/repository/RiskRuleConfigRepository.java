package com.bankguard.masterdata.repository;

import com.bankguard.masterdata.entity.RiskRuleConfigEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRuleConfigRepository extends JpaRepository<RiskRuleConfigEntity, Long> {
    Optional<RiskRuleConfigEntity> findByRuleCode(String ruleCode);
}
