export const visualEditorPermissionGroups = [
  ["content:manage", "content:read"],
  ["product:manage", "product:read"]
];

export type PreviewCenterScene =
  | "home"
  | "product"
  | "banner"
  | "channel"
  | "brand"
  | "topic";

export interface PreviewDraftEnvelope<T = Record<string, any>> {
  scene: PreviewCenterScene;
  updatedAt: string;
  payload: T;
}

const PREVIEW_DRAFT_PREFIX = "shop-admin-preview-center:";
const PREVIEW_COMMIT_KEY = `${PREVIEW_DRAFT_PREFIX}commit-token`;
const PREVIEW_DRAFT_TTL_MS = 15 * 60 * 1000;
const PREVIEW_DRAFT_SCENES: PreviewCenterScene[] = ["product", "banner", "channel", "brand", "topic"];

function canUseStorage() {
  return typeof window !== "undefined" && !!window.localStorage;
}

function buildPreviewDraftKey(scene: PreviewCenterScene) {
  return `${PREVIEW_DRAFT_PREFIX}${scene}`;
}

export function setPreviewDraft<T>(scene: PreviewCenterScene, payload: T) {
  if (!canUseStorage()) return;
  const envelope: PreviewDraftEnvelope<T> = {
    scene,
    updatedAt: new Date().toISOString(),
    payload
  };
  window.localStorage.setItem(buildPreviewDraftKey(scene), JSON.stringify(envelope));
}

export function getPreviewDraft<T>(scene: PreviewCenterScene) {
  if (!canUseStorage()) return null;
  const raw = window.localStorage.getItem(buildPreviewDraftKey(scene));
  if (!raw) return null;
  try {
    const envelope = JSON.parse(raw) as PreviewDraftEnvelope<T>;
    const updatedAt = Date.parse(envelope.updatedAt);
    if (!Number.isFinite(updatedAt) || Date.now() - updatedAt > PREVIEW_DRAFT_TTL_MS) {
      clearPreviewDraft(scene);
      return null;
    }
    return envelope;
  } catch {
    return null;
  }
}

export function clearPreviewDraft(scene: PreviewCenterScene) {
  if (!canUseStorage()) return;
  window.localStorage.removeItem(buildPreviewDraftKey(scene));
}

export function clearAllPreviewDrafts() {
  PREVIEW_DRAFT_SCENES.forEach(scene => clearPreviewDraft(scene));
}

export function getAllPreviewDrafts() {
  return PREVIEW_DRAFT_SCENES
    .map(scene => getPreviewDraft(scene))
    .filter(Boolean) as PreviewDraftEnvelope[];
}

export function notifyPreviewDataCommitted(scene?: PreviewCenterScene) {
  if (!canUseStorage()) return;
  window.localStorage.setItem(
    PREVIEW_COMMIT_KEY,
    JSON.stringify({
      scene: scene || "home",
      updatedAt: new Date().toISOString()
    })
  );
}

export function getPreviewCommitToken() {
  if (!canUseStorage()) return "";
  return window.localStorage.getItem(PREVIEW_COMMIT_KEY) || "";
}
