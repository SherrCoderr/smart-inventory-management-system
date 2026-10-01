import { useState, useEffect, useCallback } from 'react';
import { getProducts, createProduct, updateProduct, deleteProduct, toggleStatus } from '../api/products';
import { getCategories } from '../api/categories';
import { getActiveSuppliers } from '../api/suppliers';
import { useAuth } from '../hooks/useAuth';
import { Spinner } from '../components/ui/Spinner';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { Pagination } from '../components/ui/Pagination';
import { useDebounce } from '../hooks/useDebounce';
import toast from 'react-hot-toast';

const EMPTY_FORM = {
  sku: '', name: '', description: '', price: '', currentStock: '',
  minStockThreshold: '', reorderQuantity: '', categoryId: '', supplierId: '', active: true,
};

export default function ProductsPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [supplierFilter, setSupplierFilter] = useState('');
  const [activeFilter, setActiveFilter] = useState('');
  const [categories, setCategories] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const { isAdmin } = useAuth();
  const debouncedQuery = useDebounce(query, 400);

  // Load filter options
  useEffect(() => {
    getCategories().then((r) => setCategories(r.data || [])).catch(() => {});
    getActiveSuppliers().then((r) => setSuppliers(r.data || [])).catch(() => {});
  }, []);

  const fetchProducts = useCallback(async () => {
    try {
      setLoading(true);
      const params = { page, size: 10, sortBy: 'name', sortDir: 'asc' };
      if (debouncedQuery) params.query = debouncedQuery;
      if (categoryFilter) params.categoryId = categoryFilter;
      if (supplierFilter) params.supplierId = supplierFilter;
      if (activeFilter !== '') params.active = activeFilter;
      const res = await getProducts(params);
      setData(res.data);
      setError(null);
    } catch (err) {
      setError('Failed to load products');
    } finally {
      setLoading(false);
    }
  }, [page, debouncedQuery, categoryFilter, supplierFilter, activeFilter]);

  useEffect(() => { fetchProducts(); }, [fetchProducts]);

  // Reset to page 0 when filters change
  useEffect(() => { setPage(0); }, [debouncedQuery, categoryFilter, supplierFilter, activeFilter]);

  const openCreate = () => {
    setEditing(null);
    setFormData(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEdit = (product) => {
    setEditing(product);
    setFormData({
      sku: product.sku,
      name: product.name,
      description: product.description || '',
      price: product.price,
      currentStock: product.currentStock,
      minStockThreshold: product.minStockThreshold,
      reorderQuantity: product.reorderQuantity || '',
      categoryId: product.category?.id || '',
      supplierId: product.supplier?.id || '',
      active: product.active,
    });
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formData.sku || !formData.name || !formData.price) {
      toast.error('SKU, Name and Price are required');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        ...formData,
        price: parseFloat(formData.price),
        currentStock: parseInt(formData.currentStock, 10) || 0,
        minStockThreshold: parseInt(formData.minStockThreshold, 10) || 0,
        reorderQuantity: formData.reorderQuantity ? parseInt(formData.reorderQuantity, 10) : null,
        categoryId: formData.categoryId ? parseInt(formData.categoryId, 10) : null,
        supplierId: formData.supplierId ? parseInt(formData.supplierId, 10) : null,
      };
      if (editing) {
        await updateProduct(editing.id, payload);
        toast.success('Product updated');
      } else {
        await createProduct(payload);
        toast.success('Product created');
      }
      setModalOpen(false);
      fetchProducts();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this product? This cannot be undone.')) return;
    try {
      await deleteProduct(id);
      toast.success('Product deleted');
      fetchProducts();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Delete failed');
    }
  };

  const handleToggle = async (id) => {
    try {
      await toggleStatus(id);
      toast.success('Status updated');
      fetchProducts();
    } catch (err) {
      toast.error('Failed to update status');
    }
  };

  const f = (key) => (e) => setFormData({ ...formData, [key]: e.target.value });

  return (
    <div className="space-y-6">
      {/* Header + filters */}
      <div className="flex flex-col gap-4">
        <div className="flex justify-between items-center">
          <h1 className="text-2xl font-bold text-gray-900">Products</h1>
          {isAdmin() && <Button onClick={openCreate}>Create Product</Button>}
        </div>
        <div className="flex flex-wrap gap-2">
          <input
            type="text"
            placeholder="Search by name, SKU..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            className="h-10 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 flex-1 min-w-[180px]"
          />
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="h-10 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="">All Categories</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select
            value={supplierFilter}
            onChange={(e) => setSupplierFilter(e.target.value)}
            className="h-10 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="">All Suppliers</option>
            {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
          <select
            value={activeFilter}
            onChange={(e) => setActiveFilter(e.target.value)}
            className="h-10 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="">All Status</option>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </div>
      </div>

      {loading && !data ? <Spinner className="mt-12" /> : error ? <ErrorState message={error} onRetry={fetchProducts} /> : !data?.content?.length ? (
        <EmptyState message="No products found" />
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Category</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Supplier</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Price</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stock</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  {isAdmin() && <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {data.content.map((product) => (
                  <tr key={product.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-600">{product.sku}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{product.name}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{product.category?.name || '—'}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{product.supplier?.name || '—'}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      ${Number(product.price).toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={product.lowStock ? 'text-red-600 font-bold' : 'text-gray-900'}>
                        {product.currentStock}
                      </span>
                      {product.lowStock && <span className="ml-1 text-red-400" title="Low Stock">⚠</span>}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Badge variant={product.active ? 'green' : 'gray'}>
                        {product.active ? 'Active' : 'Inactive'}
                      </Badge>
                    </td>
                    {isAdmin() && (
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-3">
                        <button onClick={() => openEdit(product)} className="text-primary-600 hover:text-primary-900">Edit</button>
                        <button onClick={() => handleToggle(product.id)} className="text-yellow-600 hover:text-yellow-900">
                          {product.active ? 'Deactivate' : 'Activate'}
                        </button>
                        <button onClick={() => handleDelete(product.id)} className="text-red-600 hover:text-red-900">Delete</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onPageChange={setPage} />
        </div>
      )}

      {/* Product Form Modal */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Product' : 'Create Product'}>
        <form onSubmit={handleSave} className="space-y-3 max-h-[70vh] overflow-y-auto pr-1">
          <div className="grid grid-cols-2 gap-3">
            <Input label="SKU *" value={formData.sku} onChange={f('sku')} disabled={!!editing} required />
            <Input label="Name *" value={formData.name} onChange={f('name')} required />
          </div>
          <Input label="Description" value={formData.description} onChange={f('description')} />
          <div className="grid grid-cols-2 gap-3">
            <Input label="Price *" type="number" step="0.01" min="0" value={formData.price} onChange={f('price')} required />
            <Input label="Current Stock" type="number" min="0" value={formData.currentStock} onChange={f('currentStock')} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Min Stock Threshold" type="number" min="0" value={formData.minStockThreshold} onChange={f('minStockThreshold')} />
            <Input label="Reorder Quantity" type="number" min="1" value={formData.reorderQuantity} onChange={f('reorderQuantity')} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
            <select className="h-10 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={formData.categoryId} onChange={f('categoryId')}>
              <option value="">— None —</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Supplier</label>
            <select className="h-10 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={formData.supplierId} onChange={f('supplierId')}>
              <option value="">— None —</option>
              {suppliers.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div className="flex items-center gap-2 pt-1">
            <input type="checkbox" id="active" checked={formData.active}
              onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
              className="h-4 w-4 rounded border-gray-300 text-primary-600" />
            <label htmlFor="active" className="text-sm font-medium text-gray-700">Active</label>
          </div>
          <div className="flex justify-end space-x-3 pt-2 border-t">
            <Button variant="outline" type="button" onClick={() => setModalOpen(false)}>Cancel</Button>
            <Button type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
