export const REFERENCE_HTML = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HAGAP References</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #f8f9fa;
    color: #1a1a2e;
    padding: 16px;
  }
  .ref-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e4e7ec;
  }
  .ref-header h2 {
    font-size: 16px;
    font-weight: 600;
    color: #16213e;
  }
  .ref-header .count {
    background: #4f46e5;
    color: white;
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 12px;
  }
  .ref-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .ref-card {
    background: white;
    border-radius: 8px;
    padding: 14px 16px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    border-left: 3px solid #4f46e5;
    transition: box-shadow 0.2s;
  }
  .ref-card:hover {
    box-shadow: 0 4px 12px rgba(0,0,0,0.12);
  }
  .ref-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }
  .ref-card-title {
    font-size: 14px;
    font-weight: 600;
    color: #16213e;
  }
  .ref-score {
    font-size: 12px;
    font-weight: 600;
    padding: 2px 8px;
    border-radius: 12px;
    white-space: nowrap;
  }
  .ref-score.high {
    background: #dcfce7;
    color: #166534;
  }
  .ref-score.medium {
    background: #fef3c7;
    color: #92400e;
  }
  .ref-score.low {
    background: #fee2e2;
    color: #991b1b;
  }
  .ref-meta {
    font-size: 12px;
    color: #6b7280;
    margin-bottom: 6px;
  }
  .ref-snippet {
    font-size: 13px;
    color: #374151;
    line-height: 1.5;
    background: #f9fafb;
    padding: 8px 10px;
    border-radius: 4px;
    border: 1px solid #f0f0f0;
    margin-bottom: 8px;
  }
  .ref-link {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #4f46e5;
    text-decoration: none;
  }
  .ref-link:hover {
    text-decoration: underline;
  }
  .empty-state {
    text-align: center;
    padding: 32px 16px;
    color: #9ca3af;
  }
  .empty-state p { font-size: 14px; }
</style>
</head>
<body>
<div id="ref-root">
  <div class="empty-state"><p>No references available</p></div>
</div>
<script type="module">
  function renderReferences(references) {
    const root = document.getElementById('ref-root');
    if (!references || references.length === 0) {
      root.innerHTML = '<div class="empty-state"><p>No references available</p></div>';
      return;
    }
    let html = '<div class="ref-header">';
    html += '<h2>Sources &amp; References</h2>';
    html += '<span class="count">' + references.length + '</span>';
    html += '</div>';
    html += '<div class="ref-list">';
    for (const ref of references) {
      const pct = Math.round((ref.score || 0) * 100);
      const scoreClass = pct >= 75 ? 'high' : pct >= 50 ? 'medium' : 'low';
      html += '<div class="ref-card">';
      html += '<div class="ref-card-header">';
      html += '<span class="ref-card-title">' + escapeHtml(ref.sourceFile) + '</span>';
      html += '<span class="ref-score ' + scoreClass + '">' + pct + '% match</span>';
      html += '</div>';
      html += '<div class="ref-meta">Chunk #' + ref.chunkIndex + '</div>';
      if (ref.snippet) {
        html += '<div class="ref-snippet">' + escapeHtml(ref.snippet) + '</div>';
      }
      if (ref.sourceUrl) {
        html += '<a class="ref-link" href="' + escapeHtml(ref.sourceUrl) + '" target="_blank" rel="noopener">View source &rarr;</a>';
      }
      html += '</div>';
    }
    html += '</div>';
    root.innerHTML = html;
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  if (window.App) {
    window.App.ontoolinput = (data) => {
      try {
        const parsed = typeof data === 'string' ? JSON.parse(data) : data;
        renderReferences(parsed.references || []);
      } catch (e) {
        console.error('Failed to parse reference data:', e);
      }
    };
  }

  window.addEventListener('message', (event) => {
    try {
      const data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
      if (data.references) {
        renderReferences(data.references);
      }
    } catch (e) {
      // ignore non-JSON messages
    }
  });
</script>
</body>
</html>`;
