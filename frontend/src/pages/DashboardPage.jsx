import { useState, useEffect } from 'react';
import { getDashboardStats } from '../api/dashboard';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { Spinner } from '../components/ui/Spinner';
import { ErrorState } from '../components/ui/ErrorState';
import { Badge } from '../components/ui/Badge';

const formatCurrency = (val) => {
  const num = typeof val === 'number' ? val : Number(val);
  return isNaN(num) ? '$0.00' : `$${num.toFixed(2)}`;
};

export default function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchStats = async () => {
    try {
      setLoading(true);
      const res = await getDashboardStats();
      setStats(res.data);
      setError(null);
    } catch (err) {
      setError('Failed to load dashboard stats');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchStats(); }, []);

  if (loading) return <Spinner className="mt-12" />;
  if (error) return <ErrorState message={error} onRetry={fetchStats} />;
  if (!stats) return null;

  // Format chart dates to be readable (e.g. "Oct 01")
  const chartData = (stats.dailyMovements || []).map((d) => ({
    ...d,
    date: new Date(d.date).toLocaleDateString('en-US', { month: 'short', day: '2-digit' }),
  }));

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
        <KpiCard title="Total Products" value={stats.totalProducts} color="border-blue-500" />
        <KpiCard title="Active Products" value={stats.activeProducts} color="border-green-500" />
        <KpiCard title="Total Suppliers" value={stats.totalSuppliers} color="border-purple-500" />
        <KpiCard title="Total Categories" value={stats.totalCategories} color="border-yellow-500" />
        <KpiCard title="Total Inventory Value" value={formatCurrency(stats.totalInventoryValue)} color="border-indigo-500" />
        <div className={`bg-white overflow-hidden shadow rounded-lg border-l-4 ${stats.lowStockProductCount > 0 ? 'border-red-500' : 'border-green-500'}`}>
          <div className="p-5">
            <dt className="text-sm font-medium text-gray-500 truncate">Low Stock Count</dt>
            <dd className={`mt-1 text-3xl font-semibold ${stats.lowStockProductCount > 0 ? 'text-red-600' : 'text-gray-900'}`}>
              {stats.lowStockProductCount}
            </dd>
          </div>
        </div>
      </div>

      {/* 7-Day Stock Movement Chart */}
      <div className="bg-white p-6 rounded-lg shadow">
        <h2 className="text-lg font-medium text-gray-900 mb-4">7-Day Stock Movement</h2>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="stockIn" stroke="#3b82f6" name="Stock In" strokeWidth={2} dot={false} />
              <Line type="monotone" dataKey="stockOut" stroke="#ef4444" name="Stock Out" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Recent Transactions */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="px-4 py-5 sm:px-6">
          <h3 className="text-lg leading-6 font-medium text-gray-900">Recent Transactions</h3>
        </div>
        {(!stats.recentTransactions || stats.recentTransactions.length === 0) ? (
          <p className="text-sm text-gray-500 px-6 py-4">No recent transactions.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Product</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Qty</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Date</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {stats.recentTransactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{tx.productName}</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Badge variant={tx.transactionType === 'STOCK_IN' ? 'green' : 'red'}>
                        {tx.transactionType === 'STOCK_IN' ? 'Stock In' : 'Stock Out'}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{tx.quantity}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{tx.userFullName || tx.username}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {new Date(tx.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function KpiCard({ title, value, color = 'border-gray-200' }) {
  return (
    <div className={`bg-white overflow-hidden shadow rounded-lg border-l-4 ${color}`}>
      <div className="p-5">
        <dt className="text-sm font-medium text-gray-500 truncate">{title}</dt>
        <dd className="mt-1 text-3xl font-semibold text-gray-900">{value}</dd>
      </div>
    </div>
  );
}
