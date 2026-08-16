const runtimeEnv = typeof process !== 'undefined' && process.env ? process.env : {};
const appEnv = runtimeEnv.VUE_APP_ENV || runtimeEnv.UNI_APP_ENV || runtimeEnv.NODE_ENV || 'development';
const isProduction = appEnv === 'production';
const isStaging = appEnv === 'staging';
const injectedApiBaseUrl = isProduction
	? (runtimeEnv.VUE_APP_PROD_API_BASE_URL || runtimeEnv.VUE_APP_API_BASE_URL || '')
	: isStaging
		? (runtimeEnv.VUE_APP_STAGING_API_BASE_URL || runtimeEnv.VUE_APP_API_BASE_URL || '')
		: (runtimeEnv.VUE_APP_DEV_API_BASE_URL || runtimeEnv.VUE_APP_API_BASE_URL || '');

function normalizeBaseUrl(value) {
	const text = String(value || '').trim();
	if (!text) {
		return '';
	}
	return text.endsWith('/') ? text : text + '/';
}

const apiBaseUrl = normalizeBaseUrl(
	injectedApiBaseUrl || (isProduction ? '' : 'http://127.0.0.1:8085/')
);

module.exports = {
	appEnv,
	isProduction,
	isStaging,
	apiBaseUrl,
	validate() {
		if (!apiBaseUrl) {
			throw new Error('正式环境未配置 VUE_APP_API_BASE_URL');
		}
		if ((isProduction || isStaging) && !apiBaseUrl.startsWith('https://')) {
			throw new Error('体验版和正式环境 API 地址必须使用 HTTPS');
		}
	}
};
