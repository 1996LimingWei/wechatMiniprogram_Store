const runtimeEnv = typeof process !== 'undefined' && process.env ? process.env : {};
const isProduction = runtimeEnv.NODE_ENV === 'production';
const injectedApiBaseUrl = runtimeEnv.VUE_APP_API_BASE_URL || '';

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
	isProduction,
	apiBaseUrl,
	validate() {
		if (!apiBaseUrl) {
			throw new Error('正式环境未配置 VUE_APP_API_BASE_URL');
		}
		if (isProduction && !apiBaseUrl.startsWith('https://')) {
			throw new Error('正式环境 API 地址必须使用 HTTPS');
		}
	}
};
