import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { register as registerApi } from '../api/auth';
import { Input } from '../components/ui/Input';
import { Button } from '../components/ui/Button';
import toast from 'react-hot-toast';

export default function RegisterPage() {
  const [formData, setFormData] = useState({ username: '', email: '', password: '', firstName: '', lastName: '', role: 'EMPLOYEE' });
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await registerApi(formData);
      if (res.success) {
        login(res.data.token, res.data.user);
        toast.success('Registered successfully');
        navigate('/dashboard');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white p-8 rounded-xl shadow-md border border-gray-100">
        <div>
          <h2 className="text-center text-3xl font-extrabold text-gray-900">Create an account</h2>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          <div className="space-y-4">
            <Input label="Username" required value={formData.username} onChange={(e) => setFormData({ ...formData, username: e.target.value })} />
            <Input label="Email" type="email" required value={formData.email} onChange={(e) => setFormData({ ...formData, email: e.target.value })} />
            <Input label="Password" type="password" required value={formData.password} onChange={(e) => setFormData({ ...formData, password: e.target.value })} />
            <div className="grid grid-cols-2 gap-4">
              <Input label="First Name" required value={formData.firstName} onChange={(e) => setFormData({ ...formData, firstName: e.target.value })} />
              <Input label="Last Name" required value={formData.lastName} onChange={(e) => setFormData({ ...formData, lastName: e.target.value })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
              <select
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                value={formData.role}
                onChange={(e) => setFormData({ ...formData, role: e.target.value })}
              >
                <option value="EMPLOYEE">Employee</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
          </div>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Registering...' : 'Register'}
          </Button>
        </form>
        <div className="text-center text-sm">
          Already have an account? <Link to="/login" className="text-primary-600 hover:text-primary-500 font-medium">Sign in</Link>
        </div>
      </div>
    </div>
  );
}
