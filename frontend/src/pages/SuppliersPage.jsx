import { useState, useEffect, useCallback } from 'react';
import { getSuppliers, createSupplier, updateSupplier, deleteSupplier, toggleStatus } from '../api/suppliers';
import { useAuth } from '../hooks/useAuth';
import { Spinner } from '../components/ui/Spinner';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { Pagination } from '../components/ui/Pagination';
import toast from 'react-hot-toast';

const EMPTY_FORM = { name: '', email: '', phone: '', address: '', active: true };

export default function SuppliersPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const { isAdmin } = useAuth();

  const fetchSuppliers = useCallback(async () => {
    try {
      setLoading(true);
      const params = { page, size: 10, sortBy: 'name', sortDir: 'asc' };
      if (search) params.search = search;
      const res = await getSuppliers(params);
      setData(res.data);
      setError(null);
    } catch (err) {
      setError('Failed to load suppliers');
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => { fetchSuppliers(); }, [fetchSuppliers]);

  const openCreate = () => {
    setEditing(null);
    setFormData(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEdit = (sup) => {
    setEditing(sup);
    setFormData({ name: sup.name, email: sup.email || '', phone: sup.phone || '', address: sup.address || '', active: sup.active });
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) { toast.error('Name is required'); return; }
    setSaving(true);
    try {
      if (editing) {
        await updateSupplier(editing.id, formData);
        toast.success('Supplier updated');
      } else {
        await createSupplier(formData);
        toast.success('Supplier created');
      }
      setModalOpen(false);
      fetchSuppliers();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (id) => {
    try {
      await toggleStatus(id);
      toast.success('Supplier status toggled');
      fetchSuppliers();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to toggle status');
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this supplier?')) return;
    try {
      await deleteSupplier(id);
      toast.success('Supplier deleted');
      fetchSuppliers();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Delete failed');
    }
  };

  const f = (key) => (e) => setFormData({ ...formData, [key]: e.target.value });

  if (loading && !data) return <Spinner className="mt-12" />;
  if (error) return <ErrorState message={error} onRetry={fetchSuppliers} />;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Suppliers</h1>
        <div className="flex gap-3">
          <input
            type="text"
            placeholder="Search suppliers..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            className="h-10 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          {isAdmin() && <Button onClick={openCreate}>Create Supplier</Button>}
        </div>
      </div>

      {!data?.content?.length ? (
        <EmptyState message="No suppliers found" />
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Phone</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  {isAdmin() && <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {data.content.map((sup) => (
                  <tr key={sup.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{sup.name}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{sup.email || '—'}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{sup.phone || '—'}</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Badge variant={sup.active ? 'green' : 'gray'}>{sup.active ? 'Active' : 'Inactive'}</Badge>
                    </td>
                    {isAdmin() && (
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-3">
                        <button onClick={() => openEdit(sup)} className="text-primary-600 hover:text-primary-900">Edit</button>
                        <button onClick={() => handleToggle(sup.id)} className="text-yellow-600 hover:text-yellow-900">
                          {sup.active ? 'Deactivate' : 'Activate'}
                        </button>
                        <button onClick={() => handleDelete(sup.id)} className="text-red-600 hover:text-red-900">Delete</button>
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

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Supplier' : 'Create Supplier'}>
        <form onSubmit={handleSave} className="space-y-4">
          <Input label="Name *" value={formData.name} onChange={f('name')} required />
          <Input label="Email" type="email" value={formData.email} onChange={f('email')} />
          <Input label="Phone" value={formData.phone} onChange={f('phone')} />
          <Input label="Address" value={formData.address} onChange={f('address')} />
          <div className="flex items-center gap-2">
            <input type="checkbox" id="sup-active" checked={formData.active}
              onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
              className="h-4 w-4 rounded border-gray-300 text-primary-600" />
            <label htmlFor="sup-active" className="text-sm font-medium text-gray-700">Active</label>
          </div>
          <div className="flex justify-end space-x-3 pt-2">
            <Button variant="outline" type="button" onClick={() => setModalOpen(false)}>Cancel</Button>
            <Button type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
