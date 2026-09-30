// Đánh giá (ở client) xem 1 tập attribute có thoả 1 access policy hay không.
// Cùng văn phạm với backend (crypto/PolicyParser.java): AND chặt hơn OR, dấu
// ngoặc đơn ghi đè precedence. Chỉ dùng để gợi ý UI (biết trước có nên bấm tải
// hay không) - quyết định thật vẫn do backend kiểm tra khi ghép lại khoá AES
// bằng Shamir Secret Sharing lúc download.
const TOKEN_PATTERN = /\(|\)|AND|OR|[A-Za-z0-9_:.-]+/g;

function tokenize(policy) {
  return policy.match(TOKEN_PATTERN) || [];
}

/**
 * @param {string} policy vd "(department:CNTT AND position:giang_vien) OR role:ADMIN"
 * @param {Set<string>} ownedAttributes tập attribute (còn hiệu lực) đang có
 * @returns {boolean|null} true/false nếu đánh giá được, null nếu policy không hợp lệ
 */
export function evaluatePolicy(policy, ownedAttributes) {
  const tokens = tokenize(policy || '');
  if (tokens.length === 0) return null;

  let pos = 0;
  const peek = () => tokens[pos];

  function parseOr() {
    let result = parseAnd();
    while (peek() === 'OR') {
      pos++;
      const rhs = parseAnd();
      result = result || rhs;
    }
    return result;
  }

  function parseAnd() {
    let result = parsePrimary();
    while (peek() === 'AND') {
      pos++;
      const rhs = parsePrimary();
      result = result && rhs;
    }
    return result;
  }

  function parsePrimary() {
    const token = peek();
    if (token === undefined) {
      throw new Error('thiếu toán hạng');
    }
    if (token === '(') {
      pos++;
      const inner = parseOr();
      if (peek() !== ')') throw new Error("thiếu dấu ')'");
      pos++;
      return inner;
    }
    if (token === 'AND' || token === 'OR' || token === ')') {
      throw new Error('token không hợp lệ: ' + token);
    }
    pos++;
    return ownedAttributes.has(token);
  }

  try {
    const result = parseOr();
    if (pos !== tokens.length) return null; // dư token -> policy không hợp lệ
    return result;
  } catch {
    return null;
  }
}
