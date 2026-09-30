import { useEffect, useMemo, useState } from 'react';
import { apiFetch } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import Banner from '../components/Banner';
import PolicyTreeDiagram from '../components/PolicyTreeDiagram';

// Trang Upload cho phép chọn thuộc tính (checkbox) từ chính các attribute mà
// owner đang có, thay vì bắt buộc tự gõ cú pháp AND/OR - dễ dùng hơn cho
// người không quen kỹ thuật (vd giảng viên). Chế độ Nâng cao vẫn giữ ô nhập
// tay để không giới hạn khả năng viết policy phức tạp (OR nhiều nhánh...).
export default function UploadPage() {
  const { t } = useLanguage();
  const [file, setFile] = useState(null);
  const [myAttributes, setMyAttributes] = useState([]);
  const [attrsLoading, setAttrsLoading] = useState(true);
  const [mode, setMode] = useState('basic');
  const [checkedAttrs, setCheckedAttrs] = useState([]);
  const [advancedPolicy, setAdvancedPolicy] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    apiFetch('/api/attributes/me')
      .then((data) => {
        const active = (data || []).filter((a) => !a.revoked);
        setMyAttributes(active);
        if (active.length === 0) {
          setMode('advanced');
        }
      })
      .catch(() => setMyAttributes([]))
      .finally(() => setAttrsLoading(false));
  }, []);

  const basicPolicy = useMemo(() => checkedAttrs.join(' AND '), [checkedAttrs]);
  const effectivePolicy = mode === 'basic' ? basicPolicy : advancedPolicy;

  function toggleAttr(name) {
    setCheckedAttrs((prev) => (prev.includes(name) ? prev.filter((a) => a !== name) : [...prev, name]));
  }

  function switchToAdvanced() {
    setAdvancedPolicy(basicPolicy);
    setMode('advanced');
  }

  function switchToBasic() {
    setMode('basic');
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!file) {
      setError(t('upload.chooseFileRequired'));
      return;
    }
    if (mode === 'basic' && checkedAttrs.length === 0) {
      setError(t('upload.selectAtLeastOne'));
      return;
    }
    if (!effectivePolicy.trim()) {
      setError(t('upload.policyRequired'));
      return;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('accessPolicy', effectivePolicy);

    setUploading(true);
    try {
      const result = await apiFetch('/api/files/upload', {
        method: 'POST',
        body: formData,
      });
      setSuccess(t('upload.success', { fileName: result?.fileName || file.name }));
      setFile(null);
      setCheckedAttrs([]);
      setAdvancedPolicy('');
      e.target.reset();
    } catch (err) {
      setError(err.message || t('upload.failed'));
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="page page-narrow">
      <h1>{t('upload.title')}</h1>
      <Banner message={error} />
      <Banner type="success" message={success} />
      <form className="card form" onSubmit={handleSubmit}>
        <label>
          {t('upload.chooseFile')}
          <input type="file" onChange={(e) => setFile(e.target.files[0] || null)} required />
        </label>

        {!attrsLoading && myAttributes.length > 0 && (
          <div className="mode-switch">
            <button
              type="button"
              className={mode === 'basic' ? 'mode-btn mode-btn-active' : 'mode-btn'}
              onClick={switchToBasic}
            >
              {t('upload.modeBasic')}
            </button>
            <button
              type="button"
              className={mode === 'advanced' ? 'mode-btn mode-btn-active' : 'mode-btn'}
              onClick={switchToAdvanced}
            >
              {t('upload.modeAdvanced')}
            </button>
          </div>
        )}

        {mode === 'basic' ? (
          <div className="block-label">
            {t('upload.accessPolicy')}
            {myAttributes.length === 0 ? (
              <small className="hint">{t('upload.noAttributesWarning')}</small>
            ) : (
              <>
                <small className="hint">{t('upload.basicHint')}</small>
                <div className="attr-checklist">
                  {myAttributes.map((attr) => (
                    <label key={attr.id} className="attr-checkbox">
                      <input
                        type="checkbox"
                        checked={checkedAttrs.includes(attr.attributeName)}
                        onChange={() => toggleAttr(attr.attributeName)}
                      />
                      <code>{attr.attributeName}</code>
                    </label>
                  ))}
                </div>
                {basicPolicy && (
                  <small className="hint">
                    {t('upload.policyPreviewLabel')} <code>{basicPolicy}</code>
                  </small>
                )}
              </>
            )}
          </div>
        ) : (
          <label>
            {t('upload.accessPolicy')}
            <textarea
              rows={4}
              placeholder={t('upload.policyPlaceholder')}
              value={advancedPolicy}
              onChange={(e) => setAdvancedPolicy(e.target.value)}
              required
            />
            <small className="hint">
              {t('upload.policyHintPrefix')} <code>AND</code>, <code>OR</code> {t('upload.policyHintMiddle')}{' '}
              <code>()</code> {t('upload.policyHintSuffix')} <code>tên:giá_trị</code>.{' '}
              {t('upload.policyHintExampleLabel')} <code>department:CNTT OR role:ADMIN</code>.
            </small>
          </label>
        )}

        {effectivePolicy.trim() && (
          <div className="block-label">
            {t('policyTree.toggleShow')}
            <PolicyTreeDiagram policy={effectivePolicy} />
          </div>
        )}

        <button type="submit" disabled={uploading}>
          {uploading ? t('upload.submitting') : t('upload.submit')}
        </button>
      </form>
    </div>
  );
}
