package io.sertaoBit.odontocore.crm.modules.identity.service;

import io.sertaoBit.odontocore.crm.core.enums.*;
import io.sertaoBit.odontocore.crm.modules.identity.domain.model.PermissionRule;
import io.sertaoBit.odontocore.crm.modules.identity.repository.PermissionRuleRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static io.sertaoBit.odontocore.crm.core.enums.Action.*;
import static io.sertaoBit.odontocore.crm.core.enums.PermissionScope.*;
import static io.sertaoBit.odontocore.crm.core.enums.Resource.*;
import static io.sertaoBit.odontocore.crm.core.enums.Role.*;
import static io.sertaoBit.odontocore.crm.core.enums.Sector.*;

@Component
public class PermissionSeeder implements ApplicationRunner {

    private final PermissionRuleRepository permissionRuleRepository;

    public PermissionSeeder(PermissionRuleRepository permissionRuleRepository) {
        this.permissionRuleRepository = permissionRuleRepository;
    }


    private PermissionRule rule(
            Role role, Sector sector,
            Resource resource, Action action,
            PermissionScope scope
    ) {
        return PermissionRule.builder()
                .role(role)
                .sector(sector)
                .resource(resource)
                .scope(scope)
                .action(action)
                .allowed(true)
                .build();
    }


