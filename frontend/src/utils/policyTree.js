// Parse chuỗi accessPolicy thành cây AND/OR/Leaf để vẽ sơ đồ trực quan.
// Cùng văn phạm với backend (crypto/PolicyParser.java) và với policyEval.js -
// 3 nơi này phải luôn khớp nhau vì cùng mô tả 1 quy tắc duy nhất.
const TOKEN_PATTERN = /\(|\)|AND|OR|[A-Za-z0-9_:.-]+/g;

function tokenize(policy) {
  return policy.match(TOKEN_PATTERN) || [];
}

/**
 * @param {string} policy
 * @returns {{type:'AND'|'OR', children: object[]} | {type:'LEAF', value: string} | null}
 *          null nếu policy rỗng hoặc không hợp lệ cú pháp.
 */
export function parsePolicyTree(policy) {
  const tokens = tokenize(policy || '');
  if (tokens.length === 0) return null;

  let pos = 0;
  const peek = () => tokens[pos];

  function parseOr() {
    const children = [parseAnd()];
    while (peek() === 'OR') {
      pos++;
      children.push(parseAnd());
    }
    return children.length === 1 ? children[0] : { type: 'OR', children };
  }

  function parseAnd() {
    const children = [parsePrimary()];
    while (peek() === 'AND') {
      pos++;
      children.push(parsePrimary());
    }
    return children.length === 1 ? children[0] : { type: 'AND', children };
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
    return { type: 'LEAF', value: token };
  }

  try {
    const tree = parseOr();
    if (pos !== tokens.length) return null;
    return tree;
  } catch {
    return null;
  }
}
