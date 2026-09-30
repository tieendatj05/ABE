import { useMemo } from 'react';
import { parsePolicyTree } from '../utils/policyTree';
import { useLanguage } from '../i18n/LanguageContext';

function TreeNode({ node }) {
  const { t } = useLanguage();
  if (!node) return null;

  if (node.type === 'LEAF') {
    return <code className="ptree-leaf">{node.value}</code>;
  }

  const isAnd = node.type === 'AND';
  return (
    <div className={isAnd ? 'ptree-node ptree-and' : 'ptree-node ptree-or'}>
      <div className="ptree-label">{isAnd ? t('policyTree.andLabel') : t('policyTree.orLabel')}</div>
      <ul className="ptree-children">
        {node.children.map((child, i) => (
          <li key={i}>
            <TreeNode node={child} />
          </li>
        ))}
      </ul>
    </div>
  );
}

// Vẽ trực quan cây AND/OR của 1 access policy - cùng cấu trúc cây mà backend
// (PolicyKeyDistributor) thật sự dùng để chia khoá Shamir, không phải sơ đồ
// minh hoạ suông: node AND thể hiện "cần Shamir threshold = đủ mọi nhánh",
// node OR thể hiện "1 nhánh nào true là đủ".
export default function PolicyTreeDiagram({ policy }) {
  const { t } = useLanguage();
  const tree = useMemo(() => parsePolicyTree(policy), [policy]);

  if (!policy || !policy.trim()) return null;
  if (!tree) {
    return <p className="ptree-error">{t('policyTree.invalid')}</p>;
  }

  return (
    <div className="ptree">
      <TreeNode node={tree} />
    </div>
  );
}
