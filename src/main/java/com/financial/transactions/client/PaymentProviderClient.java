package com.financial.transactions.repository;

import com.financial.transactions.dto.ProviderRequest;
import com.financial.transactions.dto.ProviderResult;

public interface PaymentProviderClient {
    ProviderResult execute(ProviderRequest request);
}
