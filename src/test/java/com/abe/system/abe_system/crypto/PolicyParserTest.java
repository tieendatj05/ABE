package com.abe.system.abe_system.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyParserTest {

    @Test
    void singleLeafParsesToLeafNode() {
        PolicyNode node = PolicyParser.parse("role:ADMIN");

        assertThat(node).isInstanceOf(PolicyNode.Leaf.class);
        assertThat(((PolicyNode.Leaf) node).attributeName()).isEqualTo("role:ADMIN");
    }

    @Test
    void andBindsTighterThanOrWithoutParentheses() {
        // "a AND b OR c" phai hieu la (a AND b) OR c, khong phai a AND (b OR c).
        PolicyNode node = PolicyParser.parse("a AND b OR c");

        assertThat(node).isInstanceOf(PolicyNode.Or.class);
        PolicyNode.Or or = (PolicyNode.Or) node;
        assertThat(or.children()).hasSize(2);
        assertThat(or.children().get(0)).isInstanceOf(PolicyNode.And.class);
        assertThat(or.children().get(1)).isEqualTo(new PolicyNode.Leaf("c"));
    }

    @Test
    void parenthesesOverridePrecedence() {
        PolicyNode node = PolicyParser.parse("(department:CNTT AND position:giang_vien) OR role:ADMIN");

        assertThat(node).isInstanceOf(PolicyNode.Or.class);
        PolicyNode.Or or = (PolicyNode.Or) node;
        assertThat(or.children()).hasSize(2);

        PolicyNode.And and = (PolicyNode.And) or.children().get(0);
        assertThat(and.children()).containsExactly(
                new PolicyNode.Leaf("department:CNTT"),
                new PolicyNode.Leaf("position:giang_vien"));
    }

    @Test
    void consecutiveSameOperatorsAreFlattenedIntoOneNode() {
        PolicyNode node = PolicyParser.parse("a AND b AND c");

        assertThat(node).isInstanceOf(PolicyNode.And.class);
        assertThat(((PolicyNode.And) node).children()).hasSize(3);
    }

    @Test
    void blankPolicyIsRejected() {
        assertThatThrownBy(() -> PolicyParser.parse("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unbalancedParenthesesAreRejected() {
        assertThatThrownBy(() -> PolicyParser.parse("(a AND b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void danglingOperatorIsRejected() {
        assertThatThrownBy(() -> PolicyParser.parse("a AND"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
