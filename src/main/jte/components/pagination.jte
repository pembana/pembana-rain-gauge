@param int page
@param int totalPages

@if (totalPages > 1)
  <nav aria-label="Pagination">
    <span class="text-secondary">Page ${page} of ${totalPages}</span>
  </nav>
@endif
