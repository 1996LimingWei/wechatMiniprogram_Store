const assert = require('assert');
const sku = require('../shop-miniapp/utils/sku.js');

const baseGoods = { retailPrice: '99.00', counterPrice: '109.00', listPicUrl: 'spu.png' };
const first = sku.normalizeProduct({
  id: 1,
  properties: [
    { specificationId: 10, valueId: 1 },
    { specificationId: 20, valueId: 1 }
  ],
  stock: 3,
  retailPrice: '12.00',
  picUrl: 'first.png'
}, baseGoods);
const second = sku.normalizeProduct({
  id: 2,
  properties: [
    { specificationId: 10, valueId: 1 },
    { specificationId: 20, valueId: 2 }
  ],
  stock: 5,
  retailPrice: '15.00',
  picUrl: ''
}, baseGoods);

const selection = [
  { nameId: 10, valueId: 1 },
  { nameId: 20, valueId: 2 }
];
assert.strictEqual(sku.productMatchesSelection(first, selection, true), false);
assert.strictEqual(sku.productMatchesSelection(second, selection, true), true);
assert.strictEqual(sku.productMatchesSelection(second, [{ nameId: 20, valueId: 2 }], false), true);
assert.strictEqual(second.picUrl, 'spu.png');
assert.strictEqual(second.hasSkuPic, false);

const legacy = sku.normalizeProduct({ id: 3, goodsSpecificationIds: '1_2', goodsNumber: '2' }, baseGoods);
assert.strictEqual(sku.productMatchesSelection(legacy, selection, true), true);
assert.strictEqual(legacy.stock, 2);
assert.strictEqual(legacy.available, true);

const soldOut = sku.normalizeProduct({ id: 4, goodsSpecificationIds: '1_2', goodsNumber: 0 }, baseGoods);
assert.strictEqual(soldOut.available, false);

const emptyLegacy = sku.normalizeProduct({ id: 5, goodsSpecificationIds: '', goodsNumber: 1 }, baseGoods);
assert.deepStrictEqual(emptyLegacy.specificationValueIds, []);

console.log('MINIAPP_SKU_ACCEPTANCE_OK');
