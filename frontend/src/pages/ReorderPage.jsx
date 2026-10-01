import { useState, useEffect } from 'react';
import { getReorderRecommendations } from '../api/inventory';
import { Spinner } from '../components/ui/Spinner';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';

const fmt = (val) => {
  const n = typeof val === 'number' ? val : Number(val);
  return isNaN(n) ? '0.00' : n.toFixed(2);
};

export default function ReorderPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchRecommendations = async () => {
    try {
      setLoading(true);
      const res = await getReorderRecommendations();
      setData(res.data);
      setError(null);
    } catch (err) {
      setError('Failed to load recommendations');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchRecommendations(); }, []);

  if (loading) return <Spinner className="mt-12" />;
  if (error) return <ErrorState message={error} onRetry={fetchRecommendations} />;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Reorder Recommendations</h1>
        <span className="text-sm text-gray-500">{data?.length ?? 0} product(s) need reordering</span>
      </div>

      {!data?.length ? (
        <EmptyState message="No reorder recommendations — all stock levels are healthy! 🎉" />
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Product</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Current Stock</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Threshold</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Deficit</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Suggested Qty</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Unit Price</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Est. Cost</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Supplier</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {data.map((item) => (
                  <tr key={item.productId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{item.name}</div>
                      <div className="text-xs text-gray-500">{item.sku}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-red-600">{item.currentStock}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{item.minStockThreshold}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-orange-600 font-medium">{item.deficit}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-bold text-gray-900">{item.suggestedReorderQuantity}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${fmt(item.unitPrice)}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">${fmt(item.estimatedCost)}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{item.supplier?.name || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