    @Override
    @NullMarked
    public void run(ApplicationArguments args) {
        if (permissionRuleRepository.count() > 0) {
            return;
        }

        List<PermissionRule> rules = new ArrayList<>();

        //ADM_SYSTEM
        rules.add(rule(ADM_SYSTEM, null, USER, CREATE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, USER, READ, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, USER, UPDATE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, USER, DELETE, GLOBAL));

        rules.add(rule(ADM_SYSTEM, null, CUSTOMER, CREATE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, CUSTOMER, READ, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, CUSTOMER, UPDATE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, CUSTOMER, DELETE, GLOBAL));

        rules.add(rule(ADM_SYSTEM, null, TICKET, CREATE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, TICKET, UPDATE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, TICKET, READ, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, TICKET, DELETE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, TICKET, RECYCLE, GLOBAL));

        rules.add(rule(ADM_SYSTEM, null, CONTACT_LOG, CREATE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, CONTACT_LOG, READ, GLOBAL));

        rules.add(rule(ADM_SYSTEM, null, DEAL, CREATE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, DEAL, READ, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, DEAL, UPDATE, GLOBAL));
        rules.add(rule(ADM_SYSTEM, null, DEAL, CLOSE, GLOBAL));

        rules.add(rule(ADM_SYSTEM, null, ANALYTICS, READ, GLOBAL));

        rules.add(rule(ADM_SYSTEM, null, CONFIG, CONFIGURE, GLOBAL));

        //ADM_LEADS
        rules.add(rule(ADM_LEADS, LEADS, CUSTOMER, CREATE, SECTOR));
        rules.add(rule(ADM_LEADS, LEADS, CUSTOMER, READ, SECTOR));
        rules.add(rule(ADM_LEADS, LEADS, CUSTOMER, UPDATE, SECTOR));

        rules.add(rule(ADM_LEADS, LEADS, TICKET, CREATE, SECTOR));
        rules.add(rule(ADM_LEADS, LEADS, TICKET, READ, SECTOR));
        rules.add(rule(ADM_LEADS, LEADS, TICKET, UPDATE, SECTOR));

        rules.add(rule(ADM_LEADS, LEADS, CONTACT_LOG, CREATE, SECTOR));
        rules.add(rule(ADM_LEADS, LEADS, CONTACT_LOG, READ, SECTOR));

        //USER_LEADS
        rules.add(rule(USER_LEADS, LEADS, CUSTOMER, CREATE, OWN));
        rules.add(rule(USER_LEADS, LEADS, CUSTOMER, READ, OWN));
        rules.add(rule(USER_LEADS, LEADS, CUSTOMER, UPDATE, OWN));

        rules.add(rule(USER_LEADS, LEADS, TICKET, CREATE, OWN));
        rules.add(rule(USER_LEADS, LEADS, TICKET, READ, OWN));
        rules.add(rule(USER_LEADS, LEADS, TICKET, UPDATE, OWN));

        rules.add(rule(USER_LEADS, LEADS, CONTACT_LOG, CREATE, OWN));
        rules.add(rule(USER_LEADS, LEADS, CONTACT_LOG, READ, OWN));

        //USER_ATTENDANT
        rules.add(rule(USER_ATTENDANT, ATTENDANT, CUSTOMER, CREATE, OWN));
        rules.add(rule(USER_ATTENDANT, ATTENDANT, CUSTOMER, READ, OWN));
        rules.add(rule(USER_ATTENDANT, ATTENDANT, CUSTOMER, UPDATE, OWN));

        rules.add(rule(USER_ATTENDANT, ATTENDANT, TICKET, CREATE, OWN));
        rules.add(rule(USER_ATTENDANT, ATTENDANT, TICKET, READ, OWN));
        rules.add(rule(USER_ATTENDANT, ATTENDANT, TICKET, UPDATE, OWN));

        rules.add(rule(USER_ATTENDANT, ATTENDANT, CONTACT_LOG, CREATE, OWN));
        rules.add(rule(USER_ATTENDANT, ATTENDANT, CONTACT_LOG, READ, OWN));

        //ADM_EVALUATOR
        rules.add(rule(ADM_EVALUATOR, EVALUATOR, DEAL, CREATE, SECTOR));
        rules.add(rule(ADM_EVALUATOR, EVALUATOR, DEAL, READ, SECTOR));
        rules.add(rule(ADM_EVALUATOR, EVALUATOR, DEAL, UPDATE, SECTOR));

        rules.add(rule(ADM_EVALUATOR, EVALUATOR, TICKET, READ, SECTOR));
        rules.add(rule(ADM_EVALUATOR, EVALUATOR, TICKET, UPDATE, SECTOR));

        rules.add(rule(ADM_EVALUATOR, EVALUATOR, CONTACT_LOG, READ, SECTOR));

        //USER_EVALUATOR
        rules.add(rule(USER_EVALUATOR, EVALUATOR, DEAL, CREATE, OWN));
        rules.add(rule(USER_EVALUATOR, EVALUATOR, DEAL, READ, OWN));
        rules.add(rule(USER_EVALUATOR, EVALUATOR, DEAL, UPDATE, OWN));

        rules.add(rule(USER_EVALUATOR, EVALUATOR, TICKET, READ, OWN));
        rules.add(rule(USER_EVALUATOR, EVALUATOR, TICKET, UPDATE, OWN));

        rules.add(rule(USER_EVALUATOR, EVALUATOR, CONTACT_LOG, READ, GLOBAL));

        //ADM_COMMERCIAL
        rules.add(rule(ADM_COMMERCIAL, COMMERCIAL, DEAL, READ, SECTOR));
        rules.add(rule(ADM_COMMERCIAL, COMMERCIAL, DEAL, UPDATE, SECTOR));
        rules.add(rule(ADM_COMMERCIAL, COMMERCIAL, DEAL, CLOSE, SECTOR));

        rules.add(rule(ADM_COMMERCIAL, COMMERCIAL, TICKET, READ, SECTOR));
        rules.add(rule(ADM_COMMERCIAL, COMMERCIAL, TICKET, UPDATE, SECTOR));
        rules.add(rule(ADM_COMMERCIAL, COMMERCIAL, TICKET, CLOSE, SECTOR));

        rules.add(rule(ADM_COMMERCIAL, COMMERCIAL, CONTACT_LOG, READ, SECTOR));

        //USER_COMMERCIAL
        rules.add(rule(USER_COMMERCIAL, COMMERCIAL, DEAL, READ, OWN));
        rules.add(rule(USER_COMMERCIAL, COMMERCIAL, DEAL, UPDATE, OWN));
        rules.add(rule(USER_COMMERCIAL, COMMERCIAL, DEAL, CLOSE, OWN));

        rules.add(rule(USER_COMMERCIAL, COMMERCIAL, TICKET, READ, OWN));
        rules.add(rule(USER_COMMERCIAL, COMMERCIAL, TICKET, UPDATE, OWN));
        rules.add(rule(USER_COMMERCIAL, COMMERCIAL, TICKET, CLOSE, OWN));

        rules.add(rule(USER_COMMERCIAL, COMMERCIAL, CONTACT_LOG, READ, GLOBAL));

        permissionRuleRepository.saveAll(rules);
    }
}
