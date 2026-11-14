package com.trazza.trazzawebservice.iam.domain.services;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.IdentityVerification;
import com.trazza.trazzawebservice.iam.domain.model.commands.AddRoleCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.RefreshSessionCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SignInCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SignOutCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SignUpCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.SubmitIdentityVerificationCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateIdentityCommand;
import com.trazza.trazzawebservice.iam.domain.model.commands.UpdateMerchantProfileCommand;
import com.trazza.trazzawebservice.shared.domain.model.Result;

public interface UserCommandService {

    Result<AuthenticatedSession> handle(SignUpCommand command);

    Result<AuthenticatedSession> handle(SignInCommand command);

    Result<AuthenticatedSession> handle(RefreshSessionCommand command);

    Result<Boolean> handle(SignOutCommand command);

    Result<UserAccount> handle(UpdateIdentityCommand command);

    Result<UserAccount> handle(AddRoleCommand command);

    Result<UserAccount> handle(UpdateMerchantProfileCommand command);

    Result<IdentityVerification> handle(SubmitIdentityVerificationCommand command);
}
