import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { translations } from './translations';

const LanguageContext = createContext(null);
const STORAGE_KEY = 'abe_lang';

function readStoredLang() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored === 'en' ? 'en' : 'vi';
  } catch {
    return 'vi';
  }
}

function getNested(obj, path) {
  return path.split('.').reduce((acc, key) => (acc == null ? undefined : acc[key]), obj);
}

function interpolate(str, vars) {
  if (!vars) return str;
  return str.replace(/\{\{(\w+)\}\}/g, (match, key) => (vars[key] !== undefined ? vars[key] : match));
}

export function LanguageProvider({ children }) {
  const [lang, setLangState] = useState(readStoredLang);

  const setLang = useCallback((next) => {
    setLangState(next);
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // localStorage có thể bị chặn (private mode/quota) - chỉ mất khả năng ghi nhớ lựa chọn, không chặn app.
    }
  }, []);

  const toggleLang = useCallback(() => {
    setLang(lang === 'vi' ? 'en' : 'vi');
  }, [lang, setLang]);

  const t = useCallback(
    (key, vars) => {
      const value = getNested(translations[lang], key) ?? getNested(translations.vi, key) ?? key;
      return typeof value === 'string' ? interpolate(value, vars) : value;
    },
    [lang]
  );

  const value = useMemo(() => ({ lang, setLang, toggleLang, t }), [lang, setLang, toggleLang, t]);

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
}

export function useLanguage() {
  const ctx = useContext(LanguageContext);
  if (!ctx) {
    throw new Error('useLanguage phải được dùng bên trong LanguageProvider');
  }
  return ctx;
}
