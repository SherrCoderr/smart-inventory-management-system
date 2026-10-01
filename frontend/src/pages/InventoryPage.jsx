import { useState, useEffect } from 'react';
import { stockIn, stockOut } from '../api/inventory';
import { getProducts } from '../api/products';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Spinner } from '../components/ui/Spinner';
import toast from 'react-hot-toast';

export default function InventoryPage() {
  const [formData, setFormData] = useState({ productId: '', quantity: '', referenceReason: '' });
  const [loading, setLoading] = useState(false);
  const [products, setProducts] = useState([]);
  const [productsLoading, setProductsLoading] = useState(true);
  const [selectedProduct, setSelectedProduct] = useState(null);

  // Load active products for dropdown selection
  useEffect(() => {
    getProducts({ active: true, size: 200, sortBy: 'name', sortDir: 'asc' })
      .then((res) => setProducts(res.data?.content || []))
      .catch(() => toast.error('Failed to load product list'))
      .finally(() => setProductsLoading(false));
  }, []);

  const handleProductChange = (e) => {
    const id = e.target.value;
    setFormData({ ...formData, productId: id });
    const found = products.find((p) => String(p.id) === id);
    setSelectedProduct(found || null);
  };

  const handleSubmit = async (type) => {
    if (!formData.productId || !formData.quantity) {
      toast.error('Product and quantity are required');
      return;
    }
    const qty = parseInt(formData.quantity, 10);
    if (isNaN(qty) || qty <= 0) {
      toast.error('Quantity must be a positive number');
      return;
    }
    try {
      setLoading(true);
      const data = {
        productId: parseInt(formData.productId, 10),
        quantity: qty,
        referenceReason: formData.referenceReason || (type === 'IN' ? 'Stock replenishment' : 'Stock dispatch'),
      };
      if (type === 'IN') {
        await stockIn(data);
        toast.success(`Stock added: +${qty} units`);
      } else {
        await stockOut(data);
        toast.success(`Stock removed: -${qty} units`);
      }
      setFormData({ productId: '', quantity: '', referenceReason: '' });
      setSelectedProduct(null);
      // Re-fetch products to get updated stock
      const updated = await getProducts({ active: true, size: 200, sortBy: 'name', sortDir: 'asc' });
      setProducts(updated.data?.content || []);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Operation failed';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-900">Stock Operations</h1>

      <div className="bg-white p-6 rounded-lg shadow space-y-4">
        {/* Product Selector */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Product *</label>
          {productsLoading ? (
            <Spinner />
          ) : (
            <select
              className="h-10 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={formData.productId}
              onChange={handleProductChange}
            >
              <option value="">— Select a product —</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  [{p.sku}] {p.name}
                </option>
              ))}
            </select>
          )}
        </div>

        {/* Current Stock Preview */}
        {selectedProduct && (
          <div className="rounded-md bg-blue-50 border border-blue-200 p-3 text-sm text-blue-800">
            <span className="font-semibold">{selectedProduct.name}</span> — Current stock:{' '}
            <span className={`font-bold ${selectedProduct.lowStock ? 'text-red-600' : 'text-green-700'}`}>
              {selectedProduct.currentStock}
            </span>
            {selectedProduct.lowStock && (
              <span className="ml-2 text-red-500">⚠️ Low stock (threshold: {selectedProduct.minStockThreshold})</span>
            )}
          </div>
        )}

        <Input
          label="Quantity *"
          type="number"
          min="1"
          value={formData.quantity}
          onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
        />

        <Input
          label="Reference / Reason"
          placeholder="e.g. Purchase order #1234, Customer sale..."
          value={formData.referenceReason}
          onChange={(e) => setFormData({ ...formData, referenceReason: e.target.value })}
        />

        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          <Button
            onClick={() => handleSubmit('IN')}
            disabled={loading || productsLoading}
            className="w-full sm:w-auto"
          >
            {loading ? 'Processing...' : '↑ Stock In'}
          </Button>
          <Button
            variant="danger"
            onClick={() => handleSubmit('OUT')}
            disabled={loading || productsLoading}
            className="w-full sm:w-auto"
          >
            {loading ? 'Processing...' : '↓ Stock Out'}
          </Button>
        </div>

        <p className="text-xs text-gray-400">
          Stock Out will fail if requested quantity exceeds available stock (insufficient stock protection).
        </p>
      </div>
    </div>
  );
}
