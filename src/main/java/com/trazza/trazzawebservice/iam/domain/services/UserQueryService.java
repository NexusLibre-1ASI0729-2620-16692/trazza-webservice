package com.trazza.trazzawebservice.iam.domain.services;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.IdentityVerification;

import java.util.List;
import java.util.Optional;

public interface UserQueryService {

    Optional<UserAccount> getAccount(Long userId);

    List<IdentityVerification> listVerifications(Long userId);
}
