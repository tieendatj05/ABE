package com.abe.system.abe_system.crypto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Chia khoá AES (secret) xuống các lá (attribute) của cây chính sách AND/OR,
 * và ghép ngược lại khi user có đủ attribute thoả policy - đây chính là
 * "Shamir's construction cho general access structure" (Shamir 1979):
 *   - OR  : nhân bản secret cho MỌI nhánh con -> chỉ cần 1 nhánh bất kỳ thoả.
 *   - AND : chia secret bằng Shamir threshold = số nhánh con rồi đệ quy share
 *           xuống từng nhánh -> cần ĐỦ CẢ tất cả nhánh con mới ghép lại được.
 *   - Leaf: giá trị (secret/share) nhận từ node cha CHÍNH LÀ giá trị gắn với
 *           attribute đó, lưu lại để sau này user sở hữu attribute dùng.
 *
 * Cây chính sách được parse lại (bởi {@link PolicyParser}) y hệt cả lúc
 * distribute (upload) lẫn lúc reconstruct (download) từ cùng 1 chuỗi
 * accessPolicy lưu trong DB, nên thứ tự duyệt children (và do đó x-coordinate
 * dùng cho Shamir ở mỗi node AND) luôn nhất quán giữa 2 lần - không cần lưu
 * thêm thông tin nào khác ngoài danh sách (attributeName, share).
 */
public final class PolicyKeyDistributor {

    private PolicyKeyDistributor() {
    }

    public record LeafShare(String attributeName, BigInteger value) {
    }

    public static List<LeafShare> distribute(PolicyNode root, BigInteger secret) {
        List<LeafShare> leaves = new ArrayList<>();
        distributeInto(root, secret, leaves);
        return leaves;
    }

    private static void distributeInto(PolicyNode node, BigInteger secret, List<LeafShare> out) {
        switch (node) {
            case PolicyNode.Leaf leaf -> out.add(new LeafShare(leaf.attributeName(), secret));
            case PolicyNode.Or or -> {
                for (PolicyNode child : or.children()) {
                    distributeInto(child, secret, out);
                }
            }
            case PolicyNode.And and -> {
                List<ShamirSecretSharing.Share> shares = ShamirSecretSharing.split(secret, and.children().size());
                for (int i = 0; i < and.children().size(); i++) {
                    distributeInto(and.children().get(i), shares.get(i).y(), out);
                }
            }
        }
    }

    /**
     * Thử ghép lại secret (khoá AES) gốc từ tập attribute còn hiệu lực
     * (revoked=false) mà user đang sở hữu. Trả về empty nếu không thoả policy.
     */
    public static Optional<BigInteger> reconstruct(
            PolicyNode root, Set<String> userActiveAttributes, List<LeafShare> leafShares) {
        return reconstructNode(root, userActiveAttributes, leafShares);
    }

    private static Optional<BigInteger> reconstructNode(
            PolicyNode node, Set<String> userActiveAttributes, List<LeafShare> leafShares) {
        return switch (node) {
            case PolicyNode.Leaf leaf -> {
                if (!userActiveAttributes.contains(leaf.attributeName())) {
                    yield Optional.empty();
                }
                yield leafShares.stream()
                        .filter(ls -> ls.attributeName().equals(leaf.attributeName()))
                        .map(LeafShare::value)
                        .findFirst();
            }
            case PolicyNode.Or or -> {
                Optional<BigInteger> found = Optional.empty();
                for (PolicyNode child : or.children()) {
                    found = reconstructNode(child, userActiveAttributes, leafShares);
                    if (found.isPresent()) {
                        break;
                    }
                }
                yield found;
            }
            case PolicyNode.And and -> {
                List<ShamirSecretSharing.Share> shares = new ArrayList<>();
                boolean allSatisfied = true;
                for (int i = 0; i < and.children().size(); i++) {
                    Optional<BigInteger> childResult = reconstructNode(and.children().get(i), userActiveAttributes, leafShares);
                    if (childResult.isEmpty()) {
                        allSatisfied = false;
                        break;
                    }
                    shares.add(new ShamirSecretSharing.Share(i + 1, childResult.get()));
                }
                yield allSatisfied ? Optional.of(ShamirSecretSharing.combine(shares)) : Optional.empty();
            }
        };
    }
}
