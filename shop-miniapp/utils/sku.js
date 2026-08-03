function normalizeProduct(product, baseGoods) {
	const valueIds = Array.isArray(product.specificationValueIds)
		? product.specificationValueIds.map(Number).filter(Number.isFinite)
		: String(product.goodsSpecificationIds || '').split('_').filter(Boolean).map(Number).filter(Number.isFinite);
	const specificationPairs = Array.isArray(product.properties)
		? product.properties.map(property => ({
			specificationId: Number(property.specificationId),
			valueId: Number(property.valueId)
		})).filter(property => Number.isFinite(property.specificationId) && Number.isFinite(property.valueId))
		: [];
	const rawStock = product.stock !== undefined && product.stock !== null ? product.stock : product.goodsNumber;
	const parsedStock = Number(rawStock);
	const stock = Number.isFinite(parsedStock) ? Math.max(0, Math.floor(parsedStock)) : 0;
	return Object.assign({}, product, {
		specificationValueIds: valueIds,
		specificationPairs: specificationPairs,
		stock: stock,
		available: typeof product.available === 'boolean' ? product.available : stock > 0,
		retailPrice: product.retailPrice === undefined || product.retailPrice === null || product.retailPrice === '' ? baseGoods.retailPrice : product.retailPrice,
		counterPrice: product.counterPrice === undefined || product.counterPrice === null || product.counterPrice === '' ? baseGoods.counterPrice : product.counterPrice,
		hasSkuPic: Boolean(product.picUrl),
		picUrl: product.picUrl || baseGoods.listPicUrl || baseGoods.picUrl || ''
	});
}

function productMatchesSelection(product, selected, requireComplete) {
	if (Array.isArray(product.specificationPairs) && product.specificationPairs.length > 0) {
		if (requireComplete && product.specificationPairs.length !== selected.length) return false;
		return selected.every(value => product.specificationPairs.some(property =>
			property.specificationId === Number(value.nameId) && property.valueId === Number(value.valueId)));
	}
	const selectedIds = selected.map(value => Number(value.valueId));
	const skuIds = Array.isArray(product.specificationValueIds) ? product.specificationValueIds.map(Number) : [];
	return (!requireComplete || skuIds.length === selectedIds.length) && selectedIds.every(id => skuIds.indexOf(id) !== -1);
}

module.exports = {
	normalizeProduct,
	productMatchesSelection
};
