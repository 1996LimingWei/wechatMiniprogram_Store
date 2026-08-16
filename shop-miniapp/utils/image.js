const FALLBACK_IMAGE = '/static/images/logo.png';

function isTemporaryImageUrl(url) {
  return /^wxfile:\/\//i.test(url)
    || /^file:\/\//i.test(url)
    || /^http:\/\/tmp\//i.test(url)
    || /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?\/(?!uploads\/material\/)/i.test(url);
}

function normalizeImageUrl(url) {
  if (!url || typeof url !== 'string') return FALLBACK_IMAGE;
  const trimmed = url.trim();
  if (!trimmed || isTemporaryImageUrl(trimmed)) return FALLBACK_IMAGE;
  return trimmed;
}

module.exports = {
  FALLBACK_IMAGE,
  normalizeImageUrl
};
