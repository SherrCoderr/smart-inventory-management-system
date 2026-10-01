import { useState, useEffect } from 'react';
import { getCategories, createCategory, updateCategory, deleteCategory } from '../api/categories';
import { useAuth } from '../hooks/useAuth';
import { Spinner } from '../components/ui/Spinner';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';
import { Button } from '../components/ui/Button';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import toast from 'react-hot-toast';

export default function CategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null); // null = create, object = edit
  const [formData, setFormData] = useState({ name: '', description: '' });
  const [saving, setSaving] = useState(false);
  const { isAdmin } = useAuth();

  const fetchCategories = async () => {
    try {
      setLoading(true);
      const res = await getCategories(search ? { search } : {});
      // Backend returns ApiResponse<List<CategoryResponse>>, axios interceptor strips outer wrapper
      // so res is { success, message, data: [...] }
      setCategories(res.data || []);
      setError(null);
    } catch (err) {
      setError('Failed to load categories');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCategories(); }, [search]);

  const openCreate = () => {
    setEditing(null);
    setFormData({ name: '', description: '' });
    setModalOpen(true);
  };

  const openEdit = (cat) => {
    setEditing(cat);
    setFormData({ name: cat.name, description: cat.description || '' });
    setModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) { toast.error('Name is required'); return; }
    setSaving(true);
    try {
      if (editing) {
        await updateCategory(editing.id, formData);
        toast.success('Category updated');
      } else {
        await createCategory(formData);
        toast.success('Category created');
      }
      setModalOpen(false);
      fetchCategories();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this category?')) return;
    try {
      await deleteCategory(id);
      toast.success('Category deleted');
      fetchCategories();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Delete failed');
    }
  };

  if (loading) return <Spinner className="mt-12" />;
  if (error) return <ErrorState message={error} onRetry={fetchCategories} />;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
        <h1 className="text-2xl font-bold text-gray-900">Categories</h1>
        <div className="flex gap-3">
          <input
            type="text"
            placeholder="Search categories..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-10 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          {isAdmin() && <Button onClick={openCreate}>Create Category</Button>}
        </div>
      </div>

      {!categories.length ? (
        <EmptyState message="No categories found" />
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
                  {isAdmin() && <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>}
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {categories.map((cat) => (
                  <tr key={cat.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{cat.name}</td>
                    <td className="px-6 py-4 text-sm text-gray-500">{cat.description || '—'}</td>
                    {isAdmin() && (
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-3">
                        <button onClick={() => openEdit(cat)} className="text-primary-600 hover:text-primary-900">Edit</button>
                        <button onClick={() => handleDelete(cat.id)} className="text-red-600 hover:text-red-900">Delete</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Category' : 'Create Category'}>
        <form onSubmit={handleSave} className="space-y-4">
          <Input
            label="Name *"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            required
          />
          <Input
            label="Description"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          />
          <div className="flex justify-end space-x-3 pt-2">
            <Button variant="outline" type="button" onClick={() => setModalOpen(false)}>Cancel</Button>
            <Button type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}


