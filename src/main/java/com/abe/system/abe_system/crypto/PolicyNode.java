package com.abe.system.abe_system.crypto;

import java.util.List;

/**
 * Cây biểu diễn 1 access policy dạng AND/OR trên attribute, ví dụ policy
 * "(department:CNTT AND position:giang_vien) OR role:ADMIN" được parse
 * (bởi {@link PolicyParser}) thành:
 * Or[ And[Leaf(department:CNTT), Leaf(position:giang_vien)], Leaf(role:ADMIN) ]
 *
 * Sealed interface: chỉ có đúng 3 loại node, dùng switch pattern-matching
 * (Java 21+) không cần default branch mà vẫn được compiler đảm bảo bao phủ
 * hết case ở PolicyKeyDistributor.
 */
public sealed interface PolicyNode permits PolicyNode.And, PolicyNode.Or, PolicyNode.Leaf {

    record And(List<PolicyNode> children) implements PolicyNode {
    }

    record Or(List<PolicyNode> children) implements PolicyNode {
    }

    record Leaf(String attributeName) implements PolicyNode {
    }
}
