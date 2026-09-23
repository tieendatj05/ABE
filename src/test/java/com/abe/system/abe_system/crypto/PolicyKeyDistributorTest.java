package com.abe.system.abe_system.crypto;

import com.abe.system.abe_system.crypto.PolicyKeyDistributor.LeafShare;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyKeyDistributorTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    void orPolicyIsSatisfiedByAnySingleBranch() {
        PolicyNode policy = PolicyParser.parse("role:ADMIN OR department:CNTT");
        BigInteger secret = randomSecret();
        List<LeafShare> shares = PolicyKeyDistributor.distribute(policy, secret);

        Optional<BigInteger> withAdmin = PolicyKeyDistributor.reconstruct(policy, Set.of("role:ADMIN"), shares);
        Optional<BigInteger> withDept = PolicyKeyDistributor.reconstruct(policy, Set.of("department:CNTT"), shares);
        Optional<BigInteger> withNeither = PolicyKeyDistributor.reconstruct(policy, Set.of("department:KT"), shares);

        assertThat(withAdmin).contains(secret);
        assertThat(withDept).contains(secret);
        assertThat(withNeither).isEmpty();
    }

    @Test
    void andPolicyRequiresEveryAttribute() {
        PolicyNode policy = PolicyParser.parse("department:CNTT AND position:giang_vien");
        BigInteger secret = randomSecret();
        List<LeafShare> shares = PolicyKeyDistributor.distribute(policy, secret);

        Optional<BigInteger> withBoth = PolicyKeyDistributor.reconstruct(
                policy, Set.of("department:CNTT", "position:giang_vien"), shares);
        Optional<BigInteger> withOnlyOne = PolicyKeyDistributor.reconstruct(
                policy, Set.of("department:CNTT"), shares);

        assertThat(withBoth).contains(secret);
        assertThat(withOnlyOne).isEmpty();
    }

    @Test
    void nestedAndOrPolicyMatchesExpectedAccessStructure() {
        PolicyNode policy = PolicyParser.parse("(department:CNTT AND position:giang_vien) OR role:ADMIN");
        BigInteger secret = randomSecret();
        List<LeafShare> shares = PolicyKeyDistributor.distribute(policy, secret);

        assertThat(PolicyKeyDistributor.reconstruct(policy, Set.of("role:ADMIN"), shares)).contains(secret);
        assertThat(PolicyKeyDistributor.reconstruct(
                policy, Set.of("department:CNTT", "position:giang_vien"), shares)).contains(secret);
        assertThat(PolicyKeyDistributor.reconstruct(policy, Set.of("department:CNTT"), shares)).isEmpty();
        assertThat(PolicyKeyDistributor.reconstruct(policy, Set.of(), shares)).isEmpty();
    }

    @Test
    void revokedAttributeIsTreatedAsNotOwned() {
        // Mô phỏng "attribute bị revoke": chỉ đơn giản là không đưa attribute đó
        // vào tập userActiveAttributes truyền cho reconstruct (FileService lấy tập
        // này từ findByUserAndRevokedFalse).
        PolicyNode policy = PolicyParser.parse("role:ADMIN");
        BigInteger secret = randomSecret();
        List<LeafShare> shares = PolicyKeyDistributor.distribute(policy, secret);

        Optional<BigInteger> afterRevoke = PolicyKeyDistributor.reconstruct(policy, Set.of(), shares);

        assertThat(afterRevoke).isEmpty();
    }

    private BigInteger randomSecret() {
        BigInteger secret;
        do {
            secret = new BigInteger(256, RANDOM);
        } while (secret.compareTo(ShamirSecretSharing.P) >= 0);
        return secret;
    }
}
