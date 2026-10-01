import { Button } from './Button';

export const Pagination = ({ page, totalPages, totalElements, onPageChange }) => {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between px-4 py-3 border-t sm:px-6">
      <div className="flex flex-1 justify-between sm:hidden">
        <Button
          variant="outline"
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
        >
          Previous
        </Button>
        <Button
          variant="outline"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
        >
          Next
        </Button>
      </div>
      <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
        <div>
          <p className="text-sm text-gray-700">
            Total <span className="font-medium">{totalElements}</span> results
          </p>
        </div>
        <div>
          <nav className="isolate inline-flex -space-x-px rounded-md shadow-sm" aria-label="Pagination">
            <button
              onClick={() => onPageChange(page - 1)}
              disabled={page === 0}
              className="relative inline-flex items-center rounded-l-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0 disabled:opacity-50"
            >
              Previous
            </button>
            <span className="relative inline-flex items-center px-4 py-2 text-sm font-semibold text-gray-900 ring-1 ring-inset ring-gray-300">
              Page {page + 1} of {totalPages}
            </span>
            <button
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages - 1}
              className="relative inline-flex items-center rounded-r-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0 disabled:opacity-50"
            >
              Next
            </button>
          </nav>
        </div>
      </div>
    </div>
  );
};
