package com.trazza.iam.infrastructure.persistence.repositories;

import com.trazza.iam.domain.model.aggregates.*;
import com.trazza.iam.domain.repositories.*;
import com.trazza.shared.domain.model.valueobjects.GeoLocation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class IamRepositories {

    @Repository
    public static class UserRepositoryImpl implements UserRepository {
        @PersistenceContext
        private EntityManager context;

        @Override
        public Optional<User> findByEmail(String email) {
            return context.createQuery("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst();
        }

        @Override
        public Optional<User> findByPublicId(UUID publicId) {
            return context.createQuery("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.publicId = :publicId", User.class)
                    .setParameter("publicId", publicId)
                    .getResultStream()
                    .findFirst();
        }

        @Override
        public Optional<User> findWithRoles(Long id) {
            return context.createQuery("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id", User.class)
                    .setParameter("id", id)
                    .getResultStream()
                    .findFirst();
        }

        @Override
        public boolean existsByEmail(String email) {
            Long count = context.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return count > 0;
        }
    }

    @Repository
    public static class PersonIdentityRepositoryImpl implements PersonIdentityRepository {
        @PersistenceContext
        private EntityManager context;

        @Override
        public Optional<PersonIdentity> findByUserId(Long userId) {
            return context.createQuery("SELECT p FROM PersonIdentity p WHERE p.userId = :userId", PersonIdentity.class)
                    .setParameter("userId", userId)
                    .getResultStream()
                    .findFirst();
        }

        @Override
        public boolean existsByDniHash(byte[] dniHash) {
            Long count = context.createQuery("SELECT COUNT(p) FROM PersonIdentity p WHERE p.dniHash = :dniHash", Long.class)
                    .setParameter("dniHash", dniHash)
                    .getSingleResult();
            return count > 0;
        }
    }

    @Repository
    public static class IdentityVerificationRepositoryImpl implements IdentityVerificationRepository {
        @PersistenceContext
        private EntityManager context;

        @Override
        public List<IdentityVerification> listByUser(Long userId) {
            return context.createQuery("SELECT i FROM IdentityVerification i WHERE i.userId = :userId ORDER BY i.submittedAt DESC", IdentityVerification.class)
                    .setParameter("userId", userId)
                    .getResultList();
        }
    }

    @Repository
    public static class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
        @PersistenceContext
        private EntityManager context;

        @Override
        public Optional<RefreshToken> findByHash(byte[] tokenHash) {
            return context.createQuery("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash", RefreshToken.class)
                    .setParameter("tokenHash", tokenHash)
                    .getResultStream()
                    .findFirst();
        }
    }

    @Repository
    public static class CarrierProfileRepositoryImpl implements CarrierProfileRepository {
        @PersistenceContext
        private EntityManager context;

        @Override
        public Optional<CarrierProfile> findByUserId(Long userId) {
            return context.createQuery("SELECT c FROM CarrierProfile c WHERE c.userId = :userId", CarrierProfile.class)
                    .setParameter("userId", userId)
                    .getResultStream()
                    .findFirst();
        }
    }

    @Repository
    public static class MerchantProfileRepositoryImpl implements MerchantProfileRepository {
        @PersistenceContext
        private EntityManager context;

        @Override
        public Optional<MerchantProfile> findByUserId(Long userId) {
            return context.createQuery("SELECT m FROM MerchantProfile m WHERE m.userId = :userId", MerchantProfile.class)
                    .setParameter("userId", userId)
                    .getResultStream()
                    .findFirst();
        }

        @Override
        public boolean existsByRuc(String ruc) {
            Long count = context.createQuery("SELECT COUNT(m) FROM MerchantProfile m WHERE m.ruc = :ruc", Long.class)
                    .setParameter("ruc", ruc)
                    .getSingleResult();
            return count > 0;
        }
    }

    @Repository
    public static class DriverRepositoryImpl implements DriverRepository {
        @PersistenceContext
        private EntityManager context;

        @Override
        public List<Driver> listByCarrier(Long carrierProfileId) {
            return context.createQuery("SELECT d FROM Driver d WHERE d.carrierProfileId = :carrierProfileId ORDER BY d.fullName", Driver.class)
                    .setParameter("carrierProfileId", carrierProfileId)
                    .getResultList();
        }
    }

    @Repository
    public static class VehicleRepositoryImpl implements VehicleRepository {
        @PersistenceContext
        private EntityManager context;

        @Override
        public List<Vehicle> listByCarrier(Long carrierProfileId) {
            return context.createQuery("SELECT v FROM Vehicle v WHERE v.carrierProfileId = :carrierProfileId ORDER BY v.plate", Vehicle.class)
                    .setParameter("carrierProfileId", carrierProfileId)
                    .getResultList();
        }

        @Override
        public List<Vehicle> listByIds(List<Long> ids) {
            return context.createQuery("SELECT v FROM Vehicle v WHERE v.id IN :ids", Vehicle.class)
                    .setParameter("ids", ids)
                    .getResultList();
        }

        @Override
        public Optional<Vehicle> findByPublicId(UUID publicId) {
            return context.createQuery("SELECT v FROM Vehicle v WHERE v.publicId = :publicId", Vehicle.class)
                    .setParameter("publicId", publicId)
                    .getResultStream()
                    .findFirst();
        }

        @Override
        public boolean existsByPlate(String plate, Long exceptVehicleId) {
            String queryString = "SELECT COUNT(v) FROM Vehicle v WHERE v.plate = :plate";
            if (exceptVehicleId != null) {
                queryString += " AND v.id != :exceptVehicleId";
            }

            Query query = context.createQuery(queryString, Long.class)
                    .setParameter("plate", plate);

            if (exceptVehicleId != null) {
                query.setParameter("exceptVehicleId", exceptVehicleId);
            }

            Long count = (Long) query.getSingleResult();
            return count > 0;
        }
    }

    /**
     * SQL repository: saved_addresses.location is POINT SRID 4326, written with
     * ST_PointFromText and read with ST_Latitude/ST_Longitude.
     */
    @Repository
    public static class SavedAddressRepositoryImpl implements SavedAddressRepository {
        @PersistenceContext
        private EntityManager context;

        @Override
        @Transactional
        public SavedAddress add(SavedAddress address) {
            String wkt = new GeoLocation(address.latitude(), address.longitude()).toWkt();

            // INSERT con SQL Nativo
            Query insertQuery = context.createNativeQuery(
                    "INSERT INTO saved_addresses (user_id, label, address, district, location, created_at) " +
                            "VALUES (?1, ?2, ?3, ?4, ST_PointFromText(?5, 4326, 'axis-order=lat-long'), ?6)");
            insertQuery.setParameter(1, address.userId());
            insertQuery.setParameter(2, address.label());
            insertQuery.setParameter(3, address.address());
            insertQuery.setParameter(4, address.district());
            insertQuery.setParameter(5, wkt);
            insertQuery.setParameter(6, address.createdAt());
            insertQuery.executeUpdate();

            // Obtener el ID generado (LAST_INSERT_ID)
            Long generatedId = ((Number) context.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();

            // Retornar el record con el nuevo ID
            return new SavedAddress(generatedId, address.userId(), address.label(), address.address(),
                    address.district(), address.latitude(), address.longitude(), address.createdAt());
        }

        @Override
        public List<SavedAddress> listByUser(Long userId) {
            List<Object[]> rows = context.createNativeQuery(
                            "SELECT id, user_id, label, address, district, ST_Latitude(location), ST_Longitude(location), created_at " +
                                    "FROM saved_addresses WHERE user_id = ?1 ORDER BY label")
                    .setParameter(1, userId)
                    .getResultList();

            return rows.stream().map(row -> new SavedAddress(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).longValue(),
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    ((Number) row[5]).doubleValue(),
                    ((Number) row[6]).doubleValue(),
                    ((java.sql.Timestamp) row[7]).toLocalDateTime()
            )).toList();
        }

        @Override
        @Transactional
        public boolean delete(Long id, Long userId) {
            int affected = context.createNativeQuery(
                            "DELETE FROM saved_addresses WHERE id = ?1 AND user_id = ?2")
                    .setParameter(1, id)
                    .setParameter(2, userId)
                    .executeUpdate();
            return affected > 0;
        }
    }
}