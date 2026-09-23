package com.abe.system.abe_system.crypto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser cho biểu thức access policy dạng AND/OR trên attribute, ví dụ:
 * "(department:CNTT AND position:giang_vien) OR role:ADMIN".
 *
 * Precedence: AND chặt hơn OR (giống && / || trong lập trình), dấu ngoặc đơn
 * ghi đè precedence. Văn phạm (đệ quy xuống dần - recursive descent):
 *   orExpr  := andExpr (OR andExpr)*
 *   andExpr := primary (AND primary)*
 *   primary := LEAF | '(' orExpr ')'
 *
 * Các operator liên tiếp CÙNG loại được gộp vào 1 node n-ary (vd "a AND b AND c"
 * -> And[a,b,c] thay vì And[And[a,b],c]) để cây chia khoá tối giản, đúng ngữ
 * nghĩa "cần đủ cả 3" thay vì lồng 2 tầng threshold không cần thiết.
 */
public final class PolicyParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\(|\\)|AND|OR|[A-Za-z0-9_:.\\-]+");

    private final List<String> tokens;
    private int pos;

    private PolicyParser(List<String> tokens) {
        this.tokens = tokens;
    }

    public static PolicyNode parse(String policy) {
        if (policy == null || policy.isBlank()) {
            throw new IllegalArgumentException("Access policy không được để trống");
        }
        List<String> tokens = tokenize(policy);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Access policy không hợp lệ: " + policy);
        }

        PolicyParser parser = new PolicyParser(tokens);
        PolicyNode root = parser.parseOr();
        if (parser.pos != tokens.size()) {
            throw new IllegalArgumentException(
                    "Access policy không hợp lệ (dư token gần vị trí " + parser.pos + "): " + policy);
        }
        return root;
    }

    private static List<String> tokenize(String policy) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(policy);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private PolicyNode parseOr() {
        List<PolicyNode> children = new ArrayList<>();
        children.add(parseAnd());
        while ("OR".equals(peek())) {
            pos++;
            children.add(parseAnd());
        }
        return children.size() == 1 ? children.get(0) : new PolicyNode.Or(children);
    }

    private PolicyNode parseAnd() {
        List<PolicyNode> children = new ArrayList<>();
        children.add(parsePrimary());
        while ("AND".equals(peek())) {
            pos++;
            children.add(parsePrimary());
        }
        return children.size() == 1 ? children.get(0) : new PolicyNode.And(children);
    }

    private PolicyNode parsePrimary() {
        String token = peek();
        if (token == null) {
            throw new IllegalArgumentException("Access policy không hợp lệ: thiếu toán hạng");
        }
        if ("(".equals(token)) {
            pos++;
            PolicyNode inner = parseOr();
            if (!")".equals(peek())) {
                throw new IllegalArgumentException("Access policy không hợp lệ: thiếu dấu ')'");
            }
            pos++;
            return inner;
        }
        if ("AND".equals(token) || "OR".equals(token) || ")".equals(token)) {
            throw new IllegalArgumentException("Access policy không hợp lệ gần: " + token);
        }
        pos++;
        return new PolicyNode.Leaf(token);
    }

    private String peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }
}
