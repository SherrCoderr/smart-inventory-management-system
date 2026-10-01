import { useState, useEffect, useCallback } from 'react';
import { getTransactions } from '../api/inventory';
import { Spinner } from '../components/ui/Spinner';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';
import { Badge } from '../components/ui/Badge';
import { Pagination } from '../components/ui/Pagination';

export default function TransactionsPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [typeFilter, setTypeFilter] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const fetchTx = useCallback(async () => {
    try {
      setLoading(true);
      const params = { page, size: 15, sortBy: 'createdAt', sortDir: 'desc' };
      if (typeFilter) params.transactionType = typeFilter;
      // Backend expects ISO DateTimeFormat — append time component
      if (startDate) params.startDate = `${startDate}T00:00:00`;
      if (endDate) params.endDate = `${endDate}T23:59:59`;
      const res = await getTransactions(params);
      setData(res.data);
      setError(null);
    } catch (err) {
      setError('Failed to load transactions');
    } finally {
      setLoading(false);
    }
  }, [page, typeFilter, startDate, endDate]);

  useEffect(() => { fetchTx(); }, [fetchTx]);
  useEffect(() => { setPage(0); }, [typeFilter, startDate, endDate]);

  if (loading && !data) return <Spinner className="mt-12" />;
  if (error) return <ErrorState message={error} onRetry={fetchTx} />;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Transaction History</h1>

      {/* Filters */}
      <div className="flex flex-wrap gap-3 bg-white p-4 rounded-lg shadow">
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          className="h-10 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        >
          <option value="">All Types</option>
          <option value="STOCK_IN">Stock In</option>
          <option value="STOCK_OUT">Stock Out</option>
        </select>
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-600 whitespace-nowrap">From:</label>
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="h-10 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        <div className="flex items-center gap-2">
          <label className="text-sm text-gray-600 whitespace-nowrap">To:</label>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="h-10 rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        {(typeFilter || startDate || endDate) && (
          <button
            onClick={() => { setTypeFilter(''); setStartDate(''); setEndDate(''); }}
            className="h-10 px-4 text-sm text-gray-600 border border-gray-300 rounded-md hover:bg-gray-50"
          >
            Clear Filters
          </button>
        )}
      </div>

      {!data?.content?.length ? (
        <EmptyState message="No transactions found" />
      ) : (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Product</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Qty</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stock Change</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Reason</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {data.content.map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-400">#{tx.id}</td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{tx.productName}</div>
                      <div className="text-xs text-gray-500">{tx.productSku}</div>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap">
                      <Badge variant={tx.transactionType === 'STOCK_IN' ? 'green' : 'red'}>
                        {tx.transactionType === 'STOCK_IN' ? 'Stock In' : 'Stock Out'}
                      </Badge>
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-sm font-bold text-gray-900">
                      {tx.transactionType === 'STOCK_IN' ? '+' : '-'}{tx.quantity}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                      {tx.previousStock} → <span className="font-medium text-gray-900">{tx.newStock}</span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500 max-w-[160px] truncate" title={tx.referenceReason}>
                      {tx.referenceReason || '—'}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-500">
                      {tx.userFullName || tx.username}
                    </td>
                    <td className="px-4 py-3 whitespace-nowrap text-xs text-gray-500">
                      {new Date(tx.createdAt).toLocaleString()}
                    </td>
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
