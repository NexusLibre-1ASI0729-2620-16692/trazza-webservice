package com.trazza.trazzawebservice.iam.domain.services;

import com.trazza.trazzawebservice.iam.domain.model.aggregates.CarrierProfile;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.MerchantProfile;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.PersonIdentity;
import com.trazza.trazzawebservice.iam.domain.model.aggregates.User;

/** Account as seen by its owner: user, decrypted contact data and profiles. */
public record UserAccount(User user, PersonIdentity identity, String phone, CarrierProfile carrierProfile,
                          MerchantProfile merchantProfile) {
}
