package com.bankguard.riskengine.repository;

import com.bankguard.riskengine.entity.RiskRuleConfigEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRuleConfigRepository extends JpaRepository<RiskRuleConfigEntity, Long> {
    List<RiskRuleConfigEntity> findByActiveTrue();
}
