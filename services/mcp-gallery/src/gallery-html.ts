export const GALLERY_HTML = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>HAGAP Gallery</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #f8f9fa;
    color: #1a1a2e;
    padding: 16px;
  }
  .gallery-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e4e7ec;
  }
  .gallery-header h2 {
    font-size: 16px;
    font-weight: 600;
    color: #16213e;
  }
  .gallery-header .count {
    background: #4f46e5;
    color: white;
    font-size: 12px;
    padding: 2px 8px;
    border-radius: 12px;
  }
  .gallery-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 12px;
  }
  .gallery-item {
    background: white;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    transition: box-shadow 0.2s, transform 0.2s;
  }
  .gallery-item:hover {
    box-shadow: 0 4px 12px rgba(0,0,0,0.12);
    transform: translateY(-2px);
  }
  .gallery-item img {
    width: 100%;
    height: 160px;
    object-fit: cover;
    display: block;
    background: #eef0f2;
  }
  .gallery-item .caption {
    padding: 8px 12px;
    font-size: 13px;
    color: #374151;
    line-height: 1.4;
  }
  .gallery-item .source {
    padding: 0 12px 8px;
    font-size: 11px;
    color: #9ca3af;
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
<div id="gallery-root">
  <div class="empty-state"><p>No images available</p></div>
</div>
<script type="module">
  function renderGallery(images) {
    const root = document.getElementById('gallery-root');
    if (!images || images.length === 0) {
      root.innerHTML = '<div class="empty-state"><p>No images available</p></div>';
      return;
    }
    let html = '<div class="gallery-header">';
    html += '<h2>Related Images</h2>';
    html += '<span class="count">' + images.length + '</span>';
    html += '</div>';
    html += '<div class="gallery-grid">';
    for (const img of images) {
      html += '<div class="gallery-item">';
      html += '<img src="' + escapeHtml(img.url) + '" alt="' + escapeHtml(img.alt || '') + '" loading="lazy" onerror="this.src=\\'data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 width=%22200%22 height=%22160%22><rect fill=%22%23eef0f2%22 width=%22200%22 height=%22160%22/><text x=%2250%25%22 y=%2250%25%22 text-anchor=%22middle%22 dy=%22.3em%22 fill=%22%239ca3af%22 font-size=%2214%22>Image unavailable</text></svg>\\'" />';
      if (img.caption) {
        html += '<div class="caption">' + escapeHtml(img.caption) + '</div>';
      }
      if (img.source) {
        html += '<div class="source">Source: ' + escapeHtml(img.source) + '</div>';
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

  // Listen for tool input from MCP App SDK
  if (window.App) {
    window.App.ontoolinput = (data) => {
      try {
        const parsed = typeof data === 'string' ? JSON.parse(data) : data;
        renderGallery(parsed.images || []);
      } catch (e) {
        console.error('Failed to parse gallery data:', e);
      }
    };
  }

  // Also support direct message events
  window.addEventListener('message', (event) => {
    try {
      const data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
      if (data.images) {
        renderGallery(data.images);
      }
    } catch (e) {
      // ignore non-JSON messages
    }
  });
</script>
</body>
</html>`;
