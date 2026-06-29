package io.sertaoBit.odontocore.crm.modules.funnel.provider;

import io.sertaoBit.odontocore.crm.exception.ResourceNotFoundException;
import io.sertaoBit.odontocore.crm.modules.funnel.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerProviderImpl implements CustomerProvider {

    private final CustomerRepository customerRepository;

    public CustomerProviderImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerView resolveById(UUID id) {

        return customerRepository.findById(id)
                .map(c -> new CustomerView(c.getId(), c.getName()))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found by id: " + id));

    }
}
