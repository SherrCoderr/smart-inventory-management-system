import { useState, useEffect } from 'react';
import { getLowStock } from '../api/inventory';
import { Spinner } from '../components/ui/Spinner';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';
import { Pagination } from '../components/ui/Pagination';

export default function LowStockPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);

  const fetchLowStock = async () => {
    try {
      setLoading(true);
      const res = await getLowStock({ page, size: 10 });
      setData(res.data);
      setError(null);
    } catch (err) {
      setError('Failed to load low stock products');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLowStock();
  }, [page]);

  if (loading && !data) return <Spinner className="mt-12" />;
  if (error) return <ErrorState message={error} onRetry={fetchLowStock} />;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Low Stock Products</h1>

      {!data?.content?.length ? (
        <EmptyState message="No low stock products" />
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Product</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">SKU</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Current Stock</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Threshold</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Supplier</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {data.content.map((item) => (
                  <tr key={item.id} className={item.currentStock === 0 ? 'bg-red-50' : 'bg-yellow-50'}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{item.name}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{item.sku}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-red-600">{item.currentStock}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{item.minStockThreshold}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{item.supplier?.name}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onPageChange={setPage} />
        </div>
      )}
    </div>
  );
}
